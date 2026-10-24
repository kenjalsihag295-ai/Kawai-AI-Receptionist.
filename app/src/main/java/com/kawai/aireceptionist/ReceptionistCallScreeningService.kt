package com.kawai.aireceptionist

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.ContactsContract
import android.telecom.Call
import android.telecom.CallScreeningService
import android.app.Notification
import android.app.PendingIntent
import android.content.Intent

class ReceptionistCallScreeningService : CallScreeningService() {
    override fun onScreenCall(callDetails: Call.Details) {
        val number = callDetails.handle?.schemeSpecificPart.orEmpty()
        val isIncoming = callDetails.callDirection == Call.Details.DIRECTION_INCOMING
        val savedName = if (isIncoming) findContactName(number) else null

        // Safe default: never block a call in this prototype.
        val response = CallResponse.Builder()
            .setDisallowCall(false)
            .setRejectCall(false)
            .setSilenceCall(false)
            .setSkipCallLog(false)
            .setSkipNotification(false)
            .build()

        try {
            respondToCall(callDetails, response)
        } catch (_: Exception) {
            return
        }

        if (isIncoming && savedName.isNullOrBlank()) {
            showUnknownCallNotice(number)
        }
    }

    private fun findContactName(number: String): String? {
        if (number.isBlank() ||
            checkSelfPermission(Manifest.permission.READ_CONTACTS) != PackageManager.PERMISSION_GRANTED) return null
        return try {
            val uri = Uri.withAppendedPath(
                ContactsContract.PhoneLookup.CONTENT_FILTER_URI,
                Uri.encode(number)
            )
            contentResolver.query(
                uri,
                arrayOf(ContactsContract.PhoneLookup.DISPLAY_NAME),
                null, null, null
            )?.use { cursor ->
                if (cursor.moveToFirst()) cursor.getString(0) else null
            }
        } catch (_: Exception) { null }
    }

    private fun showUnknownCallNotice(number: String) {
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channelId = "unknown_call_screening"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            manager.createNotificationChannel(
                NotificationChannel(channelId, "Unknown call alerts", NotificationManager.IMPORTANCE_DEFAULT)
            )
        }
        val openApp = PendingIntent.getActivity(
            this, 1, Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val notification = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Notification.Builder(this, channelId)
        } else {
            @Suppress("DEPRECATION")
            Notification.Builder(this)
        }
            .setSmallIcon(android.R.drawable.sym_call_incoming)
            .setContentTitle("Unknown caller screened")
            .setContentText(if (number.isBlank()) "Unknown number" else number)
            .setStyle(Notification.BigTextStyle().bigText(
                "An incoming number not found in your contacts called: ${number.ifBlank { "Unknown number" }}. " +
                "This prototype did not answer the call or analyze the caller's voice."
            ))
            .setContentIntent(openApp)
            .setAutoCancel(true)
            .build()
        try { manager.notify((number.hashCode() and 0x7fffffff), notification) } catch (_: SecurityException) { }
    }
}
