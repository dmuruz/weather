package com.muruz.weather;

import com.muruz.weather.exceptions.WeatherSDKException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Set;
import java.util.concurrent.*;

/**
 * Manages the polling mechanism for updating cached weather data in polling mode.
 */
public class PollingManager {
    private static final Logger logger = LoggerFactory.getLogger(PollingManager.class);
    private static final long POLL_INTERVAL_MS = 10 * 60 * 1000;

    private final WeatherAPIClient apiClient;
    private final WeatherCache cache;
    private final ScheduledExecutorService scheduler;
    private volatile boolean isRunning = false;

    public PollingManager(WeatherAPIClient apiClient, WeatherCache cache) {
        this.apiClient = apiClient;
        this.cache = cache;
        this.scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "WeatherSDK-Poller");
            t.setDaemon(true);
            return t;
        });
    }

    public void start() {
        if (!isRunning) {
            scheduler.scheduleAtFixedRate(this::pollAllCities, 0, POLL_INTERVAL_MS, TimeUnit.MILLISECONDS);
            isRunning = true;
            logger.info("Polling manager started.");
        }
    }

    public void stop() {
        if (isRunning) {
            scheduler.shutdown();
            try {
                if (!scheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                    scheduler.shutdownNow();
                }
            } catch (InterruptedException e) {
                scheduler.shutdownNow();
                Thread.currentThread().interrupt();
            }
            isRunning = false;
            logger.info("Polling manager stopped.");
        }
    }

    private void pollAllCities() {
        Set<String> cityNames = cache.getCityNames();
        if (cityNames.isEmpty()) {
            logger.debug("No cities in cache to poll.");
            return;
        }
        logger.debug("Starting polling cycle for {} cities.", cityNames.size());

        for (String cityName : cityNames) {
            try {
                logger.debug("Polling weather for city: {}", cityName);
                String rawJson = apiClient.fetchWeatherData(cityName);
                WeatherData data = apiClient.parseWeatherData(rawJson);
                cache.put(cityName, data);
                logger.debug("Successfully polled and updated cache for city: {}", cityName);
            } catch (WeatherSDKException e) {
                logger.warn("Failed to poll weather for city '{}': {}", cityName, e.getMessage());
            }
        }
        logger.debug("Polling cycle completed.");
    }
}
