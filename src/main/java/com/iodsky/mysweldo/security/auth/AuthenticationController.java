package com.iodsky.mysweldo.security.auth;

import com.iodsky.mysweldo.common.response.ApiResponse;
import com.iodsky.mysweldo.common.response.ResponseFactory;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
@Tag(name = "Authentication", description = "Authentication endpoints")
public class AuthenticationController {

    private final AuthenticationService service;

    @PostMapping("/login")
    @Operation(summary = "Login to get JWT token", description = "Authenticate with email and password to receive a token for accessing protected endpoints")
    @SecurityRequirements()
    public ApiResponse<AuthSession> authenticate(@Valid @RequestBody AuthRequest request, HttpServletResponse response) {
        AuthSession authResponse = service.authenticate(request);

        String accessToken = service.generateAccessToken(request.getEmail(), request.getAccessType());
        service.addAccessTokenCookie(accessToken, response);

        String refreshToken = service.generateRefreshToken(request.getEmail(), request.getAccessType());
        service.addRefreshTokenCookie(refreshToken, response);

        return ResponseFactory.success("Authentication successful", authResponse);
    }

    @PostMapping("/logout")
    @Operation(summary = "Logout and clear JWT token", description = "Logout from the system and clear the JWT token cookie")
    public ApiResponse<Void> logout(HttpServletResponse response) {
        service.clearRefreshTokenCookie(response);
        service.clearAccessTokenCookie(response);
        return ResponseFactory.success("Logout success", null);
    }

    @PostMapping("/refresh")
    @Operation(summary = "Refresh access token", description = "Use the refresh token from cookie to get a new short-lived access token")
    public ApiResponse<Void> refresh(HttpServletRequest request, HttpServletResponse response) {
        String accessToken = service.generateAccessToken(request);
        service.addAccessTokenCookie(accessToken, response);
        return ResponseFactory.success("Refresh success", null);
    }

    @GetMapping("/me")
    @Operation(summary = "Get current authenticated user", description = "Retrieve the details of the currently authenticated user")
    public ApiResponse<AuthenticatedUser> me(HttpServletRequest request) {
        AuthenticatedUser response = service.getAuthenticatedUser(request);
        return ResponseFactory.success("Authenticated user fetched", response);
    }

}
