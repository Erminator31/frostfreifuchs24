package com.example.fff.model;

/**
 * Represents weights used in forecasting calculations, defined by three parameters: alpha, beta, and gamma.
 * These weights typically influence smoothing factors in forecasting models such as exponential smoothing.
 */
public class ForecastWeights {
    private double alpha;
    private double beta;
    private double gamma;

    public ForecastWeights(double alpha, double beta, double gamma) {
        this.alpha = alpha;
        this.beta = beta;
        this.gamma = gamma;
    }

    public double getAlpha() { return alpha; }
    public void setAlpha(double alpha) { this.alpha = alpha; }

    public double getBeta() { return beta; }
    public void setBeta(double beta) { this.beta = beta; }

    public double getGamma() { return gamma; }
    public void setGamma(double gamma) { this.gamma = gamma; }
}

