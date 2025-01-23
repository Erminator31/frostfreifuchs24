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

@Component
@EnableScheduling
public class DailyDemandScheduler {

    private static final Logger LOGGER = Logger.getLogger(DailyDemandScheduler.class.getName());
    private final PostgresDBProductManagement productManager;

    public DailyDemandScheduler() {
        this.productManager = PostgresDBProductManagement.getPostgresDBProductManagement();
    }

    /**
     * Tägliche Aufgabe zur Aktualisierung des dailyDemand um 1:00 Uhr morgens.
     */
    @Scheduled(cron = "0 0 23 * * ?") 
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

