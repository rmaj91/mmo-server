package com.mmo.mmoserver.player;

import com.mmo.mmoserver.auth.SessionRepository;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Arrays;

import static com.mmo.mmoserver.auth.AuthController.SESSION_COOKIE_NAME;

@Slf4j
@RestController
@RequestMapping("/api/player")
@RequiredArgsConstructor
public class PlayerController {

    private final SessionRepository sessionRepository;

    @PostMapping("/state")
    public void updateState(@RequestBody StateUpdateRequest stateUpdateRequest, HttpServletRequest request) {
        String session = getSessionFromRequestCookie(request);
        String username = sessionRepository.getUsername(session);
        log.info("Updating state for player \"{}\", to: \"{}\".", username, stateUpdateRequest.getState());
    }

    @PostMapping("/direction")
    public void updateDirection(@RequestBody RotationUpdateRequest rotationUpdateRequest, HttpServletRequest request) {
        String session = getSessionFromRequestCookie(request);
        String username = sessionRepository.getUsername(session);
        log.info("Updating rotationY for player \"{}\", to:\"{}\".", username, rotationUpdateRequest.getRotationY());
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
