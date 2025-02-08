package com.example.contactmanager.ui.screens

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CallMade
import androidx.compose.material.icons.filled.CallMissed
import androidx.compose.material.icons.filled.CallReceived
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Call
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.contactmanager.model.CallLog
import com.example.contactmanager.model.CallType
import com.example.contactmanager.viewmodel.CallLogViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CallLogsScreen(
    viewModel: CallLogViewModel = viewModel()
) {
    var searchQuery by remember { mutableStateOf("") }
    val callLogs by viewModel.callLogs.collectAsState()
    val context = LocalContext.current

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

    Column(modifier = Modifier.fillMaxSize()) {
        SearchBar(
            query = searchQuery,
            onQueryChange = { searchQuery = it },
            onSearch = { },
            active = false,
            onActiveChange = { },
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            placeholder = { Text("Search call logs...") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search") }
        ) { }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(vertical = 8.dp)
        ) {
            items(filteredCallLogs) { callLog ->
                CallLogItem(
                    callLog = callLog,
                    onCallClick = { makePhoneCall(context, callLog.phoneNumber) }
                )
            }
        }
    }
}

@Composable
private fun CallLogItem(
    callLog: CallLog,
    onCallClick: () -> Unit
) {
    val dateFormat = SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault())

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = when (callLog.type) {
                    CallType.INCOMING -> Icons.Default.CallReceived
                    CallType.OUTGOING -> Icons.Default.CallMade
                    CallType.MISSED -> Icons.Default.CallMissed
                },
                contentDescription = null,
                tint = when (callLog.type) {
                    CallType.MISSED -> MaterialTheme.colorScheme.error
                    else -> MaterialTheme.colorScheme.primary
                }
            )

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = callLog.contactName ?: callLog.phoneNumber,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = dateFormat.format(callLog.date),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            IconButton(onClick = onCallClick) {
                Icon(
                    Icons.Default.Call,
                    contentDescription = "Call",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }
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