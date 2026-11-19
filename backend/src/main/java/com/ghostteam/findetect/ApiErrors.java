package com.ghostteam.findetect;

import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;

@RestControllerAdvice
public class ApiErrors {
    record Field(String field,String message) {}
    record ErrorBody(String code,String message,List<Field> details) {}

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorBody> invalid(MethodArgumentNotValidException error) {
        var fields=error.getBindingResult().getFieldErrors().stream()
                .map(e -> new Field(e.getField(),"Недопустимое значение")).toList();
        return ResponseEntity.badRequest().body(new ErrorBody("VALIDATION","Проверьте поля",fields));
    }

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<ErrorBody> rejected(ResponseStatusException error) {
        String code=switch(error.getStatusCode().value()) {
            case 401 -> "UNAUTHORIZED";
            case 409 -> "CONFLICT";
            default -> "INVALID_REQUEST";
        };
        return ResponseEntity.status(error.getStatusCode()).body(new ErrorBody(code,code,List.of()));
    }
}
