package se.simon.iot.alarm.model;

import jakarta.persistence.*;

@Entity
@Table(name="threshold_rules", uniqueConstraints=@UniqueConstraint(columnNames="measurementType"))
public class ThresholdRule {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
    @Column(nullable=false, unique=true) private String measurementType;
    @Column(nullable=false) private double maximumValue;
    @Column(nullable=false) private String unit;
    protected ThresholdRule() {}
    public ThresholdRule(String measurementType, double maximumValue, String unit) {
        this.measurementType=measurementType; this.maximumValue=maximumValue; this.unit=unit;
    }
    public String getMeasurementType(){return measurementType;} public double getMaximumValue(){return maximumValue;}
    public String getUnit(){return unit;}
}
