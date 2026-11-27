package com.yumy.intruderselfie

import android.app.admin.DeviceAdminReceiver
import android.content.Context
import android.content.Intent

/** Receives only password challenge events explicitly delivered by Android. */
class IntrusionAdminReceiver : DeviceAdminReceiver() {
    override fun onPasswordFailed(context: Context, intent: Intent) {
        EventStore.record(context, "PASSWORD_FAILED", "Android reported an incorrect device/profile challenge password")
    }

    override fun onPasswordSucceeded(context: Context, intent: Intent) {
        EventStore.record(context, "PASSWORD_SUCCEEDED", "Android reported a successful device/profile challenge")
    }
}
