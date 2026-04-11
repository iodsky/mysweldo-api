package com.iodsky.mysweldo.security.auth;

import com.iodsky.mysweldo.security.jwt.JwtUtil;
import com.iodsky.mysweldo.security.user.User;
import com.iodsky.mysweldo.security.user.UserService;
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

@Service
@RequiredArgsConstructor
public class AuthenticationService {
    private final AuthenticationManager authenticationManager;
    private final UserService userService;
    private final JwtUtil jwtUtil;
    private final UserDetailsService userDetailsService;

    public LoginResponse authenticate(LoginRequest request) {
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

        if ("ADMIN".equals(request.getRole()) && "EMPLOYEE".equals(userRole)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Invalid access for this account");
        }

        return new LoginResponse(
                user.getId(),
                user.getEmail(),
                user.getRole().getName(),
                user.getEmployee().getId()
        );
    }

    public String generateToken(String email) {
        UserDetails userDetails = userDetailsService.loadUserByUsername(email);
        return jwtUtil.generateToken(userDetails);
    }
}
