package se.simon.iot.alarm.repository;
import org.springframework.data.jpa.repository.JpaRepository;
import se.simon.iot.alarm.model.AlarmEntity;
public interface AlarmRepository extends JpaRepository<AlarmEntity, Long> {}
