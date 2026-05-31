package com.example.data

import android.content.Context
import android.util.Log
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.HydrationRecord
import androidx.health.connect.client.units.Volume
import java.time.Instant
import java.time.ZoneOffset
import java.util.TimeZone

object HealthConnectManager {
    private const val TAG = "HealthConnectManager"

    val permissions = setOf(
        HealthPermission.getWritePermission(HydrationRecord::class),
        HealthPermission.getReadPermission(HydrationRecord::class)
    )

    fun isSdkAvailable(context: Context): Boolean {
        return try {
            val status = HealthConnectClient.getSdkStatus(context)
            status == HealthConnectClient.SDK_AVAILABLE
        } catch (e: Exception) {
            Log.e(TAG, "Health Connect SDK availability check failed: ${e.message}")
            false
        }
    }

    suspend fun hasAllPermissions(context: Context): Boolean {
        if (!isSdkAvailable(context)) return false
        return try {
            val client = HealthConnectClient.getOrCreate(context)
            val granted = client.permissionController.getGrantedPermissions()
            granted.containsAll(permissions)
        } catch (e: Exception) {
            Log.e(TAG, "Failed checking Health Connect permissions: ${e.message}")
            false
        }
    }

    suspend fun writeHydration(context: Context, amountMl: Double, timestamp: Long): Boolean {
        if (!isSdkAvailable(context)) {
            Log.w(TAG, "Health Connect SDK is not available.")
            return false
        }
        return try {
            val client = HealthConnectClient.getOrCreate(context)
            val offsetSeconds = TimeZone.getDefault().getOffset(timestamp) / 1000
            val zoneOffset = ZoneOffset.ofTotalSeconds(offsetSeconds)
            
            val hydrationRecord = HydrationRecord(
                startTime = Instant.ofEpochMilli(timestamp),
                endTime = Instant.ofEpochMilli(timestamp + 1000),
                startZoneOffset = zoneOffset,
                endZoneOffset = zoneOffset,
                volume = Volume.milliliters(amountMl)
            )
            
            client.insertRecords(listOf(hydrationRecord))
            Log.i(TAG, "Successfully wrote hydration record to Health Connect: $amountMl ml")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error writing hydration to Health Connect: ${e.message}")
            false
        }
    }
}
