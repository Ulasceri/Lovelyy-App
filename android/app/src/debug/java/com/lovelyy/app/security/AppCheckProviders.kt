package com.lovelyy.app.security

import com.google.firebase.appcheck.AppCheckProviderFactory
import com.google.firebase.appcheck.debug.DebugAppCheckProviderFactory

/**
 * Debug builds: the debug provider. On first launch it logs a token to logcat
 * ("Enter this debug secret into the allow list in the Firebase Console ..."); register it under
 * App Check > Apps > Manage debug tokens, or the emulator is refused once enforcement is on.
 */
object AppCheckProviders {
    fun factory(): AppCheckProviderFactory = DebugAppCheckProviderFactory.getInstance()
}
