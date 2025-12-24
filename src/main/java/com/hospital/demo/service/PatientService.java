package com.hospital.demo.service;

import com.hospital.demo.dto.PatientRequest;
import com.hospital.demo.exception.ResourceNotFoundException;
import com.hospital.demo.model.Patient;
import com.hospital.demo.repository.PatientRepository;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class PatientService {

    PatientRepository patientRepository;

    public Patient findByEmail(String email) {
        Optional<Patient> patient =  patientRepository.findByEmail(email);
        if (patient.isEmpty()) {
            log.error("Patient not found with email {}", email);
            throw new ResourceNotFoundException("Patient not found with email: " + email);
        }
        return  patient.get();
    }

    public Patient findById(String id) {
        Optional<Patient> patient = patientRepository.findById(UUID.fromString(id));
        if (patient.isEmpty()) {
            log.error("Patient not found with id {}", id);
            throw new ResourceNotFoundException("Patient not found");
        }
        return  patient.get();
    }

    public void createPatient(PatientRequest patient) {
        Patient builder = Patient.builder()
            .firstName(patient.getFirstName())
            .lastName(patient.getLastName())
            .email(patient.getEmail())
            .build();
        patientRepository.save(builder);
    }

    public void deletePatientById(String uuid) {
        Patient patient = findById(uuid);
        patientRepository.delete(patient);
    }

    public void deletePatientByEmail (String email) {
        Patient patient = findByEmail(email);
        patientRepository.delete(patient);
    }

    public Map<UUID, Patient> findAllByIdsMap(List<UUID> ids) {
        return patientRepository.findAllById(ids).stream()
            .collect(Collectors.toMap(
                Patient::getId,
                Function.identity()
            ));
    }
}
