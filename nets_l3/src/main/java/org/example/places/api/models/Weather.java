package org.example.places.api.models;

public class Weather {
    private String description;
    private double temperature;
    private double humidity;

    public Weather() {}

    public Weather(String description, double temperature, double humidity) {
        this.description = description;
        this.temperature = temperature;
        this.humidity = humidity;
    }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public double getTemperature() { return temperature; }
    public void setTemperature(double temperature) { this.temperature = temperature; }

    public double getHumidity() { return humidity; }
    public void setHumidity(double humidity) { this.humidity = humidity; }
}