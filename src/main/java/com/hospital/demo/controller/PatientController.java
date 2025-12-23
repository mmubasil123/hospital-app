package com.hospital.demo.controller;

import com.hospital.demo.dto.ApiEnvelope;
import com.hospital.demo.dto.PatientRequest;
import com.hospital.demo.model.Patient;
import com.hospital.demo.service.PatientService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/patients")
@RequiredArgsConstructor
@Tag(name = "Patient API")
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class PatientController {

    PatientService patientService;

    @Operation(summary = "Search by email", description = "Triggers a full table scan for performance analysis.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Patient found successfully",
            content = @Content(schema = @Schema(implementation = ApiEnvelope.class))),
        @ApiResponse(responseCode = "404", description = "Patient not found"),
        @ApiResponse(responseCode = "401", description = "Unauthorized access"),
        @ApiResponse(responseCode = "500", description = "Internal system failure during processing")
    })
    @GetMapping("/search")
    public ResponseEntity<ApiEnvelope<Patient>> search(@RequestParam String email) {
        return ApiEnvelope.success(patientService.findByEmail(email), "Patient found");
    }

    @Operation(summary = "Register new patient", description = "Validates and persists a new patient record.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "201", description = "Created successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid input data"),
        @ApiResponse(responseCode = "401", description = "Unauthorized access"),
        @ApiResponse(responseCode = "500", description = "Internal system failure during processing")
    })
    public ResponseEntity create(@Valid @RequestBody PatientRequest patient) {
        patientService.createPatient(patient);
        return ApiEnvelope.created("Patient registered");
    }

}
