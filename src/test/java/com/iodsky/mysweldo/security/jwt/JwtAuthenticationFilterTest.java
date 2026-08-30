package com.iodsky.mysweldo.security.jwt;

import io.jsonwebtoken.MalformedJwtException;
import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.User;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class JwtAuthenticationFilterTest {

    @InjectMocks
    private JwtAuthenticationFilter filter;

    @Mock
    private JwtService jwtService;

    @Mock
    private UserDetailsService userDetailsService;

    @Mock
    private FilterChain filterChain;

    private MockHttpServletRequest request;
    private MockHttpServletResponse response;

    private void initRequest(String authorizationHeader) {
        request = new MockHttpServletRequest();
        response = new MockHttpServletResponse();
        if (authorizationHeader != null) {
            request.addHeader("Authorization", authorizationHeader);
        }
    }

    @Nested
    class MalformedTokenTests {

        @Test
        void shouldNotThrowAndContinueChainWhenTokenIsMalformed() throws Exception {
            initRequest("Bearer not.a.jwt");

            when(jwtService.extractUserEmail("not.a.jwt"))
                    .thenThrow(new MalformedJwtException("Malformed token"));

            assertThatCode(() -> filter.doFilterInternal(request, response, filterChain))
                    .doesNotThrowAnyException();

            verify(filterChain).doFilter(request, response);
            assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        }

        @Test
        void shouldNotThrowAndContinueChainWhenTokenIsBlank() throws Exception {
            initRequest("Bearer ");

            when(jwtService.extractUserEmail(""))
                    .thenThrow(new IllegalArgumentException("JWT String argument cannot be null or empty."));

            assertThatCode(() -> filter.doFilterInternal(request, response, filterChain))
                    .doesNotThrowAnyException();

            verify(filterChain).doFilter(request, response);
            assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        }

        @Test
        void shouldNotThrowAndContinueChainWhenTokenIsExpired() throws Exception {
            initRequest("Bearer expired.token.value");

            when(jwtService.extractUserEmail("expired.token.value"))
                    .thenThrow(new io.jsonwebtoken.ExpiredJwtException(null, null, "Expired token"));

            assertThatCode(() -> filter.doFilterInternal(request, response, filterChain))
                    .doesNotThrowAnyException();

            verify(filterChain).doFilter(request, response);
            assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        }
    }

    @Nested
    class ValidTokenTests {

        @Test
        void shouldSetAuthenticationWhenTokenIsValid() throws Exception {
            initRequest("Bearer valid.token.value");
            UserDetails userDetails = new User("admin@example.com", "password", List.of());

            when(jwtService.extractUserEmail("valid.token.value")).thenReturn("admin@example.com");
            when(userDetailsService.loadUserByUsername("admin@example.com")).thenReturn(userDetails);
            when(jwtService.isTokenValid("valid.token.value", userDetails)).thenReturn(true);

            filter.doFilterInternal(request, response, filterChain);

            verify(filterChain).doFilter(request, response);
            assertThat(SecurityContextHolder.getContext().getAuthentication())
                    .isInstanceOf(UsernamePasswordAuthenticationToken.class)
                    .extracting(auth -> auth.getPrincipal())
                    .isEqualTo(userDetails);
        }
    }

    @Nested
    class MissingHeaderTests {

        @Test
        void shouldContinueChainWhenNoAuthorizationHeaderPresent() throws Exception {
            initRequest(null);

            filter.doFilterInternal(request, response, filterChain);

            verify(filterChain).doFilter(request, response);
            assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        }

        @Test
        void shouldContinueChainWhenHeaderDoesNotStartWithBearer() throws Exception {
            initRequest("Basic dXNlcjpwYXNz");

            filter.doFilterInternal(request, response, filterChain);

            verify(filterChain).doFilter(request, response);
            assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        }
    }
}