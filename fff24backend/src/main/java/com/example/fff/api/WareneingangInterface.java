package com.example.fff.api;

import com.example.fff.model.WareneingangItem;

import java.util.List;

/**
 * The WareneingangInterface defines methods for managing "Wareneingang" (goods receipt) entities in an inventory system.
 * It provides access to core attributes of a "Wareneingang" record such as its unique identifier, date, items,
 * and operational mode, as well as mechanisms for modifying its mode.
 */
public interface WareneingangInterface {
    int getWareneingangId();

    String getWareneingangDate();

    List<WareneingangItem> getItems();

    String getWareneingangMode();

    void setWareneingangMode(String wareneingangMode);
}
