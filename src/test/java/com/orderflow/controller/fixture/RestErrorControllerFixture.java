package com.orderflow.controller.fixture;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Size;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class RestErrorControllerFixture {

    @PostMapping("/_test/validation")
    public void validate(@Valid @RequestBody RequestBodyFixture request) {
    }

    @GetMapping("/_test/unexpected")
    public void fail() {
        throw new IllegalStateException("SQL=SELECT *; password=fixture-secret");
    }

    public record RequestBodyFixture(@Size(max = 3) String value) {
    }
}
