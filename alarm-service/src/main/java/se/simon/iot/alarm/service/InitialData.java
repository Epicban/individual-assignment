package se.simon.iot.alarm.service;

import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import se.simon.iot.alarm.model.ThresholdRule;
import se.simon.iot.alarm.repository.ThresholdRepository;

@Configuration
public class InitialData {
    @Bean CommandLineRunner seedThresholds(ThresholdRepository repository) {
        return args -> {
            if (repository.findByMeasurementTypeIgnoreCase("humidity").isEmpty())
                repository.save(new ThresholdRule("humidity", 60.0, "%"));
            if (repository.findByMeasurementTypeIgnoreCase("temperature").isEmpty())
                repository.save(new ThresholdRule("temperature", 30.0, "°C"));
        };
    }
}
