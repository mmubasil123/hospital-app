package com.hospital.demo.controller;

import com.hospital.demo.dto.ApiEnvelope;
import com.hospital.demo.dto.AppointmentResponse;
import com.hospital.demo.service.AppointmentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/appointments")
@RequiredArgsConstructor
@Tag(name = "Appointment Management")
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class AppointmentController {

    AppointmentService appointmentService;

    @Operation(summary = "Fetch all appointments with patient names",
        description = "Performance Note: Demonstrates the N+1 Select problem in the orchestration layer.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Successfully retrieved Appointment List"),
        @ApiResponse(responseCode = "401", description = "Unauthorized access"),
        @ApiResponse(responseCode = "500", description = "Internal system failure during processing")
    })
    @GetMapping
    public ResponseEntity<ApiEnvelope<List<AppointmentResponse>>> listAll() {
        return ApiEnvelope.success(appointmentService.getAllWithDetails(), "Report generated");
    }
}
