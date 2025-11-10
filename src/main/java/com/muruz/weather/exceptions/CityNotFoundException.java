package com.muruz.weather.exceptions;

public class CityNotFoundException extends WeatherSDKException {
    public CityNotFoundException(String message) {
        super(message);
    }
}
