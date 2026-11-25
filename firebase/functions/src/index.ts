/**
 * Lovelyy Cloud Functions.
 *
 * Step 1 (login + pairing + home) does NOT need any function: pairing is done
 * on the client and protected by firestore.rules, so the free Spark plan works.
 *
 * Functions will be added for features that need a trusted server, e.g.:
 *  - heartbeat / "thinking of you" push notifications (FCM)
 *  - daily question rotation
 *  - milestone celebrations (100 days, 1 year)
 * Deploying functions requires the Blaze (pay-as-you-go) plan.
 */
import { onCall } from "firebase-functions/v2/https";
import { setGlobalOptions } from "firebase-functions/v2";

setGlobalOptions({ region: "europe-west1", maxInstances: 5 });

/** Simple health check so the emulator has something to run. */
export const ping = onCall(() => ({ ok: true, app: "lovelyy" }));
