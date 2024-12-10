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

    private final Map<String, Object> additionalProperties = new HashMap<>();


    public Product(int productId, String productName, String productType) {
        this.productId = productId;
        this.productName = productName;
        this.productType = productType;
    }

    @Override
    public int getProductId(){
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
    public void setProductName(String productName) {
        this.productName = productName;
    }
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
