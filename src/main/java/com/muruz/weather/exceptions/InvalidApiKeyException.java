package com.muruz.weather.exceptions;

public class InvalidApiKeyException extends WeatherSDKException {
    public InvalidApiKeyException(String message) {
        super(message);
    }
}
