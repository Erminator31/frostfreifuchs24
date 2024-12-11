package com.example.fff.Scheduler;



import com.example.fff.databse.PostgresDBProductManagement;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.logging.Level;
import java.util.logging.Logger;

@Component
public class DailyDemandScheduler {

    private static final Logger LOGGER = Logger.getLogger(DailyDemandScheduler.class.getName());
    private final PostgresDBProductManagement productManager;

    public DailyDemandScheduler() {
        this.productManager = PostgresDBProductManagement.getPostgresDBProductManagement();
    }

    /**
     * Tägliche Aufgabe zur Aktualisierung des dailyDemand um 1:00 Uhr morgens.
     */
    @Scheduled(cron = "0 0 1 * * ?") // Täglich um 1:00 Uhr
    public void updateDailyDemandDaily() {
        try {
            LOGGER.log(Level.INFO, "Scheduled Task: Starting dailyDemand update.");
            productManager.updateDailyDemand();
            LOGGER.log(Level.INFO, "Scheduled Task: dailyDemand update completed successfully.");
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Scheduled Task: Error updating dailyDemand: " + e.getMessage(), e);
        }
    }
}

