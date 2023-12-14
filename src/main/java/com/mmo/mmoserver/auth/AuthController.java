package com.mmo.mmoserver.auth;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    public static final String SESSION_COOKIE_NAME = "session";

    @Autowired
    private final SessionRepository sessionRepository;

    @PostMapping
    public ResponseEntity<?> authenticate(@RequestBody AuthRequest authRequest, HttpServletRequest request, HttpServletResponse response) {
        String username = authRequest.getUsername();
        if (!StringUtils.hasText(username)) {
            log.info("username cannot be empty");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }
        if (sessionRepository.isAlreadyAuthenticated(username)) {
            log.info("\"{}\" username already authenticated.", username);
            String sessionFromRequestCookie = getSessionFromRequestCookie(request);
            String usernameFromCookie = sessionRepository.getUsername(sessionFromRequestCookie);
            if (!username.equals(usernameFromCookie)) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
            }
            return ResponseEntity.ok().build();
        }

        String session = UUID.randomUUID().toString();
        sessionRepository.setSession(username, session);
        Cookie sessionCookie = new Cookie(SESSION_COOKIE_NAME, session);
        sessionCookie.setMaxAge(24 * 60 * 60);
        sessionCookie.setPath("/");
        response.addCookie(sessionCookie);
        log.info("\"{}\" user authenticated successfully!", username);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest loginRequest, HttpServletRequest request) {
        String session = getSessionFromRequestCookie(request);

        String username = sessionRepository.getUsername(session);
        String character = loginRequest.getCharacter();
        // for now character must be the same as username.
        if (StringUtils.hasText(username) && username.equals(character)) {
            LoginResponse loginResponse = new LoginResponse();
            loginResponse.setCharacter(character);
            log.info("\"{}\" logged in successfully!", character);
            return ResponseEntity.ok(loginResponse);
        } else {
            log.info("\"{}\" failed to login", character);
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
    }

    @PostMapping("/logout")
    public ResponseEntity<?> logout(HttpServletRequest request, HttpServletResponse response) {
        String session = getSessionFromRequestCookie(request);
        if (sessionRepository.isSessionExist(session)) {
            String username = sessionRepository.getUsername(session);
            log.info("\"{}\" successfully logout.", username);
            sessionRepository.removeSession(session);
            Cookie cookie = new Cookie(SESSION_COOKIE_NAME, "");
            cookie.setMaxAge(0);
            cookie.setPath("/");
            response.addCookie(cookie);
            return ResponseEntity.noContent().build();
        }
        log.info("Failed to logout. session {}", session);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
    }

    private static String getSessionFromRequestCookie(HttpServletRequest request) {
        String session = null;
        Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            session =  Arrays.stream(cookies)
                    .filter(cookie -> SESSION_COOKIE_NAME.equals(cookie.getName()))
                    .findFirst()
                    .map(Cookie::getValue)
                    .orElse(null);
        }
        return session;
    }
}
