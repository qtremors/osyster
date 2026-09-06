package dev.qtremors.osyster.monitor

/** Each collection owns its baseline, including after a pause or interval change. */
internal class CpuUsageSampler {
    private val previous = mutableMapOf<Int?, CpuTimeSnapshot>()

    fun sample(snapshot: CpuTimeSnapshot): Float {
        val before = previous.put(snapshot.coreId, snapshot) ?: return 0f
        val active = snapshot.activeTime - before.activeTime
        val idle = snapshot.idleTime - before.idleTime
        return SystemMonitor.calculateCpuUsage(active, active + idle)
    }
}
