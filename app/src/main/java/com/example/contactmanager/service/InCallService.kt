package com.example.contactmanager.service

import android.content.Intent
import android.os.Build
import android.telecom.Call
import android.telecom.InCallService
import android.telecom.CallAudioState
import androidx.annotation.RequiresApi

@RequiresApi(Build.VERSION_CODES.M)
class InCallService : InCallService() {
    private val calls = mutableMapOf<Call, Call.Callback>()

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // Launch system dialer in a separate intent
        val dialerIntent = Intent(Intent.ACTION_DIAL).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        startActivity(dialerIntent)

        // Return appropriate service start flag
        return START_NOT_STICKY
    }

    override fun onCallAdded(call: Call) {
        super.onCallAdded(call)

        // Start the system's in-call UI
        val systemDialerPackage = getSystemDialerPackage()
        if (systemDialerPackage != null) {
            val inCallIntent = packageManager.getLaunchIntentForPackage(systemDialerPackage)?.apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                action = Intent.ACTION_MAIN
                addCategory(Intent.CATEGORY_DEFAULT)
            }
            inCallIntent?.let { startActivity(it) }
        }

        // Create and register callback
        val callback = object : Call.Callback() {
            override fun onStateChanged(call: Call, state: Int) {
                when (state) {
                    Call.STATE_RINGING -> {
                        // Launch system UI for incoming call
                        launchSystemInCallUI()
                    }
                    Call.STATE_CONNECTING, Call.STATE_DIALING -> {
                        // Launch system UI for outgoing call
                        launchSystemInCallUI()
                    }
                    Call.STATE_DISCONNECTED -> {
                        calls.remove(call)
                        call.unregisterCallback(this)
                    }
                }
            }
        }

        calls[call] = callback
        call.registerCallback(callback)
    }

    override fun onCallRemoved(call: Call) {
        super.onCallRemoved(call)
        calls[call]?.let { callback ->
            call.unregisterCallback(callback)
        }
        calls.remove(call)
    }

    private fun getSystemDialerPackage(): String? {
        val dialerIntent = Intent(Intent.ACTION_DIAL)
        val resolveInfoList = packageManager.queryIntentActivities(dialerIntent, 0)

        // Find the system dialer package (usually com.android.dialer or similar)
        return resolveInfoList.firstOrNull { resolveInfo ->
            resolveInfo.activityInfo.applicationInfo.flags and android.content.pm.ApplicationInfo.FLAG_SYSTEM != 0
        }?.activityInfo?.packageName
    }

    private fun launchSystemInCallUI() {
        val systemDialerPackage = getSystemDialerPackage()
        if (systemDialerPackage != null) {
            try {
                // Try to launch the in-call screen
                val inCallIntent = Intent("android.intent.action.SHOW_CALL_SCREEN").apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_NO_USER_ACTION)
                    `package` = systemDialerPackage
                }
                startActivity(inCallIntent)
            } catch (e: Exception) {
                // Fallback to launching the dialer app if in-call screen launch fails
                val dialerIntent = packageManager.getLaunchIntentForPackage(systemDialerPackage)?.apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                dialerIntent?.let { startActivity(it) }
            }
        }
    }
}