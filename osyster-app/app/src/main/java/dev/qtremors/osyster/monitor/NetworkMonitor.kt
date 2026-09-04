package dev.qtremors.osyster.monitor

import android.app.AppOpsManager
import android.app.usage.NetworkStats
import android.app.usage.NetworkStatsManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import android.net.ConnectivityManager
import android.net.TrafficStats
import android.net.Uri
import android.os.Build
import android.os.Process
import android.provider.Settings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap

// =========================================================================
// Section Comment: Network & Data Usage Models
// =========================================================================

enum class NetworkInterval {
    DAY,
    WEEK,
    MONTH
}

enum class NetworkInterfaceFilter {
    ALL,
    MOBILE,
    WIFI
}

data class NetworkBucket(
    val startTimeMillis: Long,
    val endTimeMillis: Long,
    val rxBytes: Long,
    val txBytes: Long,
    val totalBytes: Long,
    val label: String
)

data class AppNetworkUsage(
    val uid: Int,
    val packageName: String,
    val appName: String,
    val rxBytes: Long,
    val txBytes: Long,
    val totalBytes: Long,
    val mobileBytes: Long = 0L,
    val wifiBytes: Long = 0L,
    val icon: Drawable? = null
)

data class NetworkUsageSummary(
    val downloadBytes: Long,
    val uploadBytes: Long,
    val mobileBytes: Long,
    val wifiBytes: Long,
    val totalBytes: Long,
    val timeline: List<NetworkBucket>,
    val apps: List<AppNetworkUsage>
)

data class RealtimeSpeed(
    val rxBytesPerSec: Long,
    val txBytesPerSec: Long
)

// =========================================================================
// Section Comment: Network Monitoring Engine
// =========================================================================

@Suppress("DEPRECATION")
object NetworkMonitor {

    private data class CachedAppMeta(
        val appName: String,
        val packageName: String,
        val icon: Drawable?
    )

    private val appMetaCache = ConcurrentHashMap<Int, CachedAppMeta>()

    fun hasUsageAccess(context: Context): Boolean {
        val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as? AppOpsManager ?: return false
        val mode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            appOps.unsafeCheckOpNoThrow(
                AppOpsManager.OPSTR_GET_USAGE_STATS,
                Process.myUid(),
                context.packageName
            )
        } else {
            appOps.checkOpNoThrow(
                AppOpsManager.OPSTR_GET_USAGE_STATS,
                Process.myUid(),
                context.packageName
            )
        }
        return mode == AppOpsManager.MODE_ALLOWED
    }

    fun openUsageAccessSettings(context: Context) {
        val intent = Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS).apply {
            data = Uri.fromParts("package", context.packageName, null)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        runCatching {
            context.startActivity(intent)
        }.onFailure {
            val fallbackIntent = Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            runCatching { context.startActivity(fallbackIntent) }
        }
    }

    fun hasPhonePermission(context: Context): Boolean {
        return context.checkSelfPermission(android.Manifest.permission.READ_PHONE_STATE) == PackageManager.PERMISSION_GRANTED
    }

    fun getActiveNetworkType(context: Context): NetworkInterfaceFilter {
        return try {
            val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager ?: return NetworkInterfaceFilter.ALL
            val activeNetwork = cm.activeNetwork ?: return NetworkInterfaceFilter.ALL
            val caps = cm.getNetworkCapabilities(activeNetwork) ?: return NetworkInterfaceFilter.ALL
            when {
                caps.hasTransport(android.net.NetworkCapabilities.TRANSPORT_WIFI) -> NetworkInterfaceFilter.WIFI
                caps.hasTransport(android.net.NetworkCapabilities.TRANSPORT_CELLULAR) -> NetworkInterfaceFilter.MOBILE
                caps.hasTransport(android.net.NetworkCapabilities.TRANSPORT_ETHERNET) -> NetworkInterfaceFilter.WIFI
                else -> NetworkInterfaceFilter.ALL
            }
        } catch (_: Exception) {
            NetworkInterfaceFilter.ALL
        }
    }

    fun streamRealtimeSpeed(intervalMs: Long = 1000L): Flow<RealtimeSpeed> = flow {
        var lastRx = TrafficStats.getTotalRxBytes()
        var lastTx = TrafficStats.getTotalTxBytes()
        var lastTime = System.currentTimeMillis()

        while (true) {
            delay(intervalMs)
            val now = System.currentTimeMillis()
            val currentRx = TrafficStats.getTotalRxBytes()
            val currentTx = TrafficStats.getTotalTxBytes()

            val timeDeltaSec = (now - lastTime).coerceAtLeast(1L) / 1000.0
            val rxDelta = (currentRx - lastRx).coerceAtLeast(0L)
            val txDelta = (currentTx - lastTx).coerceAtLeast(0L)

            val rxSpeed = if (timeDeltaSec > 0) (rxDelta / timeDeltaSec).toLong() else 0L
            val txSpeed = if (timeDeltaSec > 0) (txDelta / timeDeltaSec).toLong() else 0L

            lastRx = currentRx
            lastTx = currentTx
            lastTime = now

            emit(RealtimeSpeed(rxBytesPerSec = rxSpeed, txBytesPerSec = txSpeed))
        }
    }.flowOn(Dispatchers.IO)

    suspend fun queryNetworkUsage(
        context: Context,
        interval: NetworkInterval,
        filter: NetworkInterfaceFilter,
        targetDateMillis: Long
    ): NetworkUsageSummary = withContext(Dispatchers.IO) {
        val networkStatsManager = context.getSystemService(Context.NETWORK_STATS_SERVICE) as? NetworkStatsManager
            ?: return@withContext emptySummary()

        val calendar = Calendar.getInstance()
        calendar.timeInMillis = targetDateMillis

        val rangeStart: Long
        val rangeEnd: Long
        val timelineBuckets = mutableListOf<NetworkBucket>()

        when (interval) {
            NetworkInterval.DAY -> {
                calendar.set(Calendar.HOUR_OF_DAY, 0)
                calendar.set(Calendar.MINUTE, 0)
                calendar.set(Calendar.SECOND, 0)
                calendar.set(Calendar.MILLISECOND, 0)
                rangeStart = calendar.timeInMillis

                calendar.set(Calendar.HOUR_OF_DAY, 23)
                calendar.set(Calendar.MINUTE, 59)
                calendar.set(Calendar.SECOND, 59)
                calendar.set(Calendar.MILLISECOND, 999)
                val dayEnd = calendar.timeInMillis
                rangeEnd = minOf(dayEnd, System.currentTimeMillis())

                // 12 2-hour buckets
                for (slot in 0..11) {
                    val startHour = slot * 2
                    val endHour = startHour + 2

                    val bucketCal = Calendar.getInstance()
                    bucketCal.timeInMillis = rangeStart
                    bucketCal.set(Calendar.HOUR_OF_DAY, startHour)
                    val bStart = bucketCal.timeInMillis

                    bucketCal.set(Calendar.HOUR_OF_DAY, if (endHour < 24) endHour else 23)
                    if (endHour >= 24) {
                        bucketCal.set(Calendar.MINUTE, 59)
                        bucketCal.set(Calendar.SECOND, 59)
                        bucketCal.set(Calendar.MILLISECOND, 999)
                    }
                    val bEnd = bucketCal.timeInMillis

                    val slotLabel = when (startHour) {
                        0 -> "12 am"
                        4 -> "4 am"
                        8 -> "8 am"
                        12 -> "12 pm"
                        16 -> "4 pm"
                        20 -> "8 pm"
                        else -> ""
                    }

                    var bRx = 0L
                    var bTx = 0L
                    if (bStart < rangeEnd) {
                        val queryEnd = minOf(bEnd, rangeEnd)
                        if (queryEnd > bStart) {
                            if (filter == NetworkInterfaceFilter.ALL || filter == NetworkInterfaceFilter.MOBILE) {
                                runCatching {
                                    val b = networkStatsManager.querySummaryForDevice(
                                        ConnectivityManager.TYPE_MOBILE,
                                        null,
                                        bStart,
                                        queryEnd
                                    )
                                    bRx += b.rxBytes
                                    bTx += b.txBytes
                                }
                            }
                            if (filter == NetworkInterfaceFilter.ALL || filter == NetworkInterfaceFilter.WIFI) {
                                runCatching {
                                    val b = networkStatsManager.querySummaryForDevice(
                                        ConnectivityManager.TYPE_WIFI,
                                        null,
                                        bStart,
                                        queryEnd
                                    )
                                    bRx += b.rxBytes
                                    bTx += b.txBytes
                                }
                            }
                        }
                    }

                    timelineBuckets.add(
                        NetworkBucket(
                            startTimeMillis = bStart,
                            endTimeMillis = bEnd,
                            rxBytes = bRx,
                            txBytes = bTx,
                            totalBytes = bRx + bTx,
                            label = slotLabel
                        )
                    )
                }
            }

            NetworkInterval.WEEK -> {
                calendar.set(Calendar.HOUR_OF_DAY, 23)
                calendar.set(Calendar.MINUTE, 59)
                calendar.set(Calendar.SECOND, 59)
                calendar.set(Calendar.MILLISECOND, 999)
                val weekEnd = calendar.timeInMillis
                rangeEnd = minOf(weekEnd, System.currentTimeMillis())

                calendar.add(Calendar.DAY_OF_YEAR, -6)
                calendar.set(Calendar.HOUR_OF_DAY, 0)
                calendar.set(Calendar.MINUTE, 0)
                calendar.set(Calendar.SECOND, 0)
                calendar.set(Calendar.MILLISECOND, 0)
                rangeStart = calendar.timeInMillis

                val dayFormat = SimpleDateFormat("EEE", Locale.getDefault())
                val dayCal = Calendar.getInstance()
                dayCal.timeInMillis = rangeStart

                for (d in 0..6) {
                    val bStart = dayCal.timeInMillis
                    val label = dayFormat.format(dayCal.time)
                    dayCal.add(Calendar.DAY_OF_YEAR, 1)
                    val bEnd = dayCal.timeInMillis

                    var bRx = 0L
                    var bTx = 0L
                    if (bStart < rangeEnd) {
                        val queryEnd = minOf(bEnd, rangeEnd)
                        if (queryEnd > bStart) {
                            if (filter == NetworkInterfaceFilter.ALL || filter == NetworkInterfaceFilter.MOBILE) {
                                runCatching {
                                    val b = networkStatsManager.querySummaryForDevice(
                                        ConnectivityManager.TYPE_MOBILE,
                                        null,
                                        bStart,
                                        queryEnd
                                    )
                                    bRx += b.rxBytes
                                    bTx += b.txBytes
                                }
                            }
                            if (filter == NetworkInterfaceFilter.ALL || filter == NetworkInterfaceFilter.WIFI) {
                                runCatching {
                                    val b = networkStatsManager.querySummaryForDevice(
                                        ConnectivityManager.TYPE_WIFI,
                                        null,
                                        bStart,
                                        queryEnd
                                    )
                                    bRx += b.rxBytes
                                    bTx += b.txBytes
                                }
                            }
                        }
                    }

                    timelineBuckets.add(
                        NetworkBucket(
                            startTimeMillis = bStart,
                            endTimeMillis = bEnd,
                            rxBytes = bRx,
                            txBytes = bTx,
                            totalBytes = bRx + bTx,
                            label = label
                        )
                    )
                }
            }

            NetworkInterval.MONTH -> {
                calendar.set(Calendar.DAY_OF_MONTH, 1)
                calendar.set(Calendar.HOUR_OF_DAY, 0)
                calendar.set(Calendar.MINUTE, 0)
                calendar.set(Calendar.SECOND, 0)
                calendar.set(Calendar.MILLISECOND, 0)
                rangeStart = calendar.timeInMillis

                val maxDays = calendar.getActualMaximum(Calendar.DAY_OF_MONTH)
                calendar.set(Calendar.DAY_OF_MONTH, maxDays)
                calendar.set(Calendar.HOUR_OF_DAY, 23)
                calendar.set(Calendar.MINUTE, 59)
                calendar.set(Calendar.SECOND, 59)
                calendar.set(Calendar.MILLISECOND, 999)
                val monthEnd = calendar.timeInMillis
                rangeEnd = minOf(monthEnd, System.currentTimeMillis())

                val dayCal = Calendar.getInstance()
                dayCal.timeInMillis = rangeStart

                for (d in 1..maxDays) {
                    val bStart = dayCal.timeInMillis
                    val label = if (d == 1 || d == 5 || d == 10 || d == 15 || d == 20 || d == 25 || d == maxDays) "$d" else ""
                    dayCal.add(Calendar.DAY_OF_MONTH, 1)
                    val bEnd = dayCal.timeInMillis

                    var bRx = 0L
                    var bTx = 0L
                    if (bStart < rangeEnd) {
                        val queryEnd = minOf(bEnd, rangeEnd)
                        if (queryEnd > bStart) {
                            if (filter == NetworkInterfaceFilter.ALL || filter == NetworkInterfaceFilter.MOBILE) {
                                runCatching {
                                    val b = networkStatsManager.querySummaryForDevice(
                                        ConnectivityManager.TYPE_MOBILE,
                                        null,
                                        bStart,
                                        queryEnd
                                    )
                                    bRx += b.rxBytes
                                    bTx += b.txBytes
                                }
                            }
                            if (filter == NetworkInterfaceFilter.ALL || filter == NetworkInterfaceFilter.WIFI) {
                                runCatching {
                                    val b = networkStatsManager.querySummaryForDevice(
                                        ConnectivityManager.TYPE_WIFI,
                                        null,
                                        bStart,
                                        queryEnd
                                    )
                                    bRx += b.rxBytes
                                    bTx += b.txBytes
                                }
                            }
                        }
                    }

                    timelineBuckets.add(
                        NetworkBucket(
                            startTimeMillis = bStart,
                            endTimeMillis = bEnd,
                            rxBytes = bRx,
                            txBytes = bTx,
                            totalBytes = bRx + bTx,
                            label = label
                        )
                    )
                }
            }
        }

        // Summary device totals
        var mobileRx = 0L
        var mobileTx = 0L
        var wifiRx = 0L
        var wifiTx = 0L

        if (rangeEnd > rangeStart) {
            runCatching {
                val b = networkStatsManager.querySummaryForDevice(
                    ConnectivityManager.TYPE_MOBILE,
                    null,
                    rangeStart,
                    rangeEnd
                )
                mobileRx = b.rxBytes
                mobileTx = b.txBytes
            }
            runCatching {
                val b = networkStatsManager.querySummaryForDevice(
                    ConnectivityManager.TYPE_WIFI,
                    null,
                    rangeStart,
                    rangeEnd
                )
                wifiRx = b.rxBytes
                wifiTx = b.txBytes
            }
        }

        val totalMobile = mobileRx + mobileTx
        val totalWifi = wifiRx + wifiTx

        val (downloadBytes, uploadBytes) = when (filter) {
            NetworkInterfaceFilter.ALL -> Pair(mobileRx + wifiRx, mobileTx + wifiTx)
            NetworkInterfaceFilter.MOBILE -> Pair(mobileRx, mobileTx)
            NetworkInterfaceFilter.WIFI -> Pair(wifiRx, wifiTx)
        }

        // Per-UID attribution separated by interface
        val mobileUidStats = mutableMapOf<Int, Pair<Long, Long>>()
        val wifiUidStats = mutableMapOf<Int, Pair<Long, Long>>()

        fun queryUidStats(networkType: Int, targetMap: MutableMap<Int, Pair<Long, Long>>) {
            var queried = false
            runCatching {
                val stats = networkStatsManager.querySummary(networkType, null, rangeStart, rangeEnd)
                val bucket = NetworkStats.Bucket()
                while (stats.hasNextBucket()) {
                    stats.getNextBucket(bucket)
                    val uid = bucket.uid
                    val prev = targetMap[uid] ?: Pair(0L, 0L)
                    targetMap[uid] = Pair(prev.first + bucket.rxBytes, prev.second + bucket.txBytes)
                }
                stats.close()
                queried = true
            }

            // Fallback for mobile on devices where subscriberId is needed
            if (!queried && networkType == ConnectivityManager.TYPE_MOBILE) {
                runCatching {
                    val telephonyManager = context.getSystemService(Context.TELEPHONY_SERVICE) as? android.telephony.TelephonyManager
                    val subId = telephonyManager?.subscriberId
                    if (!subId.isNullOrEmpty()) {
                        val stats = networkStatsManager.querySummary(networkType, subId, rangeStart, rangeEnd)
                        val bucket = NetworkStats.Bucket()
                        while (stats.hasNextBucket()) {
                            stats.getNextBucket(bucket)
                            val uid = bucket.uid
                            val prev = targetMap[uid] ?: Pair(0L, 0L)
                            targetMap[uid] = Pair(prev.first + bucket.rxBytes, prev.second + bucket.txBytes)
                        }
                        stats.close()
                    }
                }
            }
        }

        if (rangeEnd > rangeStart) {
            queryUidStats(ConnectivityManager.TYPE_MOBILE, mobileUidStats)
            queryUidStats(ConnectivityManager.TYPE_WIFI, wifiUidStats)
        }

        val pm = context.packageManager
        val installedApps = runCatching {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                pm.getInstalledApplications(PackageManager.ApplicationInfoFlags.of(0))
            } else {
                pm.getInstalledApplications(0)
            }
        }.getOrNull() ?: emptyList()

        val uidToAppInfo = mutableMapOf<Int, android.content.pm.ApplicationInfo>()
        for (info in installedApps) {
            uidToAppInfo[info.uid] = info
        }

        val allUids = (mobileUidStats.keys + wifiUidStats.keys)
        val appList = mutableListOf<AppNetworkUsage>()

        for (uid in allUids) {
            val (mRx, mTx) = mobileUidStats[uid] ?: Pair(0L, 0L)
            val (wRx, wTx) = wifiUidStats[uid] ?: Pair(0L, 0L)
            val mTotal = mRx + mTx
            val wTotal = wRx + wTx

            val (rx, tx) = when (filter) {
                NetworkInterfaceFilter.ALL -> Pair(mRx + wRx, mTx + wTx)
                NetworkInterfaceFilter.MOBILE -> Pair(mRx, mTx)
                NetworkInterfaceFilter.WIFI -> Pair(wRx, wTx)
            }
            val total = rx + tx
            if (total <= 0) continue

            val meta = getAppMeta(pm, uid, uidToAppInfo)
            appList.add(
                AppNetworkUsage(
                    uid = uid,
                    packageName = meta.packageName,
                    appName = meta.appName,
                    rxBytes = rx,
                    txBytes = tx,
                    totalBytes = total,
                    mobileBytes = mTotal,
                    wifiBytes = wTotal,
                    icon = meta.icon
                )
            )
        }

        appList.sortByDescending { it.totalBytes }

        NetworkUsageSummary(
            downloadBytes = downloadBytes,
            uploadBytes = uploadBytes,
            mobileBytes = totalMobile,
            wifiBytes = totalWifi,
            totalBytes = downloadBytes + uploadBytes,
            timeline = timelineBuckets,
            apps = appList
        )
    }

    private fun getAppMeta(
        pm: PackageManager,
        uid: Int,
        uidToAppInfo: Map<Int, android.content.pm.ApplicationInfo>
    ): CachedAppMeta {
        appMetaCache[uid]?.let { return it }

        // 1. Check known system services
        if (uid == Process.SYSTEM_UID || uid == 1000) {
            val icon = runCatching { pm.getApplicationIcon("android") }.getOrNull()
            val meta = CachedAppMeta("Android System", "android", icon)
            appMetaCache[uid] = meta
            return meta
        }
        if (uid == 0) {
            val meta = CachedAppMeta("Root Process", "root", null)
            appMetaCache[uid] = meta
            return meta
        }
        if (uid == 1073) {
            val meta = CachedAppMeta("Bluetooth Service", "com.android.bluetooth", null)
            appMetaCache[uid] = meta
            return meta
        }
        if (uid == 1001) {
            val meta = CachedAppMeta("Telephony Services", "com.android.phone", null)
            appMetaCache[uid] = meta
            return meta
        }
        if (uid == -4 || uid == -5) {
            val label = if (uid == -4) "Tethering & Hotspot" else "Removed Applications"
            val meta = CachedAppMeta(label, "system.network", null)
            appMetaCache[uid] = meta
            return meta
        }

        // 2. Direct lookup from pre-indexed installed applications
        val indexedApp = uidToAppInfo[uid]
        if (indexedApp != null) {
            val label = runCatching { pm.getApplicationLabel(indexedApp).toString() }.getOrNull()
                ?: indexedApp.packageName
            val icon = runCatching { pm.getApplicationIcon(indexedApp) }.getOrNull()
            val meta = CachedAppMeta(appName = label, packageName = indexedApp.packageName, icon = icon)
            appMetaCache[uid] = meta
            return meta
        }

        // 3. Query packages for UID
        val packages = runCatching { pm.getPackagesForUid(uid) }.getOrNull()
        val pkgName = packages?.firstOrNull()
        if (pkgName != null && !pkgName.startsWith("uid.")) {
            val appInfo = runCatching { pm.getApplicationInfo(pkgName, 0) }.getOrNull()
            val label = appInfo?.let { runCatching { pm.getApplicationLabel(it).toString() }.getOrNull() }
                ?: pkgName
            val icon = appInfo?.let { runCatching { pm.getApplicationIcon(it) }.getOrNull() }
            val meta = CachedAppMeta(appName = label, packageName = pkgName, icon = icon)
            appMetaCache[uid] = meta
            return meta
        }

        // 4. Query name for UID (e.g. shared UID)
        val rawName = runCatching { pm.getNameForUid(uid) }.getOrNull()
        if (rawName != null) {
            val cleanName = if (rawName.contains(":")) rawName.substringBefore(":") else rawName
            val appInfo = runCatching { pm.getApplicationInfo(cleanName, 0) }.getOrNull()
            val label = appInfo?.let { runCatching { pm.getApplicationLabel(it).toString() }.getOrNull() }
                ?: cleanName
            val icon = appInfo?.let { runCatching { pm.getApplicationIcon(it) }.getOrNull() }
            val meta = CachedAppMeta(appName = label, packageName = cleanName, icon = icon)
            appMetaCache[uid] = meta
            return meta
        }

        // 5. Clean fallback if UID cannot be resolved
        val fallbackMeta = CachedAppMeta(
            appName = "Application ($uid)",
            packageName = "uid.$uid",
            icon = null
        )
        appMetaCache[uid] = fallbackMeta
        return fallbackMeta
    }

    fun emptySummary(): NetworkUsageSummary = NetworkUsageSummary(
        downloadBytes = 0L,
        uploadBytes = 0L,
        mobileBytes = 0L,
        wifiBytes = 0L,
        totalBytes = 0L,
        timeline = emptyList(),
        apps = emptyList()
    )

    fun formatBytes(bytes: Long): String {
        if (bytes < 1024L) return "$bytes B"
        val kb = bytes / 1024.0
        if (kb < 1024.0) return String.format(Locale.US, "%.2f KB", kb)
        val mb = kb / 1024.0
        if (mb < 1024.0) return String.format(Locale.US, "%.2f MB", mb)
        val gb = mb / 1024.0
        return String.format(Locale.US, "%.2f GB", gb)
    }

    fun splitBytesAndUnit(bytes: Long): Pair<String, String> {
        if (bytes < 1024L) return Pair("$bytes", "B")
        val kb = bytes / 1024.0
        if (kb < 1024.0) return Pair(String.format(Locale.US, "%.2f", kb), "KB")
        val mb = kb / 1024.0
        if (mb < 1024.0) return Pair(String.format(Locale.US, "%.2f", mb), "MB")
        val gb = mb / 1024.0
        return Pair(String.format(Locale.US, "%.2f", gb), "GB")
    }

    fun formatSpeed(bytesPerSec: Long): String {
        return "${formatBytes(bytesPerSec)}/s"
    }
}
