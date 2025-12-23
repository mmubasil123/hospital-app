package com.hospital.demo.service;

import com.hospital.demo.dto.AppointmentDTO;
import com.hospital.demo.model.Appointment;
import com.hospital.demo.model.Patient;
import com.hospital.demo.repository.AppointmentRepository;
import java.util.List;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class AppointmentService {

    AppointmentRepository appointmentRepo;
    PatientService patientService;

    public List<AppointmentDTO> getAllWithDetails() {
        List<Appointment> appointments = appointmentRepo.findAll();

        // INTENTIONAL FLAW: The N+1 Select Problem
        // We loop and call the Service (which calls the Repo) for every single item.
        return appointments.stream().map(appt -> {
            Patient p = patientService.findById(String.valueOf(appt.getPatientId()));
            return new AppointmentDTO(
                appt.getId(),
                p.getFirstName() + " " + p.getLastName(),
                appt.getAppointmentTime()
            );
        }).toList();
    }
}