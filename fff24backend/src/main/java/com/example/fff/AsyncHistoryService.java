package com.example.fff;

import com.example.fff.api.ProductManager;
import com.example.fff.api.WarenausgangManager;
import com.example.fff.api.WareneingangManager;
import com.example.fff.databse.PostgresDBProductManagement;
import com.example.fff.model.*;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;
import java.util.logging.Level;
import java.util.logging.Logger;

@Service
public class AsyncHistoryService {

    private static final Logger LOGGER = Logger.getLogger(AsyncHistoryService.class.getName());

    private final ProductManager productManager;
    private final WarenausgangManager warenausgangManager;
    private final WareneingangManager wareneingangManager;

    // Store jobId -> JobStatus
    private final Map<String, JobStatus> jobStatusMap = new ConcurrentHashMap<>();

    public AsyncHistoryService(
            ProductManager productManager,
            WarenausgangManager warenausgangManager,
            WareneingangManager wareneingangManager
    ) {
        this.productManager = productManager;
        this.warenausgangManager = warenausgangManager;
        this.wareneingangManager = wareneingangManager;
    }

    public void createJobStatus(String jobId) {
        jobStatusMap.put(jobId, new JobStatus(jobId, JobStatus.Status.RUNNING, "Job started"));
    }

    public JobStatus getJobStatus(String jobId) {
        return jobStatusMap.get(jobId);
    }

    @Async
    public void generateHistoryAsync(String jobId, int startYear, int endYear) {
        try {
            // Mark the job as running (in case we update message)
            JobStatus status = jobStatusMap.get(jobId);
            if (status == null) {
                // If no job status stored, create one (safety check)
                status = new JobStatus(jobId, JobStatus.Status.RUNNING, "Job started");
                jobStatusMap.put(jobId, status);
            }

            ensureProductsExist();

            // For each year in the range, generate for all 12 months
            for (int year = startYear; year <= endYear; year++) {
                for (int month = 1; month <= 12; month++) {
                    // It's good to update job status for better tracking
                    status.setMessage(String.format("Generating data for %d-%02d", year, month));
                    generateWarenausgaengeForMonth(year, month, 30); // 5 warenausgänge per month, e.g.

                    // If you want to check for cancellation, you could do so here
                    // e.g., if status is no longer RUNNING, break early, etc.
                }
            }

            // Mark job done
            status.setStatus(JobStatus.Status.DONE);
            status.setMessage("All months complete.");

        } catch (Exception e) {
            e.printStackTrace();

            // Mark job error
            JobStatus status = jobStatusMap.get(jobId);
            if (status != null) {
                status.setStatus(JobStatus.Status.ERROR);
                status.setMessage("Error: " + e.getMessage());
            }
        }
    }

    private void generateWarenausgaengeForMonth(int year, int month, int ausgaengeProMonat) throws Exception {
        LOGGER.log(Level.INFO, "Start generating Warenausgaenge for {0}-{1}, count={2}",
                new Object[] { year, month, ausgaengeProMonat });

        LocalDate startOfMonth = LocalDate.of(year, month, 1);
        int lengthOfMonth = startOfMonth.lengthOfMonth();

        // Seasonal probabilities...
        double p1, p2, p3;
        if (month == 12 || month == 1 || month == 2) {
            p1 = 0.4; p2 = 0.2; p3 = 0.4;
        } else if (month >= 3 && month <= 5) {
            p1 = 0.3; p2 = 0.5; p3 = 0.2;
        } else if (month >= 6 && month <= 8) {
            p1 = 0.1; p2 = 0.7; p3 = 0.2;
        } else {
            p1 = 0.3; p2 = 0.3; p3 = 0.4;
        }

        for (int i = 0; i < ausgaengeProMonat; i++) {
            LOGGER.log(Level.INFO, "Month {0}-{1}: Creating Warenausgang #{2}/{3}",
                    new Object[] { year, month, (i+1), ausgaengeProMonat });

            int randomDay = ThreadLocalRandom.current().nextInt(1, lengthOfMonth + 1);
            LocalDate randomDate = LocalDate.of(year, month, randomDay);
            int randomHour = ThreadLocalRandom.current().nextInt(8, 18);
            int randomMinute = ThreadLocalRandom.current().nextInt(0, 60);

            LocalDateTime warenausgangDateTime = LocalDateTime.of(
                    randomDate.getYear(),
                    randomDate.getMonthValue(),
                    randomDate.getDayOfMonth(),
                    randomHour,
                    randomMinute
            );
            Timestamp warenausgangTimestamp = Timestamp.valueOf(warenausgangDateTime);

            // Make 2 items
            List<WarenausgangItem> items = new ArrayList<>();
            items.add(generateWarenausgangItemWithSeason(p1, p2, p3));
            items.add(generateWarenausgangItemWithSeason(p1, p2, p3));

            // 1) Create Warenausgang
            Warenausgang createdWarenausgang = warenausgangManager.createWarenausgang(items, warenausgangTimestamp);
            LOGGER.log(Level.INFO, "Warenausgang created, ID = {0}", createdWarenausgang.getWarenausgangId());

            // 2) Check stock -> create Wareneingang if needed
            for (WarenausgangItem item : items) {
                Product updatedProduct = getProductById(item.getProductId());
                if (updatedProduct != null
                        && updatedProduct.getProductQuantity() < updatedProduct.getReorderPoint()) {

                    LOGGER.log(Level.INFO, "Creating Wareneingang for productId {0} because quantity {1} < reorderPoint {2}",
                            new Object[]{ updatedProduct.getProductId(), updatedProduct.getProductQuantity(), updatedProduct.getReorderPoint() });

                    WareneingangItem wareneingangItem = new WareneingangItem(
                            updatedProduct.getProductId(),
                            updatedProduct.getReorderQuantity()
                    );
                    Wareneingang we = wareneingangManager.createWareneingang(
                            Collections.singletonList(wareneingangItem),
                            warenausgangTimestamp
                    );
                    LOGGER.log(Level.INFO, "Wareneingang created, ID = {0}", we.getWareneingangId());
                }
            }
        }

        LOGGER.log(Level.INFO, "Finished generating Warenausgaenge for {0}-{1}", new Object[] { year, month });
    }


    private WarenausgangItem generateWarenausgangItemWithSeason(double p1, double p2, double p3) {
        double rnd = Math.random();
        int productId;
        if (rnd <= p1) {
            productId = 1;
        } else if (rnd <= p1 + p2) {
            productId = 2;
        } else {
            productId = 3;
        }

        // e.g. random quantity 5..15
        int quantity = 5 + ThreadLocalRandom.current().nextInt(0, 95);
        return new WarenausgangItem(productId, quantity);
    }

    // Avoid reading all products for each lookup.
    // Make sure you implement readProductById in PostgresDBProductManagement for efficiency.
    private Product getProductById(int productId) {
        try {
            return ((PostgresDBProductManagement) productManager).readProductById(productId);
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error reading product by ID " + productId, e);
            return null;
        }
    }

    private void ensureProductsExist() throws Exception {
        List<Product> existing = productManager.readProducts(null, null);
        boolean has1 = existing.stream().anyMatch(p -> p.getProductId() == 1);
        boolean has2 = existing.stream().anyMatch(p -> p.getProductId() == 2);
        boolean has3 = existing.stream().anyMatch(p -> p.getProductId() == 3);

        if (!has1) {
            productManager.addProduct("Klaus Winter", "Mit Frostschutz", 1000);
        }
        if (!has2) {
            productManager.addProduct("Klaus Summer", "Mit Frostschutz", 1000);
        }
        if (!has3) {
            productManager.addProduct("Klaus Xtreme", "Mit Frostschutz", 1000);
        }
    }
}
