package com.reliefcoordination.relief_coordination.controller;

import com.reliefcoordination.relief_coordination.dto.EmergencyRequestDto;
import com.reliefcoordination.relief_coordination.service.EmergencyService;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * Controller xử lý yêu cầu khẩn cấp (SOS) — không yêu cầu đăng nhập.
 * URL: /emergency (GET form), /emergency (POST submit), /emergency/success (GET xác nhận).
 */
@Controller
@RequestMapping("/emergency")
public class EmergencyController {

    private final EmergencyService emergencyService;

    public EmergencyController(EmergencyService emergencyService) {
        this.emergencyService = emergencyService;
    }

    /** Hiển thị form gửi yêu cầu SOS. */
    @GetMapping
    public String hienThiForm(Model model) {
        model.addAttribute("emergencyDto", new EmergencyRequestDto());
        return "emergency/form";
    }

    /**
     * Xử lý POST gửi yêu cầu SOS.
     * Nếu validation lỗi → quay lại form.
     * Nếu SĐT đã có yêu cầu đang hoạt động → báo lỗi.
     * Nếu hợp lệ → tạo yêu cầu, redirect sang /emergency/success (PRG pattern).
     */
    @PostMapping
    public String xuLyGuiYeuCau(
            @Valid @ModelAttribute("emergencyDto") EmergencyRequestDto dto,
            BindingResult bindingResult,
            RedirectAttributes redirectAttributes) {

        // Validation lỗi → hiện lại form với thông báo
        if (bindingResult.hasErrors()) {
            return "emergency/form";
        }

        // Kiểm tra chống spam: cùng SĐT đã có yêu cầu đang hoạt động
        if (emergencyService.daCoYeuCauDangHoatDong(dto.getPhoneNumber())) {
            redirectAttributes.addFlashAttribute("warningMessage",
                    "Yêu cầu của bạn đã được ghi nhận trước đó. " +
                    "Lực lượng cứu trợ sẽ liên hệ qua số " + dto.getPhoneNumber() + ".");
            return "redirect:/emergency/success";
        }

        // Tạo yêu cầu SOS — logic nằm trong service
        emergencyService.taoYeuCauSOS(dto);

        redirectAttributes.addFlashAttribute("successMessage",
                "Yêu cầu khẩn cấp đã được gửi thành công!");
        redirectAttributes.addFlashAttribute("phoneNumber", dto.getPhoneNumber());
        return "redirect:/emergency/success";
    }

    /** Trang xác nhận sau khi gửi thành công (PRG — không render trực tiếp sau POST). */
    @GetMapping("/success")
    public String hienThiThanhCong() {
        return "emergency/success";
    }
}
