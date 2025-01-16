package com.example.fff.model;

public class TagesStatistik {
    private String datum;
    private int anzahl;
    private String produktName; // oder productId

    // Konstruktor, Getter und Setter
    public TagesStatistik(String datum, int anzahl, String produktName) {
        this.datum = datum;
        this.anzahl = anzahl;
        this.produktName = produktName;
    }

    public String getDatum() { return datum; }
    public int getAnzahl() { return anzahl; }
    public String getProduktName() { return produktName; }
}

