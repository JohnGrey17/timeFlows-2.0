package example.timeflows.controller;

import example.timeflows.model.Department;
import example.timeflows.service.DepartmentService;
import example.timeflows.service.DirectorateService;
import example.timeflows.service.DivisionService;
import example.timeflows.service.SubdivisionService;
import example.timeflows.service.UserService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@PreAuthorize("hasRole('ADMIN')")
public class OrganizationPageController {

    private final DepartmentService departmentService;
    private final DivisionService divisionService;
    private final DirectorateService directorateService;
    private final SubdivisionService subdivisionService;
    private final UserService userService;

    public OrganizationPageController(
            DepartmentService departmentService,
            DivisionService divisionService,
            DirectorateService directorateService,
            SubdivisionService subdivisionService,
            UserService userService) {
        this.departmentService = departmentService;
        this.divisionService = divisionService;
        this.directorateService = directorateService;
        this.subdivisionService = subdivisionService;
        this.userService = userService;
    }

    @GetMapping("/api/organization")
    public String page(Authentication authentication, Model model) {
        model.addAttribute("currentUser", userService.findByEmail(authentication.getName()));
        model.addAttribute("departments", departmentService.findAll());
        model.addAttribute("directorates", directorateService.findAll());
        model.addAttribute("divisions", divisionService.findAll());
        model.addAttribute("subdivisions", subdivisionService.findAll());
        model.addAttribute("activePage", "organization");
        return "admin/organization";
    }

    @PostMapping("/api/organization/directorates")
    public String createDirectorate(
            @RequestParam Long departmentId,
            @RequestParam String name,
            @RequestParam(required = false) String description,
            RedirectAttributes redirectAttributes) {
        try {
            directorateService.create(name, description, departmentId);
            redirectAttributes.addFlashAttribute("success", "Управління створено");
        } catch (RuntimeException exception) {
            redirectAttributes.addFlashAttribute("organizationError", exception.getMessage());
        }
        return "redirect:/api/organization";
    }

    @PostMapping("/api/organization/departments")
    public String createDepartment(
            @RequestParam String name,
            @RequestParam(required = false) String description,
            RedirectAttributes redirectAttributes) {
        try {
            departmentService.create(name, description);
            redirectAttributes.addFlashAttribute("success", "Департамент створено");
        } catch (RuntimeException exception) {
            redirectAttributes.addFlashAttribute("organizationError", exception.getMessage());
        }
        return "redirect:/api/organization";
    }

    @PostMapping("/api/organization/divisions")
    public String createDivision(
            @RequestParam Long directorateId,
            @RequestParam String name,
            @RequestParam(required = false) String description,
            RedirectAttributes redirectAttributes) {
        try {
            divisionService.createInDirectorate(name, description, directorateId);
            redirectAttributes.addFlashAttribute("success", "Відділ створено");
        } catch (RuntimeException exception) {
            redirectAttributes.addFlashAttribute("organizationError", exception.getMessage());
        }
        return "redirect:/api/organization";
    }

    @PostMapping("/api/organization/subdivisions")
    public String createSubdivision(
            @RequestParam Long divisionId,
            @RequestParam String name,
            @RequestParam(required = false) String description,
            RedirectAttributes redirectAttributes) {
        try {
            subdivisionService.create(name, description, divisionId);
            redirectAttributes.addFlashAttribute("success", "Підвідділ створено");
        } catch (RuntimeException exception) {
            redirectAttributes.addFlashAttribute("organizationError", exception.getMessage());
        }
        return "redirect:/api/organization";
    }

    @PostMapping("/api/organization/departments/{id}/update")
    public String updateDepartment(
            @PathVariable Long id,
            @RequestParam String name,
            @RequestParam(required = false) String description,
            RedirectAttributes redirectAttributes) {
        return organizationAction(
                () -> {
                    Department input = new Department();
                    input.setName(name);
                    input.setDescription(description);
                    departmentService.update(id, input);
                },
                "Департамент оновлено",
                redirectAttributes);
    }

    @PostMapping("/api/organization/departments/{id}/delete")
    public String deleteDepartment(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        return organizationAction(
                () -> departmentService.delete(id), "Департамент видалено", redirectAttributes);
    }

    @PostMapping("/api/organization/directorates/{id}/update")
    public String updateDirectorate(
            @PathVariable Long id,
            @RequestParam Long departmentId,
            @RequestParam String name,
            @RequestParam(required = false) String description,
            RedirectAttributes redirectAttributes) {
        return organizationAction(
                () -> directorateService.update(id, name, description, departmentId),
                "Управління оновлено",
                redirectAttributes);
    }

    @PostMapping("/api/organization/directorates/{id}/delete")
    public String deleteDirectorate(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        return organizationAction(
                () -> directorateService.delete(id), "Управління видалено", redirectAttributes);
    }

    @PostMapping("/api/organization/divisions/{id}/update")
    public String updateDivision(
            @PathVariable Long id,
            @RequestParam Long directorateId,
            @RequestParam String name,
            @RequestParam(required = false) String description,
            RedirectAttributes redirectAttributes) {
        return organizationAction(
                () -> divisionService.updateInDirectorate(id, name, description, directorateId),
                "Відділ оновлено",
                redirectAttributes);
    }

    @PostMapping("/api/organization/divisions/{id}/delete")
    public String deleteDivision(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        return organizationAction(
                () -> divisionService.delete(id), "Відділ видалено", redirectAttributes);
    }

    @PostMapping("/api/organization/subdivisions/{id}/update")
    public String updateSubdivision(
            @PathVariable Long id,
            @RequestParam Long divisionId,
            @RequestParam String name,
            @RequestParam(required = false) String description,
            RedirectAttributes redirectAttributes) {
        return organizationAction(
                () -> subdivisionService.update(id, name, description, divisionId),
                "Підвідділ оновлено",
                redirectAttributes);
    }

    @PostMapping("/api/organization/subdivisions/{id}/delete")
    public String deleteSubdivision(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        return organizationAction(
                () -> subdivisionService.delete(id), "Підвідділ видалено", redirectAttributes);
    }

    private String organizationAction(
            Runnable action, String successMessage, RedirectAttributes redirectAttributes) {
        try {
            action.run();
            redirectAttributes.addFlashAttribute("success", successMessage);
        } catch (RuntimeException exception) {
            redirectAttributes.addFlashAttribute("organizationError", exception.getMessage());
        }
        return "redirect:/api/organization";
    }
}
