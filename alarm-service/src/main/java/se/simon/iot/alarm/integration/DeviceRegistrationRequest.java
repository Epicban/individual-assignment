package se.simon.iot.alarm.integration;
public record DeviceRegistrationRequest(String deviceId, String measurementType, String unit) {}
