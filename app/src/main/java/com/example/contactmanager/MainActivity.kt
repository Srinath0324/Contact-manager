package com.example.contactmanager

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.contactmanager.ui.theme.ContactManagerTheme
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Contacts
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Dialpad
import com.example.contactmanager.ui.screens.KeypadScreen
import com.example.contactmanager.ui.screens.CallLogsScreen
import com.example.contactmanager.ui.screens.ContactsScreen
import androidx.core.content.ContextCompat
import android.widget.Toast
import android.telecom.TelecomManager
import android.content.Intent
import android.os.Build
import android.app.role.RoleManager
import android.os.Build.VERSION.SDK_INT
import android.os.Build.VERSION_CODES
import androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult
import android.content.Context
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.Dispatchers
import androidx.lifecycle.lifecycleScope
import android.app.Application
import androidx.lifecycle.ViewModelProvider
import com.example.contactmanager.viewmodel.CallLogViewModel

class MainActivity : ComponentActivity() {
    private val permissions = arrayOf(
        Manifest.permission.READ_CONTACTS,
        Manifest.permission.READ_CALL_LOG,
        Manifest.permission.CALL_PHONE,
        Manifest.permission.READ_PHONE_STATE,
        Manifest.permission.ANSWER_PHONE_CALLS,
        Manifest.permission.MANAGE_OWN_CALLS
    )

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        if (permissions.all { it.value }) {
            // All permissions granted, now request default dialer
            if (!isDefaultDialer()) {
                requestDefaultDialer()
            }
            // Refresh call logs after permissions are granted
            (application as? Application)?.let { app ->
                ViewModelProvider(
                    this,
                    ViewModelProvider.AndroidViewModelFactory(app)
                )[CallLogViewModel::class.java].refreshCallLogs()
            }
        } else {
            Toast.makeText(
                this,
                "All permissions are required for the app to function properly",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    private val roleRequestLauncher = registerForActivityResult(StartActivityForResult()) { result ->
        when (result.resultCode) {
            RESULT_OK -> {
                Toast.makeText(this, "Successfully set as default dialer", Toast.LENGTH_SHORT).show()
            }
            else -> {
                Toast.makeText(this, "Failed to set as default set manually in Settings > Apps > Default apps", Toast.LENGTH_LONG).show()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Move permission and dialer checks to a coroutine
        lifecycleScope.launch {
            withContext(Dispatchers.IO) {
                if (!hasRequiredPermissions()) {
                    withContext(Dispatchers.Main) {
                        permissionLauncher.launch(permissions)
                    }
                } else if (!isDefaultDialer()) {
                    withContext(Dispatchers.Main) {
                        requestDefaultDialer()
                    }
                }
            }
        }

        setContent {
            ContactManagerTheme {
                val navController = rememberNavController()
                var selectedItem by remember { mutableStateOf(0) }

                val items = listOf(
                    "Keypad" to Icons.Default.Dialpad,
                    "Calls" to Icons.Default.History,
                    "Contacts" to Icons.Default.Contacts
                )

                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    bottomBar = {
                        NavigationBar {
                            items.forEachIndexed { index, (title, icon) ->
                                NavigationBarItem(
                                    icon = { Icon(icon, contentDescription = title) },
                                    label = { Text(title) },
                                    selected = selectedItem == index,
                                    onClick = {
                                        selectedItem = index
                                        navController.navigate(title) {
                                            popUpTo(navController.graph.startDestinationId)
                                            launchSingleTop = true
                                        }
                                    }
                                )
                            }
                        }
                    }
                ) { innerPadding ->
                    NavHost(
                        navController = navController,
                        startDestination = "Keypad",
                        modifier = Modifier.padding(innerPadding)
                    ) {
                        composable("Keypad") {
                            KeypadScreen()
                        }
                        composable("Calls") {
                            CallLogsScreen()
                        }
                        composable("Contacts") {
                            ContactsScreen()
                        }
                    }
                }
            }
        }
    }

    private fun hasRequiredPermissions(): Boolean {
        return permissions.all { permission ->
            ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED
        }
    }

    private fun isDefaultDialer(): Boolean {
        val telecomManager = getSystemService(TELECOM_SERVICE) as TelecomManager
        return packageName == telecomManager.defaultDialerPackage
    }

    private fun requestDefaultDialer() {
        try {
            if (SDK_INT >= VERSION_CODES.Q) {
                val roleManager = getSystemService(Context.ROLE_SERVICE) as RoleManager
                if (roleManager.isRoleAvailable(RoleManager.ROLE_DIALER)) {
                    if (!roleManager.isRoleHeld(RoleManager.ROLE_DIALER)) {
                        roleRequestLauncher.launch(roleManager.createRequestRoleIntent(RoleManager.ROLE_DIALER))
                    }
                } else {
                    Toast.makeText(this, "Dialer role not available", Toast.LENGTH_SHORT).show()
                }
            } else {
                val intent = Intent(TelecomManager.ACTION_CHANGE_DEFAULT_DIALER)
                    .putExtra(TelecomManager.EXTRA_CHANGE_DEFAULT_DIALER_PACKAGE_NAME, packageName)
                roleRequestLauncher.launch(intent)
            }
        } catch (e: Exception) {
            Toast.makeText(this, "Error setting default dialer: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    private fun showError(message: String) {
        lifecycleScope.launch(Dispatchers.Main) {
            Toast.makeText(this@MainActivity, message, Toast.LENGTH_LONG).show()
        }
    }
}