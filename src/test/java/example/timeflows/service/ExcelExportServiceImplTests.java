package example.timeflows.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import example.timeflows.model.Bonus;
import example.timeflows.model.BonusCategory;
import example.timeflows.model.BonusStatus;
import example.timeflows.model.Department;
import example.timeflows.model.Division;
import example.timeflows.model.Overtime;
import example.timeflows.model.OvertimeStatus;
import example.timeflows.model.Role;
import example.timeflows.model.User;
import java.io.ByteArrayInputStream;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ExcelExportServiceImplTests {
    @Mock private DepartmentService departmentService;
    @Mock private DivisionService divisionService;
    @Mock private UserService userService;
    @Mock private OvertimeService overtimeService;
    @Mock private BonusService bonusService;
    private ExcelExportService service;
    private Department department;
    private Division division;

    @BeforeEach
    void setUp() {
        service =
                new ExcelExportServiceImpl(
                        departmentService,
                        divisionService,
                        userService,
                        overtimeService,
                        bonusService);
        department = new Department();
        department.setId(1L);
        department.setName("Engineering");
        division = new Division();
        division.setId(2L);
        division.setName("Platform");
        division.setDepartment(department);
    }

    @Test
    void createsOneSheetPerMonthWithCalendarAndFinancialSections() throws Exception {
        User user = new User();
        user.setId(3L);
        user.setEmail("ada@vyriy.com");
        user.setFirstName("Ada");
        user.setLastName("Lovelace");
        user.setDivision(division);
        user.setSalary(new BigDecimal("40000"));
        user.setRoles(new LinkedHashSet<>(Set.of(Role.EMPLOYEE)));
        when(departmentService.findById(1L)).thenReturn(department);
        when(divisionService.findById(2L)).thenReturn(division);
        when(userService.findActiveUsersByDivision(2L)).thenReturn(List.of(user));
        for (YearMonth month : List.of(YearMonth.of(2026, 7), YearMonth.of(2026, 8))) {
            when(overtimeService.findDivisionMonth(2L, month)).thenReturn(List.of());
            when(bonusService.findMonth(month)).thenReturn(List.of());
        }

        var result = service.export(1L, 2L, YearMonth.of(2026, 7), YearMonth.of(2026, 8));

        assertThat(result.filename())
                .isEqualTo("вивантаження Engineering Platform 07.2026-08.2026.xlsx");
        try (var workbook = new XSSFWorkbook(new ByteArrayInputStream(result.content()))) {
            assertThat(workbook.getNumberOfSheets()).isEqualTo(4);
            assertThat(workbook.getSheetName(0)).isEqualTo("07.2026 Календар");
            assertThat(workbook.getSheetName(1)).isEqualTo("07.2026 Фінанси");
            assertThat(workbook.getSheetName(2)).isEqualTo("08.2026 Календар");
            assertThat(workbook.getSheetName(3)).isEqualTo("08.2026 Фінанси");
            assertThat(workbook.getSheetAt(0).getRow(0).getCell(0).getStringCellValue())
                    .contains("Календар перепрацювань");
        }
    }

    @Test
    void rejectsInvalidPeriod() {
        assertThatThrownBy(
                        () ->
                                service.export(
                                        1L, null, YearMonth.of(2026, 8), YearMonth.of(2026, 7)))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void exportsOnlyApprovedOvertimesAndBonuses() throws Exception {
        User user = new User();
        user.setId(3L);
        user.setEmail("ada@vyriy.com");
        user.setFirstName("Ada");
        user.setLastName("Lovelace");
        user.setDivision(division);
        user.setSalary(new BigDecimal("40000"));
        user.setRoles(new LinkedHashSet<>(Set.of(Role.EMPLOYEE)));
        BonusCategory category = new BonusCategory();
        category.setId(10L);
        category.setName("Квартальний бонус");
        Overtime approved = overtime(user, LocalDate.of(2026, 8, 5), OvertimeStatus.APPROVED, 2.0);
        Overtime rejected = overtime(user, LocalDate.of(2026, 8, 6), OvertimeStatus.REJECTED, 4.0);
        Bonus approvedBonus = bonus(user, category, BonusStatus.APPROVED, "100");
        Bonus pendingBonus = bonus(user, category, BonusStatus.PENDING, "200");
        YearMonth month = YearMonth.of(2026, 8);
        when(departmentService.findById(1L)).thenReturn(department);
        when(divisionService.findById(2L)).thenReturn(division);
        when(userService.findActiveUsersByDivision(2L)).thenReturn(List.of(user));
        when(overtimeService.findDivisionMonth(2L, month)).thenReturn(List.of(approved, rejected));
        when(bonusService.findMonth(month)).thenReturn(List.of(approvedBonus, pendingBonus));
        when(bonusService.findCategories()).thenReturn(List.of(category));

        var result = service.export(1L, 2L, month, month);

        try (var workbook = new XSSFWorkbook(new ByteArrayInputStream(result.content()))) {
            var calendarRow = workbook.getSheet("08.2026 Календар").getRow(4);
            assertThat(calendarRow.getCell(5).getNumericCellValue()).isEqualTo(2.0);
            assertThat(calendarRow.getCell(6)).isNull();
            var financeRow = workbook.getSheet("08.2026 Фінанси").getRow(4);
            assertThat(financeRow.getCell(1).getNumericCellValue()).isEqualTo(100.0);
        }
    }

    private Overtime overtime(User user, LocalDate date, OvertimeStatus status, double hours) {
        Overtime overtime = new Overtime();
        overtime.setUser(user);
        overtime.setWorkDate(date);
        overtime.setStatus(status);
        overtime.setHours(hours);
        overtime.setDescription("Work");
        return overtime;
    }

    private Bonus bonus(User user, BonusCategory category, BonusStatus status, String amount) {
        Bonus bonus = new Bonus();
        bonus.setUser(user);
        bonus.setCategory(category);
        bonus.setStatus(status);
        bonus.setAmount(new BigDecimal(amount));
        bonus.setDescription("Bonus");
        return bonus;
    }
}
