package com.iodsky.mysweldo.security.auth;

import com.iodsky.mysweldo.security.jwt.JwtService;
import com.iodsky.mysweldo.security.user.User;
import com.iodsky.mysweldo.security.user.UserDto;
import com.iodsky.mysweldo.security.user.UserMapper;
import com.iodsky.mysweldo.security.user.UserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.InternalAuthenticationServiceException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class AuthenticationService {
    private final AuthenticationManager authenticationManager;
    private final UserService userService;
    private final JwtService jwtService;
    private final UserDetailsService userDetailsService;
    private final UserMapper userMapper;

    public AuthSession authenticate(AuthRequest request) {
        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            request.getEmail(),
                            request.getPassword()
                    )
            );
        } catch (InternalAuthenticationServiceException | BadCredentialsException ex) {
            throw new BadCredentialsException("Invalid username or password");
        }

        User user = userService.getUserByEmail(request.getEmail());
        String userRole = user.getRole().getName();

        // Validate that user can access the requested access type
        if (AccessType.ADMIN.equals(request.getAccessType()) && "EMPLOYEE".equals(userRole)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Invalid access for this account");
        }

        UserDto userDto = userMapper.toDto(user);
        return new AuthSession(
                userDto,
                request.getAccessType()
        );
    }

    public AuthenticatedUser getAuthenticatedUser(HttpServletRequest request) {
        String token = jwtService.getAccessTokenFromCookie(request);

        if (token == null || token.isBlank()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid or missing token");
        }

        String userEmail = jwtService.extractUserEmail(token);
        User user = (User) userDetailsService.loadUserByUsername(userEmail);

        if ( !jwtService.isTokenValid(token, user)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid  or expired token");
        }

        AccessType accessType = AccessType.valueOf(jwtService.extractAccessType(token));
        UserDto userDto = userMapper.toDto(user);

        return new AuthenticatedUser(userDto, accessType);
    }

    public String generateAccessToken(String email, AccessType accessType) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("accessType", accessType.name());
        UserDetails userDetails = userDetailsService.loadUserByUsername(email);
        return jwtService.generateAccessToken(claims, userDetails);
    }

    public String generateAccessToken(HttpServletRequest request) {
        String refreshToken = jwtService.getTokenFromCookie(request);

        if (refreshToken == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid or missing refresh token");
        }

        String email = jwtService.extractUserEmail(refreshToken);
        String accessType = jwtService.extractAccessType(refreshToken);
        UserDetails userDetails = userDetailsService.loadUserByUsername(email);

        if (!jwtService.isTokenValid(refreshToken, userDetails)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid refresh token. Please reauthenticate");
        }

        Map<String, Object> claims = new HashMap<>();
        claims.put("accessType", accessType);

        return jwtService.generateAccessToken(claims, userDetails);
    }

    public String generateRefreshToken(String email, AccessType accessType) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("accessType", accessType.name());
        UserDetails userDetails = userDetailsService.loadUserByUsername(email);
        return jwtService.generateRefreshToken(claims, userDetails);
    }

    public void addRefreshTokenCookie(String token, HttpServletResponse response) {
        jwtService.addTokenToCookie(token, response);
    }

    public void addAccessTokenCookie(String token, HttpServletResponse response) {
        jwtService.addAccessTokenCookie(token, response);
    }

    public void clearRefreshTokenCookie(HttpServletResponse response) {
        jwtService.clearJwtCookie(response);
    }

    public void clearAccessTokenCookie(HttpServletResponse response) {
        jwtService.clearAccessTokenCookie(response);
    }

}
