package dev.nexus.app.identityservice;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<ErrorResponse> handleRuntimeException(RuntimeException ex) {
        HttpStatus status;

        // Determine the correct HTTP status code based on the error message
        if (ex.getMessage().contains("Invalid Credentials")) {
            status = HttpStatus.UNAUTHORIZED; // 401
        } else if (ex.getMessage().contains("already taken")) {
            status = HttpStatus.CONFLICT; // 409
        } else {
            status = HttpStatus.BAD_REQUEST; // 400
        }

        ErrorResponse errorResponse = new ErrorResponse(
                ex.getMessage(),
                status.value(),
                LocalDateTime.now()
        );

        return ResponseEntity.status(status).body(errorResponse);
    }
}