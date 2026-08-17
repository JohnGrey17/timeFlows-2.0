package example.timeflows.controller;

import example.timeflows.service.OvertimeReviewPageService;
import example.timeflows.service.OvertimeService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class OvertimeReviewController {
    private final OvertimeService overtimeService;
    private final OvertimeReviewPageService pageService;

    public OvertimeReviewController(
            OvertimeService overtimeService, OvertimeReviewPageService pageService) {
        this.overtimeService = overtimeService;
        this.pageService = pageService;
    }

    @GetMapping("/api/overtime/review")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    public String review(
            @RequestParam(defaultValue = "division") String mode,
            @RequestParam(defaultValue = "matrix") String view,
            @RequestParam(required = false) Long departmentId,
            @RequestParam(required = false) Long divisionId,
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) Integer month,
            @RequestParam(required = false) Long openBonusUserId,
            Authentication authentication,
            Model model) {
        model.addAllAttributes(
                pageService.buildPage(
                        authentication.getName(),
                        view,
                        departmentId,
                        divisionId,
                        year,
                        month,
                        openBonusUserId));
        return "manager/overtime-review";
    }

    @PostMapping("/api/overtime/review/approve")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    public String approve(
            @RequestParam Long overtimeId,
            @RequestParam(required = false) String comment,
            Authentication authentication) {
        overtimeService.approve(overtimeId, comment, authentication.getName());
        return "redirect:/api/overtime/review";
    }

    @PostMapping("/api/overtime/review/reject")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    public String reject(
            @RequestParam Long overtimeId,
            @RequestParam String comment,
            Authentication authentication) {
        overtimeService.reject(overtimeId, comment, authentication.getName());
        return "redirect:/api/overtime/review";
    }
}
