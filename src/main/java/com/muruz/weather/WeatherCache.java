package com.muruz.weather;

import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * Manages caching of weather data based on city name.
 * Stores data for up to 10 cities.
 * Considers data valid for 10 minutes (600,000 ms).
 */
public class WeatherCache {
    private static final int MAX_CACHED_CITIES = 10;
    private static final long VALIDITY_PERIOD_MS = 10 * 60 * 1000; // 10 minutes

    private final Map<String, CachedWeatherData> cache = new LinkedHashMap<String, CachedWeatherData>(16, 0.75f, true) {
        @Override
        protected boolean removeEldestEntry(Map.Entry<String, CachedWeatherData> eldest) {
            return size() > MAX_CACHED_CITIES;
        }
    };

    private final ReadWriteLock lock = new ReentrantReadWriteLock();

    private static class CachedWeatherData {
        final WeatherData data;
        final long timestamp;

        CachedWeatherData(WeatherData data) {
            this.data = data;
            this.timestamp = System.currentTimeMillis();
        }
    }

    /**
     * Retrieves cached data if it exists and is still valid.
     * @param cityName The name of the city.
     * @return The cached WeatherData, or null if not found or expired.
     */
    public WeatherData getIfValid(String cityName) {
        lock.readLock().lock();
        try {
            CachedWeatherData cached = cache.get(cityName.toLowerCase());
            if (cached != null && (System.currentTimeMillis() - cached.timestamp) < VALIDITY_PERIOD_MS) {
                return cached.data;
            }
            if (cached != null) {
                lock.readLock().unlock();
                lock.writeLock().lock();
                try {
                    cached = cache.get(cityName.toLowerCase());
                    if (cached != null && (System.currentTimeMillis() - cached.timestamp) >= VALIDITY_PERIOD_MS) {
                        cache.remove(cityName.toLowerCase());
                    }
                } finally {
                    lock.readLock().lock();
                    lock.writeLock().unlock();
                }
            }
            return null;
        } finally {
            lock.readLock().unlock();
        }
    }

    /**
     * Stores weather data in the cache.
     * @param cityName The name of the city.
     * @param data The weather data to store.
     */
    public void put(String cityName, WeatherData data) {
        lock.writeLock().lock();
        try {
            cache.put(cityName.toLowerCase(), new CachedWeatherData(data));
        } finally {
            lock.writeLock().unlock();
        }
    }

    /**
     * Removes a city's data from the cache.
     * @param cityName The name of the city to remove.
     */
    public void remove(String cityName) {
        lock.writeLock().lock();
        try {
            cache.remove(cityName.toLowerCase());
        } finally {
            lock.writeLock().unlock();
        }
    }

    public Set<String> getCityNames() {
        lock.readLock().lock();
        try {
            return new HashSet<>(cache.keySet());
        } finally {
            lock.readLock().unlock();
        }
    }
}
