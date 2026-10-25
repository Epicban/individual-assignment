package se.simon.iot.alarm.repository;
import org.springframework.data.jpa.repository.JpaRepository;
import se.simon.iot.alarm.model.ThresholdRule;
import java.util.Optional;
public interface ThresholdRepository extends JpaRepository<ThresholdRule, Long> {
    Optional<ThresholdRule> findByMeasurementTypeIgnoreCase(String measurementType);
}
