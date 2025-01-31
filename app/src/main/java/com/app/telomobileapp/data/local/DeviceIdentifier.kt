package com.app.telomobileapp.data.local

import android.content.Context
import android.provider.Settings

class DeviceIdentifier(private val context: Context) {
    fun getDeviceId(): String {
        return Settings.Secure.getString(
            context.contentResolver,
            Settings.Secure.ANDROID_ID
        )
    }
}