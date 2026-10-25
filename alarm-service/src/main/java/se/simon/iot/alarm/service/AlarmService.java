package se.simon.iot.alarm.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import se.simon.iot.alarm.endpoint.ContractFault;
import se.simon.iot.alarm.model.AlarmEntity;
import se.simon.iot.alarm.model.ThresholdRule;
import se.simon.iot.alarm.repository.AlarmRepository;
import se.simon.iot.alarm.repository.ThresholdRepository;
import java.time.Instant;
import java.util.List;

@Service
public class AlarmService {
    private final AlarmRepository alarms;
    private final ThresholdRepository thresholds;
    public AlarmService(AlarmRepository alarms, ThresholdRepository thresholds) {
        this.alarms=alarms; this.thresholds=thresholds;
    }
    @Transactional
    public AlarmEntity evaluate(String deviceId, String type, double value, String unit, Instant timestamp) {
        if (deviceId.isBlank() || type.isBlank() || unit.isBlank() || !Double.isFinite(value) || timestamp==null)
            throw new ContractFault("Required measurement data is missing or invalid");
        ThresholdRule rule=thresholds.findByMeasurementTypeIgnoreCase(type)
                .orElseThrow(() -> new ContractFault("No threshold is configured for measurementType: " + type));
        if (!rule.getUnit().equals(unit)) throw new ContractFault("Unit does not match configured threshold");
        if (value <= rule.getMaximumValue()) return null;
        String message=type + " exceeded allowed value " + rule.getMaximumValue() + " " + unit;
        return alarms.save(new AlarmEntity(deviceId, type, value, unit, timestamp, message));
    }
    @Transactional(readOnly=true) public AlarmEntity find(long id) {
        return alarms.findById(id).orElse(null);
    }
    @Transactional(readOnly=true) public List<AlarmEntity> findAll() { return alarms.findAll(); }
}
