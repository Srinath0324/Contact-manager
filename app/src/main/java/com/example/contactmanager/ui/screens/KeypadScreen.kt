package com.example.contactmanager.ui.screens

import android.Manifest
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.contactmanager.model.CallType
import com.example.contactmanager.viewmodel.CallLogViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun KeypadScreen(
    callLogViewModel: CallLogViewModel = viewModel()
) {
    var number by remember { mutableStateOf("") }
    var isKeypadVisible by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val callLogs by callLogViewModel.callLogs.collectAsState()
    val permissionGranted by callLogViewModel.permissionGranted.collectAsState()

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            callLogViewModel.onPermissionGranted()
        }
    }

    // Request permission when the screen is first displayed
    LaunchedEffect(Unit) {
        if (!permissionGranted) {
            permissionLauncher.launch(Manifest.permission.READ_CALL_LOG)
        }
    }
    var searchQuery by remember { mutableStateOf("") }

    val filteredCallLogs = remember(callLogs, searchQuery) {
        if (searchQuery.isEmpty()) {
            callLogs
        } else {
            callLogs.filter { callLog ->
                callLog.phoneNumber.contains(searchQuery, ignoreCase = true) ||
                        (callLog.contactName?.contains(searchQuery, ignoreCase = true) ?: false)
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        // Recent Calls List
        Column(modifier = Modifier.fillMaxSize()) {
            // Search bar for filtering recent calls
            SearchBar(
                query = searchQuery,
                onQueryChange = { searchQuery = it},
                onSearch = { },
                active = false,
                onActiveChange = { },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                placeholder = { Text("Search contacts & places") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search") }
            ) { }

            // Recent calls list
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 80.dp) // Add padding for FAB
            ) {
                items(filteredCallLogs) { callLog ->
                    RecentCallItem(
                        callLog = callLog,
                        onCallClick = { makePhoneCall(context, callLog.phoneNumber) }
                    )
                }
            }
        }

        // Keypad FAB
        FloatingActionButton(
            onClick = { isKeypadVisible = true },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp),
            containerColor = MaterialTheme.colorScheme.primaryContainer
        ) {
            Icon(Icons.Default.Dialpad, contentDescription = "Open keypad")
        }

        // Animated Keypad Dialog
        AnimatedVisibility(
            visible = isKeypadVisible,
            enter = slideInVertically(initialOffsetY = { it }),
            exit = slideOutVertically(targetOffsetY = { it })
        ) {
            Surface(
                modifier = Modifier.fillMaxSize(),
                color = MaterialTheme.colorScheme.background
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth().fillMaxHeight(0.75f)
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Spacer(modifier = Modifier.height(100.dp))

                    IconButton(
                        onClick = { isKeypadVisible = false },
                        modifier = Modifier.align(Alignment.Start)
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "Close keypad")
                    }


                    Text(
                        text = number,
                        fontSize = 32.sp,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 32.dp),
                        textAlign = TextAlign.Center
                    )

                    KeypadGrid(
                        onNumberClick = { digit ->
                            if (number.length < 15) {
                                number += digit
                            }
                        },
                        onDeleteClick = {
                            if (number.isNotEmpty()) {
                                number = number.dropLast(1)
                            }
                        }
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    FloatingActionButton(
                        onClick = {
                            if (number.isNotEmpty()) {
                                makePhoneCall(context, number)
                            }
                        },
                        containerColor = MaterialTheme.colorScheme.primary,
                        shape = CircleShape
                    ) {
                        Icon(Icons.Default.Call, contentDescription = "Call")
                    }
                }
            }
        }
    }
}

@Composable
private fun RecentCallItem(
    callLog: com.example.contactmanager.model.CallLog,
    onCallClick: () -> Unit
) {
    val dateFormat = SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault())

    ListItem(
        headlineContent = {
            Text(
                text = callLog.contactName ?: callLog.phoneNumber,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        },
        supportingContent = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = when (callLog.type) {
                        CallType.INCOMING -> Icons.Default.CallReceived
                        CallType.OUTGOING -> Icons.Default.CallMade
                        CallType.MISSED -> Icons.Default.CallMissed
                    },
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = when (callLog.type) {
                        CallType.MISSED -> MaterialTheme.colorScheme.error
                        else -> MaterialTheme.colorScheme.onSurfaceVariant
                    }
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = dateFormat.format(callLog.date),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        trailingContent = {
            IconButton(onClick = onCallClick) {
                Icon(
                    Icons.Default.Call,
                    contentDescription = "Call",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }
    )
}

@Composable
private fun KeypadGrid(
    onNumberClick: (String) -> Unit,
    onDeleteClick: () -> Unit
) {
    val buttons = listOf(
        "1", "2", "3",
        "4", "5", "6",
        "7", "8", "9",
        "*", "0", "#"
    )

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        buttons.chunked(3).forEach { row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                row.forEach { button ->
                    KeypadButton(
                        text = button,
                        onClick = { onNumberClick(button) }
                    )
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }

        IconButton(
            onClick = onDeleteClick,
            modifier = Modifier.size(48.dp)
        ) {
            Icon(Icons.Default.Backspace, contentDescription = "Delete")
        }
    }
}

@Composable
private fun KeypadButton(
    text: String,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        modifier = Modifier.size(72.dp),
        shape = CircleShape,
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
            contentColor = MaterialTheme.colorScheme.onSurfaceVariant
        )
    ) {
        Text(text = text, fontSize = 24.sp)
    }
}

private fun makePhoneCall(context: Context, number: String) {
    try {
        val intent = Intent(Intent.ACTION_CALL).apply {
            data = Uri.parse("tel:$number")
        }
        context.startActivity(intent)
    } catch (e: SecurityException) {
        Toast.makeText(
            context,
            "Permission needed to make calls",
            Toast.LENGTH_SHORT
        ).show()
    } catch (e: ActivityNotFoundException) {
        Toast.makeText(
            context,
            "Could not make phone call",
            Toast.LENGTH_SHORT
        ).show()
    }
}