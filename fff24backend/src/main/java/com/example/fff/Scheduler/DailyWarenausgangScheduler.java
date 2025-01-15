package com.example.fff.Scheduler;

import com.example.fff.api.ProductManager;
import com.example.fff.api.WarenausgangManager;
import com.example.fff.model.Product;
import com.example.fff.model.WarenausgangItem;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import java.util.logging.Level;
import java.util.logging.Logger;

@Service
@EnableScheduling
public class DailyWarenausgangScheduler {

    private final WarenausgangManager warenausgangManager;
    private final ProductManager productManager;
    private static final Logger LOGGER = Logger.getLogger(DailyWarenausgangScheduler.class.getName());

    public DailyWarenausgangScheduler(WarenausgangManager warenausgangManager, ProductManager productManager) {
        this.warenausgangManager = warenausgangManager;
        this.productManager = productManager;
    }

    // Dieser Scheduled-Task wird einmal täglich um Mitternacht gestartet
    @Scheduled(cron = "0 25 12 * * ?")
    public void simulateDailyWarenausgaenge() {
        try {
            LOGGER.log(Level.INFO, "Starte tägliche Simulation der Warenausgänge...");

            // Heutiges Datum
            LocalDate today = LocalDate.now();

            // Holen aller Produkte aus der Datenbank
            List<Product> products = productManager.readProducts(null, null);
            Random random = new Random();
            // Für jedes Produkt werden Zielmengen berechnet und zufällige Warenausgänge erstellt
            for (Product product : products) {
                int dailyDemand = product.getDailyDemand();
                // Berechne ±15% Variation
                double variationFactor = 1 + (ThreadLocalRandom.current().nextDouble(-0.15, 0.15));
                int targetTotalQuantity = (int) Math.round(dailyDemand * variationFactor);

                // Anzahl der Warenausgänge, die wir simulieren möchten (zum Beispiel 5)
                int numberOfAusgaenge = random.nextInt(3,11);
                // Verteile die Gesamtmenge zufällig auf die einzelnen Warenausgänge
                List<Integer> quantities = distributeQuantityRandomly(targetTotalQuantity, numberOfAusgaenge);

                // Generiere für jeden simulierten Warenausgang einen zufälligen Zeitpunkt innerhalb des Tages
                List<   WarenausgangItem> items = new ArrayList<>();
                for (int qty : quantities) {
                    items.clear();
                    items.add(new WarenausgangItem(product.getProductId(), qty));

                    // Erstelle einen zufälligen Zeitpunkt heute
                    LocalTime randomTime = LocalTime.of(
                            ThreadLocalRandom.current().nextInt(0, 24),
                            ThreadLocalRandom.current().nextInt(0, 60),
                            ThreadLocalRandom.current().nextInt(0, 60)
                    );
                    LocalDateTime dateTime = LocalDateTime.of(today, randomTime);
                    Timestamp warenausgangTimestamp = Timestamp.valueOf(dateTime);

                    // Erstelle den Warenausgang für dieses Produkt
                    try {
                        warenausgangManager.createWarenausgang(new ArrayList<>(items), warenausgangTimestamp);
                        LOGGER.log(Level.INFO, "Erstellt Warenausgang für Produkt ID {0} mit Menge {1} um {2}",
                                new Object[]{product.getProductId(), qty, warenausgangTimestamp});
                    } catch (Exception e) {
                        LOGGER.log(Level.SEVERE, "Fehler beim Erstellen des Warenausgangs: " + e.getMessage(), e);
                    }
                }
            }
            LOGGER.log(Level.INFO, "Tägliche Simulation der Warenausgänge abgeschlossen.");
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Fehler in der täglichen Simulation: " + e.getMessage(), e);
        }
    }

    /**
     * Hilfsmethode zur zufälligen Verteilung einer Gesamtmenge auf eine bestimmte Anzahl an Teilen.
     */
    private List<Integer> distributeQuantityRandomly(int total, int parts) {
        List<Integer> quantities = new ArrayList<>();
        int remaining = total;
        Random random = new Random();
        for (int i = 0; i < parts - 1; i++) {
            // Verteile zufällig einen Teil der verbleibenden Menge
            int qty = random.nextInt(remaining + 1);
            quantities.add(qty);
            remaining -= qty;
        }
        // Der letzte Teil erhält den Rest
        quantities.add(remaining);
        // Mische die Liste, um zufällige Reihenfolge der Mengen zu gewährleisten
        Collections.shuffle(quantities);
        return quantities;
    }
}
