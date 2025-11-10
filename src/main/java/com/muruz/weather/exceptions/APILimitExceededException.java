package com.muruz.weather.exceptions;

public class APILimitExceededException extends WeatherSDKException {
    public APILimitExceededException(String message) {
        super(message);
    }
}
