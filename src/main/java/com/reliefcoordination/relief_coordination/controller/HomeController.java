package com.reliefcoordination.relief_coordination.controller;

import com.reliefcoordination.relief_coordination.service.PublicStatsService;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class HomeController {

    private final PublicStatsService publicStatsService;

    public HomeController(PublicStatsService publicStatsService) {
        this.publicStatsService = publicStatsService;
    }

    @GetMapping("/")
    public String index(Authentication authentication, Model model) {
        if (authentication != null
                && authentication.isAuthenticated()
                && !(authentication instanceof AnonymousAuthenticationToken)) {
            return "redirect:/home";
        }
        model.addAttribute("stats", publicStatsService.getPublicStats());
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