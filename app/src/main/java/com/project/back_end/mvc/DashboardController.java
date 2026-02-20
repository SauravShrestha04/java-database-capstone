package com.project.back_end.mvc;

import com.project.back_end.service.CommonService;
import com.project.back_end.models.Admin; 
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@Controller
public class DashboardController {

    @Autowired
    private CommonService commonService;   // service handling token validation

    // Admin Dashboard
    @GetMapping("/adminDashboard/{token}")
    public String adminDashboard(@PathVariable String token) {
        var validation = commonService.validateToken(token, "admin");

        if (validation.isEmpty()) {
            return "admin/adminDashboard";   // thymeleaf template
        }
        return "redirect:/";                 // invalid token → redirect to login
    }

    // Doctor Dashboard
    @GetMapping("/doctorDashboard/{token}")
    public String doctorDashboard(@PathVariable String token) {
        var validation = commonService.validateToken(token, "doctor");

        if (validation.isEmpty()) {
            return "doctor/doctorDashboard"; // thymeleaf template
        }
        return "redirect:/";
    }
}