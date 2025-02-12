package com.example.fff.Scheduler;



import com.example.fff.MappingController;
import com.example.fff.databse.PostgresDBProductManagement;
import com.example.fff.model.ForecastWeights;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.sql.SQLException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * The DailyDemandScheduler class is a Spring component responsible for
 * executing daily forecasting tasks using a scheduled cron job.
 *
 * This class is annotated with @EnableScheduling to enable the
 * scheduling of tasks at fixed intervals.
 *
 * The class uses dependencies like PostgresDBProductManagement to
 * manage forecast-specific functionalities and MappingController to
 * generate forecasts.
 */
@Component
@EnableScheduling
public class DailyDemandScheduler {

    private static final Logger LOGGER = Logger.getLogger(DailyDemandScheduler.class.getName());
    private final PostgresDBProductManagement productManager;

    public DailyDemandScheduler() {
        this.productManager = PostgresDBProductManagement.getPostgresDBProductManagement();
    }


    /**
     * Executes a scheduled daily forecasting task using predefined weights.
     * This method is triggered by a Spring Scheduler based on the configured cron expression. It retrieves
     * the forecasting weights (alpha, beta, gamma) from the product manager, passes these parameters to
     * the MappingController, and requests a 14-day forecast to be generated.
     *
     * If an error occurs during the forecast generation process, the exception is logged with the severity level SEVERE.
     *
     * Functional Details:
     * - The method retrieves the current forecasting weights (alpha, beta, gamma) from the product manager.
     * - It creates an instance of the MappingController to initiate the forecast process.
     * - The forecast process operates for a fixed 14-day period using the provided weights.
     * - All operations are logged to monitor processing and potential errors.
     *
     * @throws SQLException If an issue occurs when accessing the database for retrieving forecasting weights.
     */
    @Scheduled(cron = "0 0 * * * ?")
    public void runForecastDaily() throws SQLException {
        LOGGER.log(Level.INFO, "Scheduled Task: runForecastDaily() aufgerufen.");

        ForecastWeights currentWeights = productManager.getForecastWeights();
        double alpha = currentWeights.getAlpha();
        double beta  = currentWeights.getBeta();
        double gamma = currentWeights.getGamma();
        try {

            MappingController controller = new MappingController();
            controller.getForecast(14,alpha,beta,gamma);
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Fehler im Scheduled Forecast: " + e.getMessage(), e);
        }
    }

}

