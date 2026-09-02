package com.iodsky.mysweldo.security.auth;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
@Tag(name = "Authentication", description = "Authentication endpoints")
public class AuthenticationController {

    private final AuthenticationService service;

    @PostMapping("/login")
    @Operation(summary = "Login to get JWT token", description = "Authenticate with email and password to receive a token for accessing protected endpoints", operationId = "login")
    @SecurityRequirements()
    public AuthSession authenticate(@Valid @RequestBody AuthRequest request, HttpServletResponse response) {
        AuthSession authResponse = service.authenticate(request);

        String accessToken = service.generateAccessToken(request.getEmail(), request.getAccessType());
        service.addAccessTokenCookie(accessToken, response);

        String refreshToken = service.generateRefreshToken(request.getEmail(), request.getAccessType());
        service.addRefreshTokenCookie(refreshToken, response);

        return authResponse;
    }

    @PostMapping("/logout")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Logout and clear JWT token", description = "Logout from the system and clear the JWT token cookie", operationId = "logout")
    public void logout(HttpServletResponse response) {
        service.clearRefreshTokenCookie(response);
        service.clearAccessTokenCookie(response);
    }

    @PostMapping("/refresh")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Refresh access token", description = "Use the refresh token from cookie to get a new short-lived access token", operationId = "refreshToken")
    public void refresh(HttpServletRequest request, HttpServletResponse response) {
        String accessToken = service.generateAccessToken(request);
        service.addAccessTokenCookie(accessToken, response);
    }

    @GetMapping("/me")
    @Operation(summary = "Get current authenticated user", description = "Retrieve the details of the currently authenticated user", operationId = "getAuthenticatedUser")
    public AuthenticatedUser me(HttpServletRequest request) {
        return service.getAuthenticatedUser(request);
    }

}
