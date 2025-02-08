package com.example.contactmanager.viewmodel

import android.Manifest
import android.app.Application
import android.content.ContentResolver
import android.content.pm.PackageManager
import android.database.Cursor
import android.provider.CallLog
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.contactmanager.model.CallType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.util.Date

class CallLogViewModel(application: Application) : AndroidViewModel(application) {
    private val _callLogs = MutableStateFlow<List<com.example.contactmanager.model.CallLog>>(emptyList())
    val callLogs: StateFlow<List<com.example.contactmanager.model.CallLog>> = _callLogs

    private val _permissionGranted = MutableStateFlow(false)
    val permissionGranted: StateFlow<Boolean> = _permissionGranted

    init {
        checkPermission()
    }

    private fun checkPermission() {
        val hasPermission = ContextCompat.checkSelfPermission(
            getApplication(),
            Manifest.permission.READ_CALL_LOG
        ) == PackageManager.PERMISSION_GRANTED

        _permissionGranted.value = hasPermission
        if (hasPermission) {
            loadCallLogs()
        }
    }

    fun onPermissionGranted() {
        _permissionGranted.value = true
        loadCallLogs()
    }

    private fun loadCallLogs() {
        if (!_permissionGranted.value) {
            return
        }

        viewModelScope.launch {
            val contentResolver: ContentResolver = getApplication<Application>().contentResolver
            val cursor: Cursor? = contentResolver.query(
                CallLog.Calls.CONTENT_URI,
                arrayOf(
                    CallLog.Calls._ID,
                    CallLog.Calls.NUMBER,
                    CallLog.Calls.CACHED_NAME,
                    CallLog.Calls.DATE,
                    CallLog.Calls.DURATION,
                    CallLog.Calls.TYPE
                ),
                null,
                null,
                "${CallLog.Calls.DATE} DESC"
            )

            val callLogsList = mutableListOf<com.example.contactmanager.model.CallLog>()
            cursor?.use {
                while (cursor.moveToNext()) {
                    val id = cursor.getString(0)
                    val number = cursor.getString(1)
                    val name = cursor.getString(2)
                    val date = Date(cursor.getLong(3))
                    val duration = cursor.getLong(4)
                    val type = when (cursor.getInt(5)) {
                        CallLog.Calls.INCOMING_TYPE -> CallType.INCOMING
                        CallLog.Calls.OUTGOING_TYPE -> CallType.OUTGOING
                        CallLog.Calls.MISSED_TYPE -> CallType.MISSED
                        else -> CallType.MISSED
                    }

                    callLogsList.add(
                        com.example.contactmanager.model.CallLog(
                            id = id,
                            phoneNumber = number,
                            contactName = name,
                            date = date,
                            duration = duration,
                            type = type
                        )
                    )
                }
            }
            _callLogs.value = callLogsList
        }
    }

    fun refreshCallLogs() {
        if (_permissionGranted.value) {
            loadCallLogs()
        }
    }
}