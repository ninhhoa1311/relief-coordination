package com.reliefcoordination.relief_coordination.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/rescuer")
public class RescuerRequestController {

    @GetMapping("/home")
    public String home(Model model) {
        model.addAttribute("content", "rescuer/home");
        model.addAttribute("pageTitle", "Trang chủ Rescuer");
        model.addAttribute("role", "RESCUER");
        return "fragments/layout";
    }
}