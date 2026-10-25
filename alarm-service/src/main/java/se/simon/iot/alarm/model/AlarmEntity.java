package se.simon.iot.alarm.model;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "alarms")
public class AlarmEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false) private String deviceId;
    @Column(nullable = false) private String measurementType;
    @Column(name = "measurement_value", nullable = false) private double value;
    @Column(nullable = false) private String unit;
    @Column(nullable = false) private Instant measuredAt;
    @Column(nullable = false) private Instant createdAt;
    @Column(nullable = false) private String message;

    protected AlarmEntity() {}
    public AlarmEntity(String deviceId, String measurementType, double value, String unit, Instant measuredAt, String message) {
        this.deviceId=deviceId; this.measurementType=measurementType; this.value=value; this.unit=unit;
        this.measuredAt=measuredAt; this.createdAt=Instant.now(); this.message=message;
    }
    public Long getId(){return id;} public String getDeviceId(){return deviceId;}
    public String getMeasurementType(){return measurementType;} public double getValue(){return value;}
    public String getUnit(){return unit;} public Instant getMeasuredAt(){return measuredAt;}
    public Instant getCreatedAt(){return createdAt;} public String getMessage(){return message;}
}
