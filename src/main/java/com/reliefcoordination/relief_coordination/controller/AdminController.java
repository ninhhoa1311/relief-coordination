package com.reliefcoordination.relief_coordination.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/admin")
public class AdminController {

    @GetMapping("/home")
    public String home(Model model) {
        model.addAttribute("content", "admin/home");
        model.addAttribute("pageTitle", "Trang chủ Admin");
        model.addAttribute("role", "ADMIN");
        return "fragments/layout";
    }
}