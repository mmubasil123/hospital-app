package com.hospital.core.util;

import com.github.javafaker.Faker;

import com.hospital.demo.model.Appointment;
import com.hospital.demo.model.Patient;
import com.hospital.demo.repository.AppointmentRepository;
import com.hospital.demo.repository.PatientRepository;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.IntStream;

@Component
@RequiredArgsConstructor
@Slf4j
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class DataSeeder implements CommandLineRunner {

    PatientRepository patientRepo;
    AppointmentRepository apptRepo;
    Faker faker = new Faker();

    @Override
    // Note: We do NOT use @Transactional on the whole method because
    // 150,000 records in one transaction might overwhelm the Postgres Undo Log/WAL.
    public void run(String... args) {
        if (patientRepo.count() > 0) {
            log.info("Database already seeded. Skipping...");
            return;
        }

        seedPatients();
        seedAppointments();

        log.info("System baseline established. 100k Patients, 50k Appointments.");
    }

    private void seedPatients() {
        log.info("Generating 100,000 patients in batches of 1,000...");
        for (int i = 0; i < 100; i++) {
            List<Patient> batch = IntStream.range(0, 1000).mapToObj(n ->
                Patient.builder()
                    .firstName(faker.name().firstName())
                    .lastName(faker.name().lastName())
                    .email(UUID.randomUUID() + "@hospital.com")
                    .build()
            ).toList();
            patientRepo.saveAll(batch);
        }
    }

    private void seedAppointments() {
        log.info("Fetching patient IDs for appointment generation...");
        List<UUID> patientIds = patientRepo.findAllIds();

        log.info("Generating 50,000 appointments...");
        List<Appointment> batch = new ArrayList<>();

        for (int i = 1; i <= 50000; i++) {
            batch.add(Appointment.builder()
                .patientId(patientIds.get(faker.random().nextInt(0, 99999)))
                .appointmentTime(LocalDateTime.now().plusDays(faker.random().nextInt(1, 30)))
                .notes(faker.lorem().sentence(10))
                .build());

            if (i % 1000 == 0) {
                apptRepo.saveAll(batch);
                batch.clear();
                log.info("Saved {} appointments...", i);
            }
        }
    }
}