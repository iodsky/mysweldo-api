package com.iodsky.mysweldo.security.auth;

import com.iodsky.mysweldo.common.response.ApiResponse;
import com.iodsky.mysweldo.common.response.ResponseFactory;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
@Tag(name = "Authentication", description = "Authentication endpoints")
public class AuthenticationController {

    private final AuthenticationService service;

    @PostMapping("/login")
    @Operation(summary = "Login to get JWT token", description = "Authenticate with email and password to receive a JWT token for accessing protected endpoints")
    @SecurityRequirements()
    public ApiResponse<Map<String, String>> authenticate(@Valid @RequestBody  LoginRequest loginRequest) {
        String token = service.authenticate(loginRequest);
        return ResponseFactory.success("Login successful", Map.of("token", token));
    }

}
