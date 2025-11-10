package com.muruz.weather;

import java.util.List;

public class WeatherData {
    public List<Weather> weather;
    public Main main;
    public int visibility;
    public Wind wind;
    public long dt;
    public Sys sys;
    public int timezone;
    public String name;

    public static class Weather {
        public String main;
        public String description;
    }

    public static class Main {
        public double temp;
        public double feels_like;
    }

    public static class Wind {
        public double speed;
    }

    public static class Sys {
        public long sunrise;
        public long sunset;
    }
}