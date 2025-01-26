package com.example.fff.model;

import com.example.fff.api.ProductInterface;
import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.HashMap;
import java.util.Map;

/**
 * Modellklasse für Produkte.
 */
@JsonIgnoreProperties(ignoreUnknown = true) // Ignoriert unbekannte JSON-Felder
public class Product implements ProductInterface {
    private int productId;
    private String productName;
    private String productType;
    private int quantity;
    private int dailyDemand;
    private int reorderPoint;
    private int reorderQuantity;

    // Hinzugefügtes Feld für zusätzliche Eigenschaften
    private Map<String, Object> additionalProperties = new HashMap<>();

    /**
     * Konstruktor für die Product-Klasse.
     *
     * @param productId       Die eindeutige ID des Produkts.
     * @param productName     Der Name des Produkts.
     * @param productType     Der Typ des Produkts.
     * @param quantity        Die verfügbare Menge des Produkts.
     * @param dailyDemand     Der tägliche Bedarf des Produkts.
     * @param reorderPoint    Der Schwellenwert, bei dem nachbestellt werden soll.
     */
    public Product(int productId, String productName, String productType, int quantity, int dailyDemand, int reorderPoint, int reorderQuantity) {
        this.productId = productId;
        this.productName = productName;
        this.productType = productType;
        this.quantity = quantity;
        this.dailyDemand = dailyDemand;
        this.reorderPoint = reorderPoint;
        this.reorderQuantity = reorderQuantity;
    }

    // Getter und Setter für alle Felder

    @Override
    public int getDailyDemand() {
        return dailyDemand;
    }

    @Override
    public void setDailyDemand(int dailyDemand) {
        this.dailyDemand = dailyDemand;
    }

    @Override
    public int getReorderPoint() {
        return reorderPoint;
    }

    @Override
    public void setReorderPoint(int reorderPoint) {
        this.reorderPoint = reorderPoint;
    }

    @Override
    public void setReorderQuantity(int reorderQuantity) {
        this.reorderQuantity = reorderQuantity;
    }

    @Override
    public int getReorderQuantity() {
        return reorderQuantity;
    }

    // Entfernen Sie den Setter für reorderQuantity, um die Dynamik sicherzustellen
    // @Override
    // public void setReorderQuantity(int reorderQuantity) {
    //     this.reorderQuantity = reorderQuantity;
    // }

    @Override
    public int getProductId() {
        return productId;
    }

    @Override
    public String getProductName() {
        return productName;
    }

    @Override
    public String getProductType() {
        return productType;
    }

    @Override
    public int getProductQuantity() {
        return quantity;
    }

    @Override
    public void setProductQuantity(int quantity) {
        this.quantity = quantity;
    }

    @Override
    public void setProductName(String productName) {
        this.productName = productName;
    }

    @Override
    public void setProductType(String productType) {
        this.productType = productType;
    }


    /**
     * Gibt die zusätzlichen Eigenschaften zurück, die nicht explizit in der Klasse definiert sind.
     *
     * @return Eine Map mit zusätzlichen Eigenschaften.
     */
    @JsonAnyGetter
    public Map<String, Object> getAdditionalProperties() {
        return this.additionalProperties;
    }

    /**
     * Fügt eine zusätzliche Eigenschaft hinzu, die nicht explizit in der Klasse definiert ist.
     *
     * @param name  Der Name der zusätzlichen Eigenschaft.
     * @param value Der Wert der zusätzlichen Eigenschaft.
     */
    @JsonAnySetter
    public void setAdditionalProperty(String name, Object value) {
        this.additionalProperties.put(name, value);
    }


}
