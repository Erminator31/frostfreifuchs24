package com.example.fff.api;

import com.example.fff.model.WarenausgangItem;

import java.util.List;

/**
 * The WarenausgangInterface provides methods to retrieve information
 * related to a shipping process in an inventory or warehouse system.
 * It outlines the structure for accessing the unique identifier,
 * the date of shipping, and the list of items associated with the shipping record.
 */
public interface WarenausgangInterface {

    int getWarenausgangId();

    String getWarenausgangDate();

    List<WarenausgangItem> getItems();
}
