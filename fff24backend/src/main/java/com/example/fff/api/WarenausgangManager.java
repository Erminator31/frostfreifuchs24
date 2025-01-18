package com.example.fff.api;

import com.example.fff.model.TagesStatistik;
import com.example.fff.model.Warenausgang;
import com.example.fff.model.WarenausgangItem;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.List;

public interface WarenausgangManager {
    void createWarenausgangTable() throws Exception;
    void createWarenausgangItemTable() throws Exception;
    Warenausgang createWarenausgang(List<WarenausgangItem> items) throws Exception;
    Warenausgang createWarenausgang(List<WarenausgangItem> items, Timestamp warenausgangDate) throws Exception;
    Warenausgang getWarenausgang(int warenausgangId) throws Exception;
    List<Warenausgang> getAllWarenausgaenge() throws Exception;
    void deleteWarenausgangTable() throws SQLException;
    void deleteWarenausgangItemsTable() throws SQLException;

    double calculateAverageDailyDemand(int productId, Connection connection) throws SQLException;

    List<Warenausgang> getWarenausgaenge(Timestamp from, Timestamp to) throws Exception;

    List<TagesStatistik> getWarenausgaengeProTag(Timestamp from, Timestamp to) throws Exception;

    boolean deleteWarenausgang(int warenausgangId) throws Exception;
}
