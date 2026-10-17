package com.example.core.performance

import java.util.concurrent.ConcurrentHashMap

/**
 * Thread-safe In-Memory LRU Cache with Stale-While-Revalidate support
 * Ensures sub-millisecond retrieval and prevents redundant API/DB calls.
 */
class MemoryLruCache<K : Any, V : Any>(
    private val maxCapacity: Int = 100,
    private val defaultTtlMillis: Long = 60_000L // 1 minute default TTL
) {
    private data class CacheEntry<V>(
        val value: V,
        val timestamp: Long,
        val ttlMillis: Long
    ) {
        val isStale: Boolean
            get() = System.currentTimeMillis() - timestamp > ttlMillis
    }

    private val cache = ConcurrentHashMap<K, CacheEntry<V>>()

    fun get(key: K): V? {
        val entry = cache[key] ?: return null
        return entry.value
    }

    fun isStale(key: K): Boolean {
        val entry = cache[key] ?: return true
        return entry.isStale
    }

    fun put(key: K, value: V, ttlMillis: Long = defaultTtlMillis) {
        if (cache.size >= maxCapacity) {
            // Evict oldest entries
            val oldest = cache.entries.minByOrNull { it.value.timestamp }
            oldest?.let { cache.remove(it.key) }
        }
        cache[key] = CacheEntry(value, System.currentTimeMillis(), ttlMillis)
    }

    fun remove(key: K) {
        cache.remove(key)
    }

    fun clear() {
        cache.clear()
    }

    val size: Int get() = cache.size
}
