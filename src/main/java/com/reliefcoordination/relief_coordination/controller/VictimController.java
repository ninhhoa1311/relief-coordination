package com.reliefcoordination.relief_coordination.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/victim")
public class VictimController {

    @GetMapping("/home")
    public String home(Model model) {
        model.addAttribute("content", "victim/home");
        model.addAttribute("pageTitle", "Trang chủ Victim");
        model.addAttribute("role", "VICTIM");
        return "fragments/layout";
    }
}