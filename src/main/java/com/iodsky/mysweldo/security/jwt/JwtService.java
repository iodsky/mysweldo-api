package com.iodsky.mysweldo.security.jwt;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseCookie;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import java.security.Key;
import java.util.Date;
import java.util.Map;
import java.util.function.Function;

@Service
public class JwtService {

    @Value("${security.jwt.secret-key}")
    private String secretKey;

    @Value("${security.jwt.access-token.expiration-time}")
    private Long accessTokenExpiration;

    @Value("${security.jwt.refresh-token.expiration-time}")
    private Long refreshTokenExpiration;

    @Value("${security.jwt.cookie.secure:false}")
    private boolean secure;

    @Value("${security.jwt.cookie.http-only:true}")
    private boolean httpOnly;

    @Value("${security.jwt.cookie.same-site:Lax}")
    private String sameSite;

    private static final String JWT_COOKIE_NAME = "jwt";
    private static final String ACCESS_TOKEN_COOKIE_NAME = "access_token";
    private static final String COOKIE_PATH = "/";

    public String buildToken(Map<String, Object> claims, UserDetails userDetails, Long expirationTime) {
        return Jwts
                .builder()
                .setClaims(claims)
                .setSubject(userDetails.getUsername())
                .setIssuedAt(new Date(System.currentTimeMillis()))
                .setExpiration(new Date(System.currentTimeMillis() + expirationTime))
                .signWith(getSignInKey(), SignatureAlgorithm.HS256)
                .compact();
    }

    public String generateAccessToken(Map<String, Object> claims, UserDetails userDetails) {
        return buildToken(claims, userDetails, accessTokenExpiration);
    }

    public String generateRefreshToken(Map<String, Object> claims, UserDetails userDetails) {
        return buildToken(claims, userDetails, refreshTokenExpiration);
    }

    public boolean isTokenValid(String token, UserDetails userDetails) {
        final String email = extractUserEmail(token);
        return (email.equals(userDetails.getUsername()) && !isTokenExpired(token));
    }

    private boolean isTokenExpired(String token) {
        return extractExpiration(token).before(new Date());
    }

    public String extractUserEmail(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    public String extractAccessType(String token) {
        return extractClaim(token, claims -> claims.get("accessType", String.class));
    }

    private Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }

    private <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }

    private Claims extractAllClaims(String token) {
        return Jwts
                .parserBuilder()
                .setSigningKey(getSignInKey())
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    private Key getSignInKey() {
        byte[] bytes = Decoders.BASE64.decode(secretKey);
        return Keys.hmacShaKeyFor(bytes);
    }

    // Cookie Management Methods

    public void addTokenToCookie(String token, HttpServletResponse response) {
        addCookie(JWT_COOKIE_NAME, token, refreshTokenExpiration / 1000, response);
    }

    public void addAccessTokenCookie(String token, HttpServletResponse response) {
        addCookie(ACCESS_TOKEN_COOKIE_NAME, token, accessTokenExpiration / 1000, response);
    }

    public void clearJwtCookie(HttpServletResponse response) {
        addCookie(JWT_COOKIE_NAME, "", 0, response);
    }

    public void clearAccessTokenCookie(HttpServletResponse response) {
        addCookie(ACCESS_TOKEN_COOKIE_NAME, "", 0, response);
    }

    private void addCookie(String name, String value, long maxAge, HttpServletResponse response) {
        ResponseCookie cookie = ResponseCookie
                .from(name, value)
                .path(COOKIE_PATH)
                .maxAge(maxAge)
                .httpOnly(httpOnly)
                .secure(secure)
                .sameSite(sameSite)
                .build();

        response.addHeader("Set-Cookie", cookie.toString());
    }

    public String getTokenFromCookie(HttpServletRequest request) {
        return getCookieValue(request, JWT_COOKIE_NAME);
    }

    public String getAccessTokenFromCookie(HttpServletRequest request) {
        return getCookieValue(request, ACCESS_TOKEN_COOKIE_NAME);
    }

    private String getCookieValue(HttpServletRequest request, String name) {
        Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            for (Cookie cookie : cookies) {
                if (name.equals(cookie.getName())) {
                    return cookie.getValue();
                }
            }
        }
        return null;
    }
}
