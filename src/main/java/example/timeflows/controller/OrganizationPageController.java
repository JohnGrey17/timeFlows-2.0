package example.timeflows.controller;

import example.timeflows.model.Department;
import example.timeflows.model.Division;
import example.timeflows.service.DepartmentService;
import example.timeflows.service.DivisionService;
import example.timeflows.service.UserService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@PreAuthorize("hasRole('ADMIN')")
public class OrganizationPageController {

    private final DepartmentService departmentService;
    private final DivisionService divisionService;
    private final UserService userService;

    public OrganizationPageController(DepartmentService departmentService, DivisionService divisionService, UserService userService) {
        this.departmentService = departmentService;
        this.divisionService = divisionService;
        this.userService = userService;
    }

    @GetMapping("/api/organization")
    public String page(Authentication authentication, Model model) {
        model.addAttribute("currentUser", userService.findByEmail(authentication.getName()));
        model.addAttribute("departments", departmentService.findAll());
        model.addAttribute("activePage", "organization");
        return "admin/organization";
    }

    @PostMapping("/api/organization/departments")
    public String createDepartment(@RequestParam String name, @RequestParam(required = false) String description,
                                   RedirectAttributes redirectAttributes) {
        Department department = new Department();
        department.setName(name.trim());
        department.setDescription(description == null ? null : description.trim());
        try {
            departmentService.create(department);
            redirectAttributes.addFlashAttribute("success", "Департамент створено");
        } catch (RuntimeException exception) {
            redirectAttributes.addFlashAttribute("organizationError", exception.getMessage());
        }
        return "redirect:/api/organization";
    }

    @PostMapping("/api/organization/divisions")
    public String createDivision(@RequestParam Long departmentId, @RequestParam String name,
                                 RedirectAttributes redirectAttributes) {
        Division division = new Division();
        division.setName(name.trim());
        try {
            divisionService.create(division, departmentId);
            redirectAttributes.addFlashAttribute("success", "Підвідділ створено");
        } catch (RuntimeException exception) {
            redirectAttributes.addFlashAttribute("organizationError", exception.getMessage());
        }
        return "redirect:/api/organization";
    }
}
