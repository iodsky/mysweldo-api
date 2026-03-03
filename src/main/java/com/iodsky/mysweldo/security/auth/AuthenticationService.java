package com.iodsky.mysweldo.security.auth;

import com.iodsky.mysweldo.security.jwt.JwtUtil;
import com.iodsky.mysweldo.security.user.User;
import com.iodsky.mysweldo.security.user.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.InternalAuthenticationServiceException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;


@Service
@RequiredArgsConstructor
public class AuthenticationService {
    private final AuthenticationManager authenticationManager;
    private final UserService userService;
    private final JwtUtil jwtUtil;

    public String authenticate(LoginRequest loginRequest) {

        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            loginRequest.getEmail(),
                            loginRequest.getPassword()
                    )
            );
        } catch (InternalAuthenticationServiceException e) {
            if (e.getMessage().contains("404")) {
                String message = e.getMessage().split("\"")[1].trim();
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, message);
            }
        }

        User user = userService.getUserByEmail(loginRequest.getEmail());
        return jwtUtil.generateToken(user);
    }

}
