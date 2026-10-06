package com.fatec.gisa.exceptions;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.authentication.BadCredentialsException;

class RestExceptionHandlerTest {

    @Test
    void authenticationFailureReturnsStandardUnauthorizedResponse() {
        RestExceptionHandler handler = new RestExceptionHandler();
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/auth/login");

        var response = handler.handleAuthentication(new BadCredentialsException("Bad credentials"), request);

        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(401, response.getBody().status());
        assertEquals("Credenciais inválidas.", response.getBody().message());
        assertEquals("/api/auth/login", response.getBody().path());
    }
}