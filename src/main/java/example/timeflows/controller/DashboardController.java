package example.timeflows.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class DashboardController {

    @GetMapping("/")
    public String index() {
        return "redirect:/api/dashboard";
    }

    @GetMapping("/api/dashboard")
    public String dashboard() {
        return "redirect:/api/overtime";
    }
}
