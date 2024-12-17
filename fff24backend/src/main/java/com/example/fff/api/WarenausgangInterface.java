package com.example.fff.api;

import com.example.fff.model.WarenausgangItem;

import java.util.List;

public interface WarenausgangInterface {

    int getWarenausgangId();

    String getWarenausgangDate();

    List<WarenausgangItem> getItems();
}
