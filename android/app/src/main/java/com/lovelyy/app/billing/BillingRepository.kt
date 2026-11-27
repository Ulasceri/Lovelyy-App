package com.lovelyy.app.billing

import android.app.Activity
import android.content.Context
import com.android.billingclient.api.AcknowledgePurchaseParams
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClient.BillingResponseCode
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.ConsumeParams
import com.android.billingclient.api.PendingPurchasesParams
import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.PurchasesUpdatedListener
import com.android.billingclient.api.QueryProductDetailsParams
import com.android.billingclient.api.QueryPurchasesParams
import com.android.billingclient.api.acknowledgePurchase
import com.android.billingclient.api.consumePurchase
import com.android.billingclient.api.queryPurchasesAsync
import com.lovelyy.app.data.PlusPlan
import com.lovelyy.app.data.PlusProducts
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import timber.log.Timber
import kotlin.coroutines.resume

/** Prices from Google Play, or null when the products could not be loaded (the UI then shows string prices). */
data class PlusCatalog(
    val yearly: String? = null,
    /** The first-year offer price on the yearly base plan (shown only in the 48 h window). */
    val yearlyFirstYear: String? = null,
    val monthly: String? = null,
    val gift: String? = null,
) {
    val available: Boolean get() = yearly != null || monthly != null || gift != null
}

/** A purchase as the app needs it (the plan comes from the obfuscated profile id set when it was launched). */
data class StorePurchase(
    val plan: PlusPlan,
    val token: String,
    val purchaseTime: Long,
    val autoRenew: Boolean,
    val acknowledged: Boolean,
)

sealed interface PurchaseEvent {
    data class Purchased(val purchase: StorePurchase) : PurchaseEvent
    /** Paid with a slow method (cash, bank): Plus opens when Google Play confirms it; restore picks it up. */
    data object Pending : PurchaseEvent
    data object Cancelled : PurchaseEvent
    /** [code] is the Play Billing response code (a number, no personal data) for analytics. */
    data class Failed(val code: Int) : PurchaseEvent
}

/**
 * Google Play Billing for Lovelyy Plus: connect, load the three products, launch a purchase, acknowledge or
 * consume it, and list what this Google account owns (restore). Knows nothing about Firestore: PlusViewModel
 * maps purchases to the couple's plus map (PlusRules.afterPurchase) and only then acknowledges them, so a
 * purchase that never reached Firestore is refunded by Google instead of being lost.
 */
class BillingRepository(context: Context) : PurchasesUpdatedListener {

    private val _events = MutableSharedFlow<PurchaseEvent>(extraBufferCapacity = 8)
    /** Results of purchases started with [launch] (Google Play answers through the listener, not the call). */
    val events: SharedFlow<PurchaseEvent> = _events.asSharedFlow()

    private val _catalog = MutableStateFlow(PlusCatalog())
    val catalog: StateFlow<PlusCatalog> = _catalog.asStateFlow()

    private val client: BillingClient = BillingClient.newBuilder(context.applicationContext)
        .setListener(this)
        .enablePendingPurchases(PendingPurchasesParams.newBuilder().enableOneTimeProducts().build())
        // Play Billing 8: reconnects by itself when Google Play drops the connection, so calls after a disconnect
        // don't fail with SERVICE_DISCONNECTED.
        .enableAutoServiceReconnection()
        .build()

    private val connectLock = Mutex()
    private var subDetails: ProductDetails? = null
    private var giftDetails: ProductDetails? = null

    /** Connects to Google Play once (and again after a disconnect). False when Play is not available. */
    suspend fun connect(): Boolean = connectLock.withLock {
        if (client.isReady) return@withLock true
        suspendCancellableCoroutine { cont ->
            client.startConnection(object : BillingClientStateListener {
                override fun onBillingSetupFinished(result: BillingResult) {
                    if (result.responseCode != BillingResponseCode.OK) Timber.w("Billing setup failed: %d", result.responseCode)
                    if (cont.isActive) cont.resume(result.responseCode == BillingResponseCode.OK)
                }

                override fun onBillingServiceDisconnected() {
                    if (cont.isActive) cont.resume(false)
                }
            })
        }
    }

    /** Loads prices. Keeps the last good catalog when Play is unreachable. */
    suspend fun loadCatalog(): PlusCatalog {
        try {
            if (!connect()) return _catalog.value
            val sub = query(PlusProducts.SUB, BillingClient.ProductType.SUBS)
            val gift = query(PlusProducts.GIFT, BillingClient.ProductType.INAPP)
            subDetails = sub ?: subDetails
            giftDetails = gift ?: giftDetails
            val offers = subDetails?.subscriptionOfferDetails.orEmpty()
            fun base(plan: String) = offers.firstOrNull { it.basePlanId == plan && it.offerId == null }
            val firstYear = offers.firstOrNull { it.basePlanId == PlusProducts.BASE_YEARLY && it.offerId == PlusProducts.OFFER_FIRST_YEAR }
            _catalog.value = PlusCatalog(
                yearly = base(PlusProducts.BASE_YEARLY)?.pricingPhases?.pricingPhaseList?.lastOrNull()?.formattedPrice,
                yearlyFirstYear = firstYear?.pricingPhases?.pricingPhaseList?.firstOrNull()?.formattedPrice,
                monthly = base(PlusProducts.BASE_MONTHLY)?.pricingPhases?.pricingPhaseList?.lastOrNull()?.formattedPrice,
                gift = giftDetails?.oneTimePurchaseOfferDetails?.formattedPrice,
            )
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Timber.w(e, "Loading Plus products failed")
        }
        return _catalog.value
    }

    /**
     * One product's details. Uses the callback API (Play Billing 8 hands it a QueryProductDetailsResult) instead of
     * the ktx wrapper, whose result type changed between major versions.
     */
    private suspend fun query(id: String, type: String): ProductDetails? {
        val params = QueryProductDetailsParams.newBuilder()
            .setProductList(listOf(QueryProductDetailsParams.Product.newBuilder().setProductId(id).setProductType(type).build()))
            .build()
        return suspendCancellableCoroutine { cont ->
            client.queryProductDetailsAsync(params) { result, details ->
                val product = if (result.responseCode == BillingResponseCode.OK) {
                    details.productDetailsList.firstOrNull()
                } else {
                    Timber.w("Product query failed: %d", result.responseCode)
                    null
                }
                if (product == null && result.responseCode == BillingResponseCode.OK) {
                    // Usually: the product isn't set up (or not active) in Play Console yet, see docs/play-console.md.
                    Timber.w("Product not found in Google Play: %s", id)
                }
                if (cont.isActive) cont.resume(product)
            }
        }
    }

    /**
     * Opens Google Play's purchase window. False when the product is not loaded or Play refused to open it
     * (the caller shows a friendly error). The result arrives later in [events].
     */
    suspend fun launch(activity: Activity, plan: PlusPlan, firstYearOffer: Boolean): Boolean {
        if (subDetails == null && giftDetails == null) loadCatalog()
        val params = when (plan) {
            PlusPlan.Gift -> {
                val details = giftDetails ?: return false
                BillingFlowParams.ProductDetailsParams.newBuilder().setProductDetails(details).build()
            }
            else -> {
                val details = subDetails ?: return false
                val basePlan = if (plan == PlusPlan.Yearly) PlusProducts.BASE_YEARLY else PlusProducts.BASE_MONTHLY
                val offers = details.subscriptionOfferDetails.orEmpty()
                val offer = offers.firstOrNull { firstYearOffer && plan == PlusPlan.Yearly && it.basePlanId == basePlan && it.offerId == PlusProducts.OFFER_FIRST_YEAR }
                    ?: offers.firstOrNull { it.basePlanId == basePlan && it.offerId == null }
                    ?: return false
                BillingFlowParams.ProductDetailsParams.newBuilder().setProductDetails(details).setOfferToken(offer.offerToken).build()
            }
        }
        val flow = BillingFlowParams.newBuilder()
            .setProductDetailsParamsList(listOf(params))
            // Purchases don't say which base plan was bought; this fixed key does (no personal data in it).
            .setObfuscatedProfileId(plan.name)
            .build()
        val result = client.launchBillingFlow(activity, flow)
        if (result.responseCode != BillingResponseCode.OK) Timber.w("Launching the purchase failed: %d", result.responseCode)
        return result.responseCode == BillingResponseCode.OK
    }

    override fun onPurchasesUpdated(result: BillingResult, purchases: MutableList<Purchase>?) {
        when (result.responseCode) {
            BillingResponseCode.OK -> purchases.orEmpty().forEach { p ->
                when (p.purchaseState) {
                    Purchase.PurchaseState.PURCHASED -> p.toStore()?.let { _events.tryEmit(PurchaseEvent.Purchased(it)) }
                    Purchase.PurchaseState.PENDING -> _events.tryEmit(PurchaseEvent.Pending)
                    else -> Unit
                }
            }
            BillingResponseCode.USER_CANCELED -> _events.tryEmit(PurchaseEvent.Cancelled)
            else -> {
                Timber.w("Purchase failed: %d", result.responseCode)
                _events.tryEmit(PurchaseEvent.Failed(result.responseCode))
            }
        }
    }

    /** Everything this Google account owns and has paid for (subscriptions and an unconsumed gift). Null if Play failed. */
    suspend fun owned(): List<StorePurchase>? {
        return try {
            if (!connect()) return null
            val subs = client.queryPurchasesAsync(QueryPurchasesParams.newBuilder().setProductType(BillingClient.ProductType.SUBS).build())
            val inapp = client.queryPurchasesAsync(QueryPurchasesParams.newBuilder().setProductType(BillingClient.ProductType.INAPP).build())
            if (subs.billingResult.responseCode != BillingResponseCode.OK || inapp.billingResult.responseCode != BillingResponseCode.OK) return null
            (subs.purchasesList + inapp.purchasesList)
                .filter { it.purchaseState == Purchase.PurchaseState.PURCHASED }
                .mapNotNull { it.toStore() }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Timber.w(e, "Querying purchases failed")
            null
        }
    }

    /** Tells Google the purchase was delivered: acknowledges a subscription, consumes the gift (so it can be bought again). */
    suspend fun finish(purchase: StorePurchase): Boolean {
        return try {
            if (!connect()) return false
            val code = if (purchase.plan == PlusPlan.Gift) {
                client.consumePurchase(ConsumeParams.newBuilder().setPurchaseToken(purchase.token).build()).billingResult.responseCode
            } else {
                if (purchase.acknowledged) return true
                client.acknowledgePurchase(AcknowledgePurchaseParams.newBuilder().setPurchaseToken(purchase.token).build()).responseCode
            }
            if (code != BillingResponseCode.OK) Timber.w("Finishing the purchase failed: %d", code)
            code == BillingResponseCode.OK
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Timber.w(e, "Finishing the purchase failed")
            false
        }
    }

    private fun Purchase.toStore(): StorePurchase? {
        val plan = when {
            PlusProducts.GIFT in products -> PlusPlan.Gift
            PlusProducts.SUB in products -> runCatching { PlusPlan.valueOf(accountIdentifiers?.obfuscatedProfileId.orEmpty()) }
                .getOrNull()?.takeIf { it.isSubscription } ?: PlusPlan.Yearly
            else -> return null
        }
        return StorePurchase(plan, purchaseToken, purchaseTime, isAutoRenewing, isAcknowledged)
    }

    companion object {
        /** Google Play's subscription page for this app (P8 "Aboneliği yönet"). */
        fun manageUrl(packageName: String) =
            "https://play.google.com/store/account/subscriptions?sku=${PlusProducts.SUB}&package=$packageName"
    }
}
