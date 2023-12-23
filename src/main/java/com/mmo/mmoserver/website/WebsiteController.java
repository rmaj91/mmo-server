package com.mmo.mmoserver.website;

import com.mmo.mmoserver.auth.SessionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class WebsiteController {

    @Autowired
    private SessionService sessionService;

    @GetMapping("/total-online")
    public int getTotalOnline() {
        return sessionService.getTotalOnline();
    }
}
