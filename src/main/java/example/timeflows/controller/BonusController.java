package example.timeflows.controller;

import example.timeflows.controller.dto.MonthOption;
import example.timeflows.model.*;
import example.timeflows.service.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.List;
import java.util.Map;

@Controller
@PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
public class BonusController {
    private final BonusService bonusService; private final UserService userService; private final DepartmentService departmentService; private final DivisionService divisionService;
    public BonusController(BonusService bonusService, UserService userService, DepartmentService departmentService, DivisionService divisionService) { this.bonusService=bonusService; this.userService=userService; this.departmentService=departmentService; this.divisionService=divisionService; }

    @GetMapping("/api/bonuses")
    public String page(@RequestParam(required=false) Integer year, @RequestParam(required=false) Integer month,
                       @RequestParam(required=false) Long departmentId, @RequestParam(required=false) Long divisionId,
                       @RequestParam(required=false) BonusStatus status, Authentication auth, Model model) {
        User current=userService.findByEmail(auth.getName()); YearMonth selected=year==null||month==null?YearMonth.now():YearMonth.of(year,month);
        boolean admin=current.getRoles().contains(Role.ADMIN);
        Long effectiveDepartmentId=admin?departmentId:current.getDivision().getDepartment().getId();
        Long effectiveDivisionId=admin?divisionId:current.getDivision().getId();
        List<Bonus> bonuses=effectiveDivisionId!=null?bonusService.findDivisionMonth(effectiveDivisionId,selected):bonusService.findMonth(selected);
        if(effectiveDepartmentId!=null) bonuses=bonuses.stream().filter(b->b.getUser().getDivision().getDepartment().getId().equals(effectiveDepartmentId)).toList();
        if(status!=null) bonuses=bonuses.stream().filter(b->b.getStatus()==status).toList();
        model.addAttribute("currentUser",current); model.addAttribute("selectedMonth",selected);
        model.addAttribute("months", monthOptions());
        model.addAttribute("bonuses",bonuses);
        model.addAttribute("categories", bonusService.findCategories());
        model.addAttribute("users",admin?(effectiveDivisionId!=null?userService.findActiveUsersByDivision(effectiveDivisionId):effectiveDepartmentId!=null?userService.findActiveUsersByDepartment(effectiveDepartmentId):userService.findActiveUsers()):userService.findActiveUsersByDivision(current.getDivision().getId()));
        model.addAttribute("departments",admin?departmentService.findAll():List.of(current.getDivision().getDepartment()));
        model.addAttribute("divisions",admin?(effectiveDepartmentId==null?List.of():divisionService.findByDepartment(effectiveDepartmentId)):List.of(current.getDivision()));
        model.addAttribute("selectedDepartmentId",effectiveDepartmentId); model.addAttribute("selectedDivisionId",effectiveDivisionId); model.addAttribute("selectedStatus",status);
        model.addAttribute("admin",admin); return "manager/bonuses";
    }

    private List<MonthOption> monthOptions() {
        return List.of(
                new MonthOption(1, "Січень"), new MonthOption(2, "Лютий"),
                new MonthOption(3, "Березень"), new MonthOption(4, "Квітень"),
                new MonthOption(5, "Травень"), new MonthOption(6, "Червень"),
                new MonthOption(7, "Липень"), new MonthOption(8, "Серпень"),
                new MonthOption(9, "Вересень"), new MonthOption(10, "Жовтень"),
                new MonthOption(11, "Листопад"), new MonthOption(12, "Грудень")
        );
    }

    @GetMapping("/api/bonus-categories") @ResponseBody
    public List<Map<String,Object>> categories(){return bonusService.findCategories().stream().map(c->Map.<String,Object>of("id",c.getId(),"name",c.getName())).toList();}
    @GetMapping("/api/bonuses/{id}/details") @ResponseBody
    public Map<String,Object> details(@PathVariable Long id,Authentication auth){assertCanEdit(auth,id);Bonus b=bonusService.find(id);return Map.of("id",b.getId(),"categoryId",b.getCategory().getId(),"category",b.getCategory().getName());}

    @PostMapping("/api/bonuses")
    public String create(@RequestParam Long userId,@RequestParam Long categoryId,@RequestParam BigDecimal amount,@RequestParam(required=false) String description,@RequestParam(required=false) String returnTo,Authentication auth,RedirectAttributes ra) {
        User current=userService.findByEmail(auth.getName()); assertCanManage(current,userService.findById(userId));
        try { bonusService.create(userId,categoryId,amount,description,auth.getName()); ra.addFlashAttribute("success","Бонус створено та відправлено на погодження"); }
        catch(IllegalArgumentException e){ra.addFlashAttribute("bonusError",e.getMessage());} return "review".equals(returnTo) ? "redirect:/api/overtime/review?mode=division&openBonusUserId="+userId : redirect(returnTo);
    }
    @PostMapping("/api/bonuses/{id}/update")
    public String update(@PathVariable Long id,@RequestParam Long categoryId,@RequestParam BigDecimal amount,@RequestParam(required=false) String description,@RequestParam(required=false) String returnTo,Authentication auth){ assertCanEdit(auth,id); bonusService.update(id,categoryId,amount,description,true); return redirect(returnTo); }
    @PostMapping("/api/bonuses/{id}/category")
    public String updateCategory(@PathVariable Long id,@RequestParam Long categoryId,Authentication auth){assertCanEdit(auth,id);Bonus b=bonusService.find(id);bonusService.update(id,categoryId,b.getAmount(),b.getDescription(),true);return "redirect:/api/bonuses";}
    @PostMapping("/api/bonuses/{id}/amount")
    public String updateAmount(@PathVariable Long id,@RequestParam BigDecimal amount,Authentication auth){assertCanEdit(auth,id);Bonus b=bonusService.find(id);bonusService.update(id,b.getCategory().getId(),amount,b.getDescription(),true);return "redirect:/api/bonuses";}
    @PostMapping("/api/bonuses/{id}/description")
    public String updateDescription(@PathVariable Long id,@RequestParam(required=false) String description,Authentication auth){assertCanEdit(auth,id);Bonus b=bonusService.find(id);bonusService.update(id,b.getCategory().getId(),b.getAmount(),description,true);return "redirect:/api/bonuses";}
    @PostMapping("/api/bonuses/{id}/delete")
    public String delete(@PathVariable Long id,@RequestParam(required=false) String returnTo,Authentication auth){ assertCanEdit(auth,id); bonusService.delete(id,true); return redirect(returnTo); }
    @PostMapping("/api/bonuses/{id}/approve") @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    public String approve(@PathVariable Long id,@RequestParam(required=false) String comment,@RequestParam(required=false) String returnTo,Authentication auth){ assertCanEdit(auth,id); bonusService.decide(id,BonusStatus.APPROVED,comment); return redirect(returnTo); }
    @PostMapping("/api/bonuses/{id}/reject") @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    public String reject(@PathVariable Long id,@RequestParam(required=false) String comment,@RequestParam(required=false) String returnTo,Authentication auth){ assertCanEdit(auth,id); bonusService.decide(id,BonusStatus.REJECTED,comment); return redirect(returnTo); }
    @PostMapping("/api/bonus-categories") @PreAuthorize("hasRole('ADMIN')")
    public String createCategory(@RequestParam String name, RedirectAttributes ra){try{bonusService.createCategory(name);}catch(IllegalArgumentException e){ra.addFlashAttribute("bonusError",e.getMessage());}return "redirect:/api/bonuses";}
    @PostMapping("/api/bonus-categories/{id}/update") @PreAuthorize("hasRole('ADMIN')")
    public String updateCategory(@PathVariable Long id,@RequestParam String name,RedirectAttributes ra){try{bonusService.updateCategory(id,name);}catch(IllegalArgumentException e){ra.addFlashAttribute("bonusError",e.getMessage());}return "redirect:/api/bonuses";}
    @PostMapping("/api/bonus-categories/{id}/delete") @PreAuthorize("hasRole('ADMIN')")
    public String deleteCategory(@PathVariable Long id,RedirectAttributes ra){try{bonusService.deleteCategory(id);}catch(IllegalArgumentException e){ra.addFlashAttribute("bonusError",e.getMessage());}return "redirect:/api/bonuses";}
    private void assertCanEdit(Authentication auth,Long id){User c=userService.findByEmail(auth.getName());assertCanManage(c,bonusService.find(id).getUser());}
    private void assertCanManage(User current,User target){if(!current.getRoles().contains(Role.ADMIN)&&!current.getDivision().getId().equals(target.getDivision().getId()))throw new IllegalArgumentException("Керівник може працювати лише зі своїм підвідділом");}
    private String redirect(String returnTo){
        if ("summary".equals(returnTo)) return "redirect:/api/overtime/review?mode=division&view=summary";
        return "review".equals(returnTo)?"redirect:/api/overtime/review?mode=division":"redirect:/api/bonuses";
    }
}
