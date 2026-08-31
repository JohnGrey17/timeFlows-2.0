package example.timeflows.controller;

import example.timeflows.model.Overtime;
import example.timeflows.model.OvertimeStatus;
import example.timeflows.service.OvertimeReviewExcelService;
import example.timeflows.service.OvertimeReviewPageService;
import example.timeflows.service.OvertimeService;
import example.timeflows.service.SavedOvertimeFilterService;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class OvertimeReviewController {
    private final OvertimeService overtimeService;
    private final OvertimeReviewPageService pageService;
    private final OvertimeReviewExcelService excelService;
    private final SavedOvertimeFilterService savedFilterService;

    public OvertimeReviewController(
            OvertimeService overtimeService,
            OvertimeReviewPageService pageService,
            OvertimeReviewExcelService excelService,
            SavedOvertimeFilterService savedFilterService) {
        this.overtimeService = overtimeService;
        this.pageService = pageService;
        this.excelService = excelService;
        this.savedFilterService = savedFilterService;
    }

    @GetMapping("/api/overtime/review/export")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    public ResponseEntity<byte[]> export(
            @RequestParam(required = false) Long departmentId,
            @RequestParam(required = false) Long directorateId,
            @RequestParam(required = false) Long divisionId,
            @RequestParam(required = false) Long subdivisionId,
            @RequestParam(required = false) OvertimeStatus status,
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) Integer month,
            Authentication authentication) {
        var page =
                pageService.buildPage(
                        authentication.getName(),
                        "division",
                        "summary",
                        departmentId,
                        directorateId,
                        divisionId,
                        subdivisionId,
                        status,
                        year,
                        month,
                        null,
                        null);
        var result = excelService.exportSummary(page);
        ContentDisposition disposition =
                ContentDisposition.attachment()
                        .filename(result.filename(), StandardCharsets.UTF_8)
                        .build();
        return ResponseEntity.ok()
                .contentType(
                        MediaType.parseMediaType(
                                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .header(HttpHeaders.CONTENT_DISPOSITION, disposition.toString())
                .contentLength(result.content().length)
                .body(result.content());
    }

    @GetMapping("/api/overtime/review")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    public String review(
            @RequestParam(defaultValue = "division") String mode,
            @RequestParam(defaultValue = "matrix") String view,
            @RequestParam(required = false) Long departmentId,
            @RequestParam(required = false) Long directorateId,
            @RequestParam(required = false) Long divisionId,
            @RequestParam(required = false) Long subdivisionId,
            @RequestParam(required = false) OvertimeStatus status,
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) Integer month,
            @RequestParam(required = false) Long openBonusUserId,
            Authentication authentication,
            Model model) {
        Map<String, Object> page =
                pageService.buildPage(
                        authentication.getName(),
                        mode,
                        view,
                        departmentId,
                        directorateId,
                        divisionId,
                        subdivisionId,
                        status,
                        year,
                        month,
                        userId,
                        openBonusUserId);
        if (Boolean.TRUE.equals(page.get("admin"))) {
            page.put(
                    "savedOvertimeFilters",
                    savedFilterService.findForAdmin(authentication.getName()));
        }
        model.addAllAttributes(page);
        return "manager/overtime-review";
    }

    @PostMapping("/api/overtime/review/filters")
    @PreAuthorize("hasRole('ADMIN')")
    public String saveFilter(
            @RequestParam String name,
            @RequestParam Long departmentId,
            @RequestParam(required = false) Long directorateId,
            @RequestParam(required = false) Long divisionId,
            @RequestParam(required = false) Long subdivisionId,
            @RequestParam(required = false) OvertimeStatus status,
            @RequestParam Integer year,
            @RequestParam Integer month,
            @RequestParam(defaultValue = "matrix") String view,
            Authentication authentication,
            RedirectAttributes redirectAttributes) {
        try {
            savedFilterService.save(
                    authentication.getName(),
                    name,
                    departmentId,
                    directorateId,
                    divisionId,
                    subdivisionId,
                    status,
                    year,
                    month);
            redirectAttributes.addFlashAttribute("savedFilterSuccess", "Фільтр збережено");
        } catch (RuntimeException exception) {
            redirectAttributes.addFlashAttribute("savedFilterError", exception.getMessage());
        }
        return reviewRedirect(
                departmentId,
                directorateId,
                divisionId,
                subdivisionId,
                status,
                year,
                month,
                view,
                "division",
                null);
    }

    @PostMapping("/api/overtime/review/approve")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    public String approve(
            @RequestParam Long overtimeId,
            @RequestParam(required = false) String comment,
            @RequestParam(defaultValue = "division") String mode,
            @RequestParam(defaultValue = "matrix") String view,
            @RequestParam(required = false) Long departmentId,
            @RequestParam(required = false) Long directorateId,
            @RequestParam(required = false) Long divisionId,
            @RequestParam(required = false) Long subdivisionId,
            @RequestParam(required = false) OvertimeStatus status,
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) Integer month,
            Authentication authentication) {
        overtimeService.approve(overtimeId, comment, authentication.getName());
        return reviewRedirect(
                departmentId,
                directorateId,
                divisionId,
                subdivisionId,
                status,
                year,
                month,
                view,
                mode,
                userId);
    }

    @PostMapping("/api/overtime/review/approve-all")
    @PreAuthorize("hasRole('ADMIN')")
    public String approveAll(
            @RequestParam(required = false) Long departmentId,
            @RequestParam(required = false) Long directorateId,
            @RequestParam(required = false) Long divisionId,
            @RequestParam(required = false) Long subdivisionId,
            @RequestParam(required = false) OvertimeStatus status,
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) Integer month,
            @RequestParam(defaultValue = "matrix") String view,
            Authentication authentication) {
        var page =
                pageService.buildPage(
                        authentication.getName(),
                        "division",
                        view,
                        departmentId,
                        directorateId,
                        divisionId,
                        subdivisionId,
                        status,
                        year,
                        month,
                        null,
                        null);
        @SuppressWarnings("unchecked")
        var overtimes =
                (java.util.List<Overtime>)
                        page.getOrDefault("filteredOvertimes", java.util.List.of());
        overtimeService.approveAll(
                overtimes.stream().map(Overtime::getId).toList(),
                "Погоджено адміністратором масово",
                authentication.getName());
        return reviewRedirect(
                departmentId,
                directorateId,
                divisionId,
                subdivisionId,
                status,
                year,
                month,
                view,
                "division",
                null);
    }

    @PostMapping("/api/overtime/review/reject")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    public String reject(
            @RequestParam Long overtimeId,
            @RequestParam String comment,
            @RequestParam(defaultValue = "division") String mode,
            @RequestParam(defaultValue = "matrix") String view,
            @RequestParam(required = false) Long departmentId,
            @RequestParam(required = false) Long directorateId,
            @RequestParam(required = false) Long divisionId,
            @RequestParam(required = false) Long subdivisionId,
            @RequestParam(required = false) OvertimeStatus status,
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) Integer month,
            Authentication authentication) {
        overtimeService.reject(overtimeId, comment, authentication.getName());
        return reviewRedirect(
                departmentId,
                directorateId,
                divisionId,
                subdivisionId,
                status,
                year,
                month,
                view,
                mode,
                userId);
    }

    private String reviewRedirect(
            Long departmentId,
            Long directorateId,
            Long divisionId,
            Long subdivisionId,
            OvertimeStatus status,
            Integer year,
            Integer month,
            String view,
            String mode,
            Long userId) {
        var parameters = new java.util.ArrayList<String>();
        if (departmentId != null) parameters.add("departmentId=" + departmentId);
        if (directorateId != null) parameters.add("directorateId=" + directorateId);
        if (divisionId != null) parameters.add("divisionId=" + divisionId);
        if (subdivisionId != null) parameters.add("subdivisionId=" + subdivisionId);
        if (status != null) parameters.add("status=" + status);
        if (year != null) parameters.add("year=" + year);
        if (month != null) parameters.add("month=" + month);
        parameters.add("mode=" + ("employee".equals(mode) ? "employee" : "division"));
        if (userId != null) parameters.add("userId=" + userId);
        parameters.add("view=" + ("summary".equals(view) ? "summary" : "matrix"));
        return "redirect:/api/overtime/review?" + String.join("&", parameters);
    }
}
