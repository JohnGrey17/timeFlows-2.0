package example.timeflows.controller;

import example.timeflows.model.Division;
import example.timeflows.service.DepartmentService;
import example.timeflows.service.DirectorateService;
import example.timeflows.service.DivisionService;
import example.timeflows.service.SubdivisionService;
import example.timeflows.service.UserService;
import java.util.Comparator;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class CompanyStructureController {

    private final DepartmentService departmentService;
    private final DirectorateService directorateService;
    private final DivisionService divisionService;
    private final SubdivisionService subdivisionService;
    private final UserService userService;

    public CompanyStructureController(
            DepartmentService departmentService,
            DirectorateService directorateService,
            DivisionService divisionService,
            SubdivisionService subdivisionService,
            UserService userService) {
        this.departmentService = departmentService;
        this.directorateService = directorateService;
        this.divisionService = divisionService;
        this.subdivisionService = subdivisionService;
        this.userService = userService;
    }

    @GetMapping("/api/company-structure")
    @PreAuthorize("!hasAnyRole('ADMIN','SYS_ADMIN','ABSOLUT')")
    public String page(Authentication authentication, Model model) {
        List<Division> divisions =
                divisionService.findAll().stream()
                        .sorted(
                                Comparator.comparing(
                                        CompanyStructureController::divisionPath,
                                        String.CASE_INSENSITIVE_ORDER))
                        .toList();
        model.addAttribute("currentUser", userService.findByEmail(authentication.getName()));
        model.addAttribute("departments", departmentService.findAll());
        model.addAttribute("directorates", directorateService.findAll());
        model.addAttribute("divisions", divisions);
        model.addAttribute("subdivisions", subdivisionService.findAll());
        model.addAttribute("activePage", "company-structure");
        return "company-structure";
    }

    private static String divisionPath(Division division) {
        String department = division.getDepartment().getName();
        return division.getDirectorate() == null
                ? department + " / " + division.getName()
                : department
                        + " / "
                        + division.getDirectorate().getName()
                        + " / "
                        + division.getName();
    }
}
