package se.simon.iot.alarm.repository;
import org.springframework.data.jpa.repository.JpaRepository;
import se.simon.iot.alarm.model.RegisteredDevice;
public interface DeviceRepository extends JpaRepository<RegisteredDevice, String> {}
