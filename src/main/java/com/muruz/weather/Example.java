package com.muruz.weather;

import com.muruz.weather.WeatherSDK.Mode;
import com.muruz.weather.exceptions.*;

import java.util.Scanner;

/**
 * Example usage of the WeatherSDK.
 * All user-facing messages are in English.
 */
public class Example {

    public static void main(String[] args) {
        String apiKey = System.getenv("OPENWEATHER_API_KEY");
        if (args.length > 0 && args[0] != null && !args[0].trim().isEmpty()) {
            apiKey = args[0];
        }
        if (apiKey == null || apiKey.trim().isEmpty()) {
            System.err.println("Error: OpenWeatherMap API key not provided.");
            System.err.println("Run with:");
            System.err.println("  java -cp ... com.muruz.weather.Example YOUR_API_KEY");
            System.err.println("  or set the OPENWEATHER_API_KEY environment variable.");
            return;
        }
        if (apiKey.length() < 30) {
            System.err.println("Suspiciously short API key: '" + apiKey + "'");
        }

        System.out.println("API key received. Starting examples...\n");

        System.out.println("Example 1: ON_DEMAND mode (fetch on request)");
        WeatherSDK onDemandSdk = null;
        try {
            WeatherSDKConfig config = WeatherSDKConfig.builder()
                    .apiKey(apiKey)
                    .mode(Mode.ON_DEMAND)
                    .build();

            onDemandSdk = WeatherSDK.getInstance(config);

            String city = "Moscow";
            System.out.println("→ Requesting weather for city: " + city);
            WeatherData data1 = onDemandSdk.getWeather(city);
            printWeather(data1);

            System.out.println("\n→ Re-requesting weather for the same city (should come from cache):");
            WeatherData data2 = onDemandSdk.getWeather(city);
            printWeather(data2);

        } catch (InvalidApiKeyException e) {
            System.err.println("Invalid API key: " + e.getMessage());
        } catch (CityNotFoundException e) {
            System.err.println("City not found: " + e.getMessage());
        } catch (APILimitExceededException e) {
            System.err.println("API rate limit exceeded: " + e.getMessage());
        } catch (WeatherSDKException e) {
            System.err.println("SDK error: " + e.getMessage());
            e.printStackTrace();
        } catch (Exception e) {
            System.err.println("Unexpected error: " + e.getMessage());
            e.printStackTrace();
        } finally {
            if (onDemandSdk != null) {
                System.out.println("\n→ Shutting down ON_DEMAND SDK...");
                onDemandSdk.destroy();
                System.out.println("ON_DEMAND SDK stopped.");
            }
        }

        System.out.println("\n" + "=".repeat(60) + "\n");

        System.out.println("Example 2: POLLING mode (background updates)");
        WeatherSDK pollingSdk = null;
        try {
            WeatherSDKConfig config = WeatherSDKConfig.builder()
                    .apiKey(apiKey)
                    .mode(Mode.POLLING)
                    .build();

            pollingSdk = WeatherSDK.getInstance(config);
            String city = "Tokyo";
            System.out.println("→ Initial request for " + city);
            WeatherData d1 = pollingSdk.getWeather(city);
            printWeatherMini(city, d1);

            System.out.println("\nSDK updates cached data in the background every 10 minutes.");
            System.out.println("Wait ~10–15 seconds (or press Enter to continue immediately)...");
            waitForUserOrTimeout(15_000);

            System.out.println("\n→ Re-requesting weather for " + city);
            WeatherData d2 = pollingSdk.getWeather(city);
            printWeatherMini(city, d2);

            double temp1 = d1.main != null ? d1.main.temp : Double.NaN;
            double temp2 = d2.main != null ? d2.main.temp : Double.NaN;
            if (!Double.isNaN(temp1) && !Double.isNaN(temp2) && Math.abs(temp1 - temp2) > 0.1) {
                System.out.println("Data was updated in the background!");
            } else {
                System.out.println("Data served from cache (no change).");
            }

        } catch (WeatherSDKException e) {
            System.err.println("Error in POLLING mode: " + e.getMessage());
            e.printStackTrace();
        } finally {
            if (pollingSdk != null) {
                System.out.println("\n→ Shutting down POLLING SDK...");
                pollingSdk.destroy();
                System.out.println("POLLING SDK stopped.");
            }
        }

        System.out.println("\nExample completed.");
    }
    private static void printWeather(WeatherData w) {
        WeatherData.Weather weather = w.weather != null && !w.weather.isEmpty() ? w.weather.get(0) : null;
        WeatherData.Main main = w.main;

        System.out.printf(
                "City: %s\n" +
                        "Temperature: %.1f°C (feels like %.1f°C)\n" +
                        "Weather: %s (%s)\n" +
                        "Wind: %.1f m/s\n" +
                        "Visibility: %d m\n" +
                        "Sunrise: %s\n" +
                        "Sunset: %s\n" +
                        "Data timestamp: %s (UTC)\n",
                w.name,
                main != null ? main.temp : Double.NaN,
                main != null ? main.feels_like : Double.NaN,
                weather != null ? weather.main : "N/A",
                weather != null ? weather.description : "N/A",
                w.wind != null ? w.wind.speed : 0.0,
                w.visibility,
                java.time.Instant.ofEpochSecond(w.sys.sunrise).toString(),
                java.time.Instant.ofEpochSecond(w.sys.sunset).toString(),
                java.time.Instant.ofEpochSecond(w.dt).toString()
        );
    }

    private static void printWeatherMini(String city, WeatherData w) {
        WeatherData.Main main = w.main;
        double temp = main != null ? main.temp : Double.NaN;
        System.out.printf("  %s: %.1f°C\n", city, temp);
    }

    private static void waitForUserOrTimeout(long timeoutMs) {
        Scanner scanner = new Scanner(System.in);
        System.out.print("Press Enter to continue or wait " + (timeoutMs / 1000) + " seconds... ");

        Thread inputThread = new Thread(() -> {
            try {
                scanner.nextLine();
            } catch (Exception ignored) {}
        });
        inputThread.start();

        try {
            inputThread.join(timeoutMs);
        } catch (InterruptedException ignored) {
            Thread.currentThread().interrupt();
        }

        if (inputThread.isAlive()) {
            System.out.println("\n(timed out — continuing)");
            inputThread.interrupt();
        } else {
            System.out.println("(continuing on Enter press)");
        }
    }
}