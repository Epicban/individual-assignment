package se.simon.iot.alarm.integration;
public record MeasurementRequest(String deviceId, String measurementType, Double value, String unit, String timestamp) {}
