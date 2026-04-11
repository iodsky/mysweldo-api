package com.iodsky.mysweldo.security.jwt;

import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

@Component
public class JwtCookieProvider {

    @Value("${security.jwt.expiration-time}")
    private Long expirationTime;

    @Value("${security.jwt.cookie.secure:false}")
    private boolean secure;

    @Value("${security.jwt.cookie.http-only:true}")
    private boolean httpOnly;

    @Value("${security.jwt.cookie.same-site:Lax}")
    private String sameSite;

    private static final String JWT_COOKIE_NAME = "jwt";
    private static final String COOKIE_PATH = "/";

    public void addJwtCookie(String token, HttpServletResponse response) {
        ResponseCookie cookie = ResponseCookie
                .from(JWT_COOKIE_NAME, token)
                .path(COOKIE_PATH)
                .maxAge(expirationTime / 1000)
                .httpOnly(httpOnly)
                .secure(secure)
                .sameSite(sameSite)
                .build();

        response.addHeader("Set-Cookie", cookie.toString());
    }

    public void clearJwtCookie(HttpServletResponse response) {
        ResponseCookie cookie = ResponseCookie
                .from(JWT_COOKIE_NAME, "")
                .path(COOKIE_PATH)
                .maxAge(0)
                .httpOnly(httpOnly)
                .secure(secure)
                .sameSite(sameSite)
                .build();

        response.addHeader("Set-Cookie", cookie.toString());
    }
}
