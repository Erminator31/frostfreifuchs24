package com.example.fff.api;

import com.example.fff.model.WareneingangItem;

import java.util.List;

public interface WareneingangInterface {
    int getWareneingangId();

    String getWareneingangDate();

    List<WareneingangItem> getItems();
}
