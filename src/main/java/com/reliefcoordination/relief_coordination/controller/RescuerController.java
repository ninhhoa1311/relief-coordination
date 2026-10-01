package com.reliefcoordination.relief_coordination.controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
@RequestMapping("/rescuer")
public class RescuerController {

    @GetMapping("/home")
    public String home(Model model) {
        model.addAttribute("content", "rescuer/home");
        model.addAttribute("pageTitle", "Trang chu Rescuer");
        return "fragments/layout";
    }
}