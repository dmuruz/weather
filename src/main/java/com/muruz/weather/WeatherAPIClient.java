package com.muruz.weather;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import com.muruz.weather.exceptions.*;
import okhttp3.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Objects;

/**
 * Handles HTTP requests to the OpenWeatherMap API.
 */
public class WeatherAPIClient {
    private static final Logger logger = LoggerFactory.getLogger(WeatherAPIClient.class);
    private static final String BASE_URL = "https://api.openweathermap.org/data/2.5/weather";
    private final OkHttpClient httpClient;
    private final String apiKey;
    private final Gson gson;

    public WeatherAPIClient(String apiKey) {
        this.apiKey = Objects.requireNonNull(apiKey, "API Key cannot be null");
        this.httpClient = new OkHttpClient.Builder()
                .build();
        this.gson = new Gson();
    }

    /**
     * Fetches weather data for a specific city from the OpenWeatherMap API.
     *
     * @param cityName The name of the city.
     * @return The raw JSON string response from the API.
     * @throws WeatherSDKException If an error occurs during the API call.
     */
    public String fetchWeatherData(String cityName) throws WeatherSDKException {
        HttpUrl.Builder urlBuilder = HttpUrl.parse(BASE_URL).newBuilder()
                .addQueryParameter("q", cityName)
                .addQueryParameter("appid", apiKey)
                .addQueryParameter("units", "metric"); // Optional: get temperature in Celsius

        Request request = new Request.Builder()
                .url(urlBuilder.build())
                .build();

        logger.debug("Making API request for city: {}", cityName);

        try (Response response = httpClient.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                String errorBody = response.body() != null ? response.body().string() : "";
                logger.warn("API request failed for city: {}. Status: {}, Body: {}", cityName, response.code(), errorBody);

                switch (response.code()) {
                    case 401:
                        throw new InvalidApiKeyException("Unauthorized: Invalid API key provided.");
                    case 404:
                        throw new CityNotFoundException("City not found: " + cityName);
                    case 429:
                        throw new APILimitExceededException("API rate limit exceeded.");
                    default:
                        throw new WeatherSDKException("Unexpected API response: " + response.code() + " - " + errorBody);
                }
            }

            String responseBody = Objects.requireNonNull(response.body()).string();
            logger.debug("API request successful for city: {}", cityName);
            return responseBody;
        } catch (IOException e) {
            logger.error("Network error occurred while fetching data for city: {}", cityName, e);
            throw new WeatherSDKException("Network error while fetching weather data for city: " + cityName, e);
        }
    }

    /**
     * Parses the raw JSON response from the API into a WeatherData object.
     *
     * @param jsonResponse The JSON string from the API.
     * @return The parsed WeatherData object.
     * @throws WeatherSDKException If parsing fails.
     */
    public WeatherData parseWeatherData(String jsonResponse) throws WeatherSDKException {
        try {
            return gson.fromJson(jsonResponse, WeatherData.class);
        } catch (JsonSyntaxException e) {
            logger.error("Failed to parse JSON response: {}", jsonResponse, e);
            throw new WeatherSDKException("Failed to parse weather data from API response.", e);
        }
    }
}
