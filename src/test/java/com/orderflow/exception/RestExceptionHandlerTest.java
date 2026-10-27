package com.orderflow.exception;

import com.orderflow.controller.fixture.RestErrorControllerFixture;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

@WebMvcTest(
        controllers = RestErrorControllerFixture.class,
        properties = "spring.datasource.url=jdbc:postgresql://localhost:5432/orderflow")
@Import(RestExceptionHandler.class)
class RestExceptionHandlerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void shouldReturnProblemDetailsForInvalidRequestBody() throws Exception {
        var response = mockMvc.perform(post("/_test/validation")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"value\":\"secret-input\"}"))
                .andReturn()
                .getResponse();

        assertThat(response.getStatus()).isEqualTo(400);
        assertThat(response.getContentType()).isEqualTo(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
        assertThat(response.getContentAsString())
                .contains("\"status\":400", "Request validation failed.")
                .doesNotContain("secret-input");
    }

    @Test
    void shouldReturnGenericProblemDetailsForUnexpectedFailure() throws Exception {
        var response = mockMvc.perform(get("/_test/unexpected"))
                .andReturn()
                .getResponse();

        assertThat(response.getStatus()).isEqualTo(500);
        assertThat(response.getContentType()).isEqualTo(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
        assertThat(response.getContentAsString())
                .contains("\"status\":500", "An unexpected error occurred.")
                .doesNotContain("fixture-secret", "SELECT", "IllegalStateException");
    }
}
