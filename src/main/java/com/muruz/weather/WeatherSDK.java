package com.muruz.weather;

import com.muruz.weather.exceptions.WeatherSDKException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Main SDK class for accessing weather data.
 * Implements singleton pattern per API key to prevent duplicate instances.
 */
public class WeatherSDK {
    private static final Logger logger = LoggerFactory.getLogger(WeatherSDK.class);
    private static final ConcurrentMap<String, WeatherSDK> instances = new ConcurrentHashMap<>();

    private final WeatherAPIClient apiClient;
    private final WeatherCache cache;
    private final PollingManager pollingManager;
    private final Mode mode;
    private final String apiKey;

    public enum Mode {
        ON_DEMAND, POLLING
    }

    private WeatherSDK(WeatherSDKConfig config) {
        this.apiKey = config.getApiKey();
        this.mode = config.getMode();
        this.apiClient = new WeatherAPIClient(this.apiKey);
        this.cache = new WeatherCache();

        if (this.mode == Mode.POLLING) {
            this.pollingManager = new PollingManager(this.apiClient, this.cache);
            this.pollingManager.start();
        } else {
            this.pollingManager = null;
        }
        logger.info("WeatherSDK initialized in {} mode with API key hash: {}", mode, apiKey.hashCode());
    }

    /**
     * Creates or retrieves an SDK instance for the given API key.
     * Ensures only one instance per API key exists.
     *
     * @param config The configuration containing the API key and mode.
     * @return The SDK instance for the specified API key.
     */
    public static WeatherSDK getInstance(WeatherSDKConfig config) {
        String key = config.getApiKey();
        return instances.computeIfAbsent(key, k -> new WeatherSDK(config));
    }

    /**
     * Gets weather data for a given city.
     * In ON_DEMAND mode, fetches from API if not cached or expired.
     * In POLLING mode, returns cached data (assumed fresh due to polling).
     *
     * @param cityName The name of the city.
     * @return The WeatherData object.
     * @throws WeatherSDKException If an error occurs during retrieval.
     */
    public WeatherData getWeather(String cityName) throws WeatherSDKException {
        if (cityName == null || cityName.trim().isEmpty()) {
            throw new IllegalArgumentException("City name cannot be null or empty.");
        }

        logger.debug("Requesting weather for city: {}", cityName);

        WeatherData cachedData = cache.getIfValid(cityName);
        if (cachedData != null) {
            logger.debug("Returning cached weather data for city: {}", cityName);
            return cachedData;
        }

        if (mode == Mode.ON_DEMAND) {
            logger.debug("Cache miss for city: {}. Fetching from API.", cityName);
            String rawJson = apiClient.fetchWeatherData(cityName);
            WeatherData data = apiClient.parseWeatherData(rawJson);
            cache.put(cityName, data);
            return data;
        } else {
            logger.debug("Cache miss for city: {} in POLLING mode. Fetching from API as fallback.", cityName);
            String rawJson = apiClient.fetchWeatherData(cityName);
            WeatherData data = apiClient.parseWeatherData(rawJson);
            cache.put(cityName, data);
            return data;
        }
    }

    /**
     * Removes the SDK instance associated with the API key used to create this instance.
     * Stops the polling manager if running.
     */
    public void destroy() {
        String key = this.apiKey;
        WeatherSDK removed = instances.remove(key);
        if (removed != null) {
            if (pollingManager != null) {
                pollingManager.stop();
            }
            logger.info("WeatherSDK instance for API key hash {} has been destroyed.", key.hashCode());
        } else {
            logger.warn("Attempted to destroy SDK instance that was not found in the registry.");
        }
    }

    /**
     * Gets the current operating mode of the SDK instance.
     *
     * @return The mode (ON_DEMAND or POLLING).
     */
    public Mode getMode() {
        return mode;
    }
}
