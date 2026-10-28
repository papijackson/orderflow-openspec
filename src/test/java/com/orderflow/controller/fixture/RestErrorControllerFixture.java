package com.orderflow.controller.fixture;

import com.orderflow.exception.IdempotencyConflictException;
import com.orderflow.exception.OrderNotFoundException;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Size;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
public class RestErrorControllerFixture {

    @PostMapping("/_test/validation")
    public void validate(@Valid @RequestBody RequestBodyFixture request) {
    }

    @GetMapping("/_test/unexpected")
    public void fail() {
        throw new IllegalStateException("SQL=SELECT *; password=fixture-secret");
    }

    @GetMapping("/_test/not-found")
    public void notFound() {
        throw new OrderNotFoundException(UUID.fromString("00000000-0000-0000-0000-000000000001"));
    }

    @GetMapping("/_test/conflict")
    public void conflict() {
        throw new IdempotencyConflictException();
    }

    public record RequestBodyFixture(@Size(max = 3) String value) {
    }
}
