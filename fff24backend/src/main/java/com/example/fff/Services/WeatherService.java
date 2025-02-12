package com.example.fff.Services;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import org.springframework.stereotype.Service;

@Service
public class WeatherService {

    private final HttpClient httpClient = HttpClient.newHttpClient();

    /**
     * Fetches weather data from the specified URL by sending an HTTP GET request.
     *
     * @param url The URL of the weather API endpoint to fetch data from.
     * @return A string containing the response body of the weather API, if the HTTP status is 200.
     * @throws Exception If an error occurs during the HTTP request or response handling.
     */
    public String fetchWeatherData(String url) throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .GET()
                .build();
        HttpResponse<String> response = httpClient.send(
                request,
                HttpResponse.BodyHandlers.ofString()
        );
        if (response.statusCode() == 200) {
            return response.body();
        } else {
            throw new RuntimeException("Fehler beim Abruf der Wetter-API. Status=" + response.statusCode());
        }
    }

    /**
     * Fetches electricity pricing data from the specified URL by sending an HTTP GET request.
     *
     * @param url The URL of the electricity pricing API endpoint to fetch data from.
     * @return A string containing the response body of the electricity pricing API, if the HTTP status is 200.
     * @throws Exception If an error occurs during the HTTP request or response handling.
     */
    public String fetchElectricityPriceData(String url) throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .GET()
                .build();
        HttpResponse<String> response = httpClient.send(
                request,
                HttpResponse.BodyHandlers.ofString()
        );
        if (response.statusCode() == 200) {
            return response.body();
        } else {
            throw new RuntimeException("Fehler beim Abruf der Strompreis-API. Status=" + response.statusCode());
        }
    }
}
