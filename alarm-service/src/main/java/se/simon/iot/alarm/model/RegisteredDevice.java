package se.simon.iot.alarm.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name="registered_devices")
public class RegisteredDevice {
    @Id @Column(length=80) private String deviceId;
    @Column(nullable=false) private String measurementType;
    @Column(nullable=false) private String unit;
    @Column(nullable=false) private Instant registeredAt;
    protected RegisteredDevice() {}
    public RegisteredDevice(String deviceId, String measurementType, String unit) {
        this.deviceId=deviceId; this.measurementType=measurementType; this.unit=unit; this.registeredAt=Instant.now();
    }
    public String getDeviceId(){return deviceId;} public String getMeasurementType(){return measurementType;}
    public String getUnit(){return unit;} public Instant getRegisteredAt(){return registeredAt;}
}
