package com.kawai.aireceptionist

import android.Manifest
import android.app.Activity
import android.app.AlertDialog
import android.app.role.RoleManager
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.ContactsContract
import android.view.Gravity
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast

class MainActivity : Activity() {
    private lateinit var status: TextView
    private lateinit var contactsCount: TextView
    private val requestContacts = 100
    private val requestNotifications = 101
    private val requestScreeningRole = 102

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        buildUi()
        refreshStatus()
    }

    private fun buildUi() {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(28, 36, 28, 24)
            setBackgroundColor(0xFFF7F7FB.toInt())
        }
        val scroll = ScrollView(this)
        val content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(0, 0, 0, 24)
        }
        val title = TextView(this).apply {
            text = "Kawai AI Receptionist"
            textSize = 26f
            setTextColor(0xFF171923.toInt())
            gravity = Gravity.CENTER_VERTICAL
        }
        val subtitle = TextView(this).apply {
            text = "Your call assistant • Early prototype"
            textSize = 15f
            setTextColor(0xFF555B68.toInt())
            setPadding(0, 8, 0, 22)
        }
        status = TextView(this).apply {
            textSize = 16f
            setTextColor(0xFF202431.toInt())
            setPadding(16, 16, 16, 16)
            setBackgroundColor(0xFFE8EAF3.toInt())
        }
        contactsCount = TextView(this).apply {
            textSize = 16f
            setTextColor(0xFF202431.toInt())
            setPadding(0, 18, 0, 18)
        }
        content.addView(title)
        content.addView(subtitle)
        content.addView(status)
        content.addView(contactsCount)
        content.addView(actionButton("1. Allow contact access") { requestContactPermission() })
        content.addView(actionButton("2. Enable call screening") { requestScreeningRole() })
        content.addView(actionButton("Refresh status") { refreshStatus() })
        content.addView(actionButton("What this version can do") {
            AlertDialog.Builder(this)
                .setTitle("Prototype limits")
                .setMessage(
                    "This build can request contact access, ask to become your call-screening app, " +
                    "allow incoming calls, and notify you about unknown numbers when Android invokes " +
                    "the screening service. It does NOT answer calls, talk to callers, record audio, " +
                    "or use AI yet. Android normally blocks ordinary apps from capturing both sides " +
                    "of cellular-call audio."
                )
                .setPositiveButton("Got it", null)
                .show()
        })
        val note = TextView(this).apply {
            text = "Privacy: contacts are checked on this phone. This prototype does not upload contacts."
            textSize = 13f
            setTextColor(0xFF656B78.toInt())
            setPadding(0, 22, 0, 0)
        }
        content.addView(note)
        scroll.addView(content)
        root.addView(scroll, LinearLayout.LayoutParams(-1, 0, 1f))
        setContentView(root)
    }

    private fun actionButton(label: String, action: () -> Unit): Button =
        Button(this).apply {
            text = label
            isAllCaps = false
            setOnClickListener { action() }
            val lp = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
            lp.setMargins(0, 8, 0, 8)
            layoutParams = lp
        }

    private fun requestContactPermission() {
        if (checkSelfPermission(Manifest.permission.READ_CONTACTS) == PackageManager.PERMISSION_GRANTED) {
            refreshStatus()
        } else {
            requestPermissions(arrayOf(Manifest.permission.READ_CONTACTS), requestContacts)
        }
    }

    private fun requestScreeningRole() {
        val roleManager = getSystemService(RoleManager::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q &&
            roleManager.isRoleAvailable(RoleManager.ROLE_CALL_SCREENING)) {
            if (roleManager.isRoleHeld(RoleManager.ROLE_CALL_SCREENING)) {
                Toast.makeText(this, "Call screening is already enabled.", Toast.LENGTH_SHORT).show()
            } else {
                startActivityForResult(
                    roleManager.createRequestRoleIntent(RoleManager.ROLE_CALL_SCREENING),
                    requestScreeningRole
                )
            }
        } else {
            AlertDialog.Builder(this)
                .setMessage("This device does not expose Android's call-screening role.")
                .setPositiveButton("OK", null).show()
        }
    }

    private fun refreshStatus() {
        val hasContacts = checkSelfPermission(Manifest.permission.READ_CONTACTS) == PackageManager.PERMISSION_GRANTED
        val roleManager = getSystemService(RoleManager::class.java)
        val hasRole = Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q &&
            roleManager.isRoleAvailable(RoleManager.ROLE_CALL_SCREENING) &&
            roleManager.isRoleHeld(RoleManager.ROLE_CALL_SCREENING)
        status.text = "Contact permission: ${if (hasContacts) "Granted" else "Not granted"}\n" +
            "Call screening role: ${if (hasRole) "Enabled" else "Not enabled"}\n" +
            "AI voice receptionist: Not available in this prototype"
        contactsCount.text = if (hasContacts) {
            "Contacts found on device: ${countContacts()}"
        } else {
            "Grant contact access to enable contact-aware screening."
        }
    }

    private fun countContacts(): Int {
        return try {
            contentResolver.query(
                ContactsContract.Contacts.CONTENT_URI,
                arrayOf(ContactsContract.Contacts._ID),
                null, null, null
            )?.use { it.count } ?: 0
        } catch (_: SecurityException) { 0 }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == requestContacts || requestCode == requestNotifications) refreshStatus()
    }

    @Deprecated("Uses role request callback for broad Android compatibility")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == requestScreeningRole) refreshStatus()
    }

    override fun onResume() {
        super.onResume()
        if (::status.isInitialized) refreshStatus()
    }
}
