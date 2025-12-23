package com.hospital.demo.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.time.LocalDateTime;
import java.util.List;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ApiResponse<T> {
    int status;
    String message;
    T data;
    List<String> errors;
    LocalDateTime timestamp;

    public static <T> ResponseEntity<ApiResponse> success(T data, String message) {
        return ResponseEntity.ok(ApiResponse.<T>builder()
            .status(HttpStatus.OK.value())
            .message(message)
            .data(data)
            .timestamp(LocalDateTime.now())
            .build());
    }

    public static <T> ResponseEntity<ApiResponse> created(T data, String message) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.<T>builder()
            .status(HttpStatus.CREATED.value())
            .message(message)
            .data(data)
            .timestamp(LocalDateTime.now())
            .build());
    }
}
