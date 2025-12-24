package com.hospital.demo.service;

import com.hospital.demo.dto.AppointmentResponse;
import com.hospital.demo.model.Appointment;
import com.hospital.demo.model.Patient;
import com.hospital.demo.repository.AppointmentRepository;
import java.util.List;
import java.util.Map;
import java.util.UUID;
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

    public List<AppointmentResponse> getAllWithDetails() {
        List<Appointment> appointments = appointmentRepo.findAll();

        // 2. Extract the unique Patient IDs needed for this request
        List<UUID> patientIds = appointments.stream()
            .map(Appointment::getPatientId)
            .distinct()
            .toList();

        // 3. Fetch all needed patients in ONE batch (Query #2)
        Map<UUID, Patient> patientMap = patientService.findAllByIdsMap(patientIds);

          // Comment out this return and comment the return below to play with the N+1 query.
//        // INTENTIONAL FLAW: The N+1 Select Problem
//        // We loop and call the Service (which calls the Repo) for every single item.
//        return appointments.stream().map(appt -> {
//            Patient p = patientService.findById(String.valueOf(appt.getPatientId()));
//            return new AppointmentResponse(
//                appt.getId(),
//                p.getFirstName() + " " + p.getLastName(),
//                appt.getAppointmentTime(),
//                appt.getNotes()
//            );
//        }).toList();

        // 4. Merge the data into our Response DTO
        // Notice: NO MORE DATABASE CALLS INSIDE THIS MAP!
        return appointments.stream().map(appt -> {
            Patient p = patientMap.get(appt.getPatientId());

            return AppointmentResponse.builder()
                .id(appt.getId())
                .patientFullName(p.getFirstName() + " " + p.getLastName())
                .startTime(appt.getAppointmentTime())
                .notes(appt.getNotes())
                .build();
        }).toList();
    }
}