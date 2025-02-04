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
    @Scheduled(cron = "0 0 12 * * ?")
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
                int targetTotalQuantity = (int) Math.round(dailyDemand * variationFactor+1);
if(targetTotalQuantity<=0){
    targetTotalQuantity=1;
}
                // Anzahl der Warenausgänge, die wir simulieren möchten (zum Beispiel 5)
                int numberOfAusgaenge = random.nextInt(3,5);
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
    /**
     * Verteilt eine Gesamtmenge zufällig auf eine bestimmte Anzahl von Teilen,
     * wobei sichergestellt wird, dass jeder Teil mindestens 1 beträgt.
     *
     * Falls total kleiner als parts ist, wird total auf parts gesetzt.
     */
    private List<Integer> distributeQuantityRandomly(int total, int parts) {
        // Damit jeder Teil mindestens 1 ist, muss total mindestens parts betragen.
        if (total < parts) {
            total = parts;
        }

        // Erzeuge (parts - 1) zufällige "Schnittstellen" im Intervall [1, total-1]
        List<Integer> cuts = new ArrayList<>();
        Random random = new Random();
        for (int i = 0; i < parts - 1; i++) {
            int cut = random.nextInt(total - 1) + 1; // Bereich: [1, total-1]
            cuts.add(cut);
        }
        // Füge die Grenzen 0 und total hinzu
        cuts.add(0);
        cuts.add(total);

        // Sortiere die Schnittstellen
        Collections.sort(cuts);

        // Berechne die Differenzen zwischen den benachbarten Zahlen
        List<Integer> result = new ArrayList<>();
        for (int i = 1; i < cuts.size(); i++) {
            result.add(cuts.get(i) - cuts.get(i - 1));
        }

        return result;
    }
}
