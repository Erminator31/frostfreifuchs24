package com.example.fff.api;

import com.example.fff.model.TagesStatistik;
import com.example.fff.model.Wareneingang;
import com.example.fff.model.WareneingangItem;

import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.List;

public interface WareneingangManager {
    void createWareneingangTable() throws Exception;
    void createWareneingangItemTable() throws Exception;
    Wareneingang createWareneingang(List<WareneingangItem> items) throws Exception;
    Wareneingang createWareneingang(List<WareneingangItem> items, Timestamp wareneingangDate) throws Exception;
    Wareneingang getWareneingang(int wareneingangId) throws Exception;
    List<Wareneingang> getAllWareneingaenge() throws Exception;
    void deleteWareneingangTable() throws SQLException;
    void deleteWareneingangItemsTable() throws SQLException;

    List<TagesStatistik> getWareneingaengeProTag(Timestamp from, Timestamp to) throws Exception;
}
