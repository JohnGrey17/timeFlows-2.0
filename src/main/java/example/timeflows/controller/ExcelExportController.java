package example.timeflows.controller;

import example.timeflows.service.DepartmentService;
import example.timeflows.service.DivisionService;
import example.timeflows.service.ExcelExportService;
import example.timeflows.service.UserService;
import java.nio.charset.StandardCharsets;
import java.time.YearMonth;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@PreAuthorize("hasRole('ADMIN')")
public class ExcelExportController {
    private static final MediaType XLSX =
            MediaType.parseMediaType(
                    "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
    private final ExcelExportService excelExportService;
    private final DepartmentService departmentService;
    private final DivisionService divisionService;
    private final UserService userService;

    public ExcelExportController(
            ExcelExportService excelExportService,
            DepartmentService departmentService,
            DivisionService divisionService,
            UserService userService) {
        this.excelExportService = excelExportService;
        this.departmentService = departmentService;
        this.divisionService = divisionService;
        this.userService = userService;
    }

    @GetMapping("/api/admin/export")
    public String page(Authentication authentication, Model model) {
        model.addAttribute("currentUser", userService.findByEmail(authentication.getName()));
        model.addAttribute("departments", departmentService.findAll());
        model.addAttribute("divisions", divisionService.findAll());
        model.addAttribute("defaultFrom", YearMonth.now().minusMonths(1));
        model.addAttribute("defaultTo", YearMonth.now());
        model.addAttribute("activePage", "export");
        return "admin/excel-export";
    }

    @GetMapping("/api/admin/export/download")
    public ResponseEntity<byte[]> download(
            @RequestParam Long departmentId,
            @RequestParam(required = false) Long divisionId,
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM") YearMonth from,
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM") YearMonth to) {
        var result = excelExportService.export(departmentId, divisionId, from, to);
        ContentDisposition disposition =
                ContentDisposition.attachment()
                        .filename(result.filename(), StandardCharsets.UTF_8)
                        .build();
        return ResponseEntity.ok()
                .contentType(XLSX)
                .header(HttpHeaders.CONTENT_DISPOSITION, disposition.toString())
                .contentLength(result.content().length)
                .body(result.content());
    }
}
