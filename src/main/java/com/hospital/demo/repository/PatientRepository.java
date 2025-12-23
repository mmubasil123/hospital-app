package com.hospital.demo.repository;

import com.hospital.demo.model.Patient;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PatientRepository extends JpaRepository<Patient, UUID> {
    // This will be slow because of missing index
    Optional<Patient> findByEmail(String email);
}
