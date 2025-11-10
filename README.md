Java Weather SDK

A lightweight, production-ready SDK for the OpenWeatherMap API, designed for simplicity, performance, and reliability.

Meets all task requirements: caching (10 cities, 10-minute TTL), dual modes (on-demand and polling), singleton-per-key, error handling, and standardized JSON output.

Features

- API Key Initialization: Passed via WeatherSDKConfig.
- City-Based Query: sdk.getWeather("London").
- Intelligent Caching: Stores up to 10 cities; data expires after 600 seconds.
- Two Operation Modes: ON_DEMAND (fetch on request) and POLLING (background refresh every 10 minutes).
- Error Handling: Typed exceptions (InvalidApiKeyException, CityNotFoundException, etc.).
- Singleton per API Key: WeatherSDK.getInstance(config) ensures only one instance per key.
- Destroy Support: .destroy() stops polling and releases resources.
- Standardized Output: Returns data exactly matching the required JSON structure.

Installation

Add the following dependencies to your Maven project (pom.xml):
```
<dependency>
  <groupId>com.squareup.okhttp3</groupId>
  <artifactId>okhttp</artifactId>
  <version>4.12.0</version>
</dependency>

<dependency>
  <groupId>com.google.code.gson</groupId>
  <artifactId>gson</artifactId>
  <version>2.10.1</version>
</dependency>

<dependency>
  <groupId>org.slf4j</groupId>
  <artifactId>slf4j-api</artifactId>
  <version>2.0.9</version>
</dependency>

<dependency>
  <groupId>org.slf4j</groupId>
  <artifactId>slf4j-simple</artifactId>
  <version>2.0.9</version>
  <scope>runtime</scope>
</dependency>
```

Note: slf4j-simple is optional but recommended. Without it, SLF4J defaults to no-operation logging (no console output).

Quick Start

1. Obtain an API key from https://home.openweathermap.org/api_keys.
2. Configure and use the SDK:
```
WeatherSDKConfig config = WeatherSDKConfig.builder()
    .apiKey("your_api_key")
    .mode(WeatherSDK.Mode.ON_DEMAND)
    .build();

WeatherSDK sdk = WeatherSDK.getInstance(config);

try {
    WeatherData weather = sdk.getWeather("Berlin");
    System.out.println(weather.name + ": " + weather.main.temp + "°C");
} catch (InvalidApiKeyException e) {
    System.err.println("Invalid API key: " + e.getMessage());
} catch (CityNotFoundException e) {
    System.err.println("City not found: " + e.getMessage());
} catch (WeatherSDKException e) {
    System.err.println("SDK error: " + e.getMessage());
} finally {
    sdk.destroy();
}
```

Polling Mode

Use Mode.POLLING to enable background updates:
```
WeatherSDKConfig config = WeatherSDKConfig.builder()
.apiKey("your_api_key")
.mode(WeatherSDK.Mode.POLLING)
.build();

WeatherSDK sdk = WeatherSDK.getInstance(config);
sdk.getWeather("Tokyo"); // First call may hit API
sdk.getWeather("Tokyo"); // Subsequent calls use cached (fresh) data

sdk.destroy(); // Stops background thread
```

Response Format

The SDK returns a WeatherData object with the following structure (mapped to JSON):
```
{
"weather": [
{
"main": "Clouds",
"description": "scattered clouds"
}
],
"main": {
"temp": 269.6,
"feels_like": 267.57
},
"visibility": 10000,
"wind": {
"speed": 1.38
},
"dt": 1675744800,
"sys": {
"sunrise": 1675751262,
"sunset": 1675787560
},
"timezone": 3600,
"name": "Zocca"
}
```


Note: OpenWeatherMap returns "weather" as an array (even with one item), and uses "dt" instead of "datetime". The field "temperature" in the task spec is represented as "main" in the API and SDK model.

Error Handling

The SDK throws the following exceptions:

- InvalidApiKeyException: API key is missing, invalid, or unauthorized (HTTP 401).
- CityNotFoundException: City name not found (HTTP 404).
- APILimitExceededException: Too many requests (HTTP 429).
- WeatherSDKException: Network issues, JSON parsing errors, or unexpected API responses.

Always handle these exceptions in client code.

Architecture Highlights

- Thread-Safe Cache: Uses ReentrantReadWriteLock for concurrent access.
- Singleton Registry: Ensures one SDK instance per unique API key.
- Decoupled Design: Separate classes for API client, cache, and polling logic.
- SLF4J Logging: Integration-ready; add any SLF4J provider for logs.
- No External Config: Pure Java API — no XML, properties, or environment dependencies beyond the API key.

Running the Example

Set your API key:

```export OPENWEATHER_API_KEY=your_key_here```


Run the included Example.java:

```mvn compile exec:java -Dexec.mainClass="com.muruz.weather.Example"```

License

MIT License — free for commercial and personal use.