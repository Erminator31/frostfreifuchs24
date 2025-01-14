package com.example.fff.model;

public class TagesStatistik {
    private String datum;
    private int anzahl;

    public TagesStatistik(String datum, int anzahl) {
        this.datum = datum;
        this.anzahl = anzahl;
    }

    public String getDatum() {
        return datum;
    }

    public void setDatum(String datum) {
        this.datum = datum;
    }

    public int getAnzahl() {
        return anzahl;
    }

    public void setAnzahl(int anzahl) {
        this.anzahl = anzahl;
    }
}
