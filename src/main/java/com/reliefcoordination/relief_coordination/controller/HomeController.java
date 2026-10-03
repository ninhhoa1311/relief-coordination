package com.reliefcoordination.relief_coordination.controller;

import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class HomeController {

    @GetMapping("/")
    public String index(Authentication authentication) {
        if (authentication != null
                && authentication.isAuthenticated()
                && !(authentication instanceof AnonymousAuthenticationToken)) {
            return "redirect:/home";
        }
        return "landing";
    }

    @GetMapping("/home")
    public String home(Authentication authentication) {
        for (GrantedAuthority authority : authentication.getAuthorities()) {
            String role = authority.getAuthority();
            if (role.equals("ROLE_ADMIN")) return "redirect:/admin/home";
            if (role.equals("ROLE_RESCUER")) return "redirect:/rescuer/home";
            if (role.equals("ROLE_VICTIM")) return "redirect:/victim/home";
        }
        return "redirect:/login";
    }
}