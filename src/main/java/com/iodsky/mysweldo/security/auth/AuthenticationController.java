package com.iodsky.mysweldo.security.auth;

import com.iodsky.mysweldo.common.response.ApiResponse;
import com.iodsky.mysweldo.common.response.ResponseFactory;
import com.iodsky.mysweldo.security.jwt.JwtCookieProvider;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;


@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
@Tag(name = "Authentication", description = "Authentication endpoints")
public class AuthenticationController {

    private final AuthenticationService service;
    private final JwtCookieProvider jwtCookieProvider;

    @PostMapping("/login")
    @Operation(summary = "Login to get JWT token", description = "Authenticate with email and password to receive a JWT token in a secure HTTP-only cookie for accessing protected endpoints")
    @SecurityRequirements()
    public ApiResponse<LoginResponse> authenticate(@Valid @RequestBody LoginRequest request, HttpServletResponse response) {
        LoginResponse loginResponse = service.authenticate(request);

        String token = service.generateToken(loginResponse.getEmail());
        jwtCookieProvider.addJwtCookie(token, response);

        return ResponseFactory.success("Login successful", loginResponse);
    }

    @PostMapping("/logout")
    @Operation(summary = "Logout and clear JWT token", description = "Logout from the system and clear the JWT token cookie")
    public ApiResponse<Void> logout(HttpServletResponse response) {
        jwtCookieProvider.clearJwtCookie(response);
        return ResponseFactory.success("Logout successful", null);
    }

}
