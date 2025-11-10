package com.muruz.weather;

import java.util.Objects;

/**
 * Configuration object for the WeatherSDK.
 */
public class WeatherSDKConfig {
    private final String apiKey;
    private final WeatherSDK.Mode mode;

    private WeatherSDKConfig(Builder builder) {
        this.apiKey = Objects.requireNonNull(builder.apiKey, "API Key cannot be null");
        this.mode = Objects.requireNonNull(builder.mode, "Mode cannot be null");
    }

    public String getApiKey() {
        return apiKey;
    }

    public WeatherSDK.Mode getMode() {
        return mode;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String apiKey;
        private WeatherSDK.Mode mode;

        public Builder apiKey(String apiKey) {
            this.apiKey = apiKey;
            return this;
        }

        public Builder mode(WeatherSDK.Mode mode) {
            this.mode = mode;
            return this;
        }

        public WeatherSDKConfig build() {
            return new WeatherSDKConfig(this);
        }
    }
}
