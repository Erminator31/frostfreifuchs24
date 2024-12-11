package model;

import com.example.fff.api.ProductInterface;
import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;

import java.util.HashMap;
import java.util.Map;

public class Product implements ProductInterface {

    private int productId;
    private String productName;

    private String productType;

    private int quantity;

    private final Map<String, Object> additionalProperties = new HashMap<>();


    public Product(int productId, String productName, String productType, int quantity) {
        this.productId = productId;
        this.productName = productName;
        this.productType = productType;
        this.quantity = quantity;
    }

    /**
     * This method retrieves the product ID associated with this product.
     *
     * @return The product ID of this product.
     */
    @Override
    public int getProductId(){
        return productId;
    }

    /**
     * Retrieves the name of the product.
     *
     * @return The name of the product.
     */
    @Override
    public String getProductName() {
        return productName;
    }

    /**
     * Retrieves the product type associated with this product.
     *
     * @return The product type of this product.
     */
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

    /**
     * Sets the name of the product.
     *
     * @param productName The new name to be assigned to the product.
     */
    @Override
    public void setProductName(String productName) {
        this.productName = productName;
    }

    /**
     * Sets the product type for this product.
     *
     * @param productType The new product type to be assigned to this product.
     */
    @Override
    public void setProductType(String productType) {
        this.productType = productType;
    }


    /**
     * Provides a mechanism for serializing additional properties during JSON serialization.
     */
    @JsonAnyGetter
    public Map<String, Object> getAdditionalProperties() {
        return this.additionalProperties;
    }

    /**
     * Allows for dynamically adding properties to the object during JSON deserialization.
     */
    @JsonAnySetter
    public void setAdditionalProperty(String name, Object value) {
        this.additionalProperties.put(name, value);
    }


}
