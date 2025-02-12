package com.example.fff.Services;

import com.example.fff.api.ProductManager;
import org.json.JSONArray;
import org.json.JSONObject;

/**
 * The CalculationService class provides utility methods for determining factors related to weather,
 * seasons, and costs. It integrates external services, like weather and product management, to
 * calculate operational and cost-related data for products.
 */
public class CalculationService {

    WeatherService weatherService;
    ProductManager productManager;


    /**
     * Calculates a weather factor based on the product ID and temperature.
     * The weather factor is determined by specific conditions for a given range of temperatures:
     * - Below -5°C
     * - Between -5°C and 4°C
     * - Above 4°C
     * Each product ID maps to a specific factor within these temperature ranges.
     *
     * @param productId the unique identifier of the product (e.g. 1, 2, 3)
     * @param temperature the current temperature in degrees Celsius
     * @return the weather factor as a double value
     */
    public static double getWeatherFactor(int productId, double temperature) {
        if (temperature < -5) {
            return switch (productId) {
                case 1 -> 0.7;
                case 2 -> 0.2;
                case 3 -> 1.0;
                default -> 1.0;
            };
        } else if (temperature <= 4) {
            return switch (productId) {
                case 1 -> 1.0;
                case 2 -> 0.2;
                case 3 -> 0.5;
                default -> 1.0;
            };
        } else {
            return switch (productId) {
                case 1 -> 0.1;
                case 2 -> 1.0;
                case 3 -> 0.05;
                default -> 1.0;
            };
        }
    }

    /**
     * Calculates a seasonal adjustment factor based on the product ID and a given date in ISO format.
     * The adjustment factor is determined by specific seasonal patterns for the product.
     *
     * @param productId the unique identifier of the product (e.g., 1, 2, 3)
     * @param isoDate the date in ISO format (yyyy-MM-dd), used to determine the season
     * @return the seasonal adjustment factor as a double
     */
    public static double getSeasonFactor(int productId, String isoDate) {
        java.time.LocalDate date = java.time.LocalDate.parse(isoDate);
        int monthIndex = date.getMonthValue() - 1; // 0-basiert (0 = Januar)
        return switch (productId) {
            case 1 -> seasonFactorProduct1(monthIndex);
            case 2 -> seasonFactorProduct2(monthIndex);
            case 3 -> seasonFactorProduct3(monthIndex);
            default -> 0.0;
        };
    }

    /**
     * Calculates the seasonal adjustment factor for product ID 1 based on a given month index.
     * The method computes a cosine-based factor influenced by the input index and scales it with a fixed multiplier.
     *
     * @param x the month index (0-based, where 0 represents January and 11 represents December)
     * @return the computed seasonal adjustment factor as a positive double value
     */
    private static double seasonFactorProduct1(int x) {
        double val = -(Math.cos(Math.PI / 6.0 * x) + 1.2) * 0.45;
        return Math.abs(val);
    }

    /**
     * Calculates a seasonal adjustment factor for a specific product based on a cosine function and scaling parameters.
     * The result is a positive double value that represents the computed seasonal factor.
     *
     * @param x the input parameter representing a specific seasonal index for the product (e.g., month-based index)
     * @return the computed seasonal adjustment factor as a positive double value
     */
    private static double seasonFactorProduct2(int x) {
        double val = -(Math.cos(Math.PI / 6.0 * x) - 1.2) * 0.45;
        return Math.abs(val);
    }

    /**
     * Computes the seasonal adjustment factor for product ID 3 based on the given month index.
     * If the month index corresponds to January (0) or December (11), a custom seasonal
     * factor is calculated using a specific formula. For all other month indices,
     * a predefined seasonal factor for June is used.
     *
     * @param x the month index (0-based, where 0 represents January and 11 represents December)
     * @return the computed seasonal adjustment factor as a positive double value
     */
    private static double seasonFactorProduct3(int x) {
        if (x == 0 || x == 11) {
            return seasonFactorProduct1(x);
        } else {
            return seasonFactorProduct1(6);
        }
    }

    /**
     * Calculates the cost per item based on warehouse quantities, electricity pricing,
     * and temperature-dependent operational costs.
     *
     * This method fetches weather and electricity pricing data from external APIs, computes
     * operational costs based on current warehouse conditions and temperature, and then
     * divides the total costs by the total quantity of items in the warehouse to determine
     * the cost per item.
     *
     * If there are no items in the warehouse, the method will return the total operational
     * costs instead of dividing by zero.
     *
     * @return the calculated cost per item as a double, or the total operational costs if
     *         the total warehouse quantity is zero
     * @throws Exception if any API call fails or data retrieval encounters an issue
     */
    public double getCostPerItem() throws Exception {
        String weatherUrl = "https://api.open-meteo.com/v1/forecast"
                + "?latitude=49.3536"
                + "&longitude=9.1511"
                + "&daily=temperature_2m_max,temperature_2m_min"
                + "&forecast_days=1"
                + "&timezone=Europe/Berlin";
        String weatherJson = weatherService.fetchWeatherData(weatherUrl);
        JSONObject weatherObj = new JSONObject(weatherJson);
        JSONObject daily = weatherObj.getJSONObject("daily");
        double tmax = daily.getJSONArray("temperature_2m_max").optDouble(0, 0.0);
        double tmin = daily.getJSONArray("temperature_2m_min").optDouble(0, 0.0);
        double currentTemperature = (tmax + tmin) / 2.0;

        long nowUnix = System.currentTimeMillis() / 1000L;
        long oneDayLater = nowUnix + 24L * 3600L;
        String priceUrl = String.format(
                "https://api.energy-charts.info/price?bzn=DE-LU&start=%d&end=%d",
                nowUnix, oneDayLater
        );
        String priceJson = weatherService.fetchElectricityPriceData(priceUrl);
        JSONObject priceObj = new JSONObject(priceJson);
        JSONArray priceArray = priceObj.getJSONArray("price");
        double sumPrice = 0.0;
        for (int i = 0; i < priceArray.length(); i++) {
            sumPrice += priceArray.getDouble(i);
        }
        double avgPriceMWh = (priceArray.length() > 0) ? (sumPrice / priceArray.length()) : 0.0;
        double avgPriceKWh = avgPriceMWh / 1000.0;

        int totalQuantity = productManager.getTotalWarehouseQuantity();

        double pBasis = 1000.0;
        double kP = 7000.0;
        double grundkosten = 1000.0;
        double P;
        if (currentTemperature > 0.0) {
            P = pBasis + kP * (1.0 / currentTemperature);
        } else {
            P = pBasis + kP * Math.abs(currentTemperature);
        }
        double stromkosten = P * avgPriceKWh;
        double betriebskosten = stromkosten + grundkosten;

        if (totalQuantity > 0) {
            return betriebskosten / totalQuantity;
        } else {
            return betriebskosten;
        }
    }
}

