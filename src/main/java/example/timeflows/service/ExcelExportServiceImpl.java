package example.timeflows.service;

import example.timeflows.exception.DivisionException;
import example.timeflows.model.Bonus;
import example.timeflows.model.BonusStatus;
import example.timeflows.model.Department;
import example.timeflows.model.Division;
import example.timeflows.model.Overtime;
import example.timeflows.model.OvertimeStatus;
import example.timeflows.model.User;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.DayOfWeek;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ExcelExportServiceImpl implements ExcelExportService {
    private static final DateTimeFormatter MONTH_FORMAT = DateTimeFormatter.ofPattern("MM.yyyy");
    private final DepartmentService departmentService;
    private final DivisionService divisionService;
    private final UserService userService;
    private final OvertimeService overtimeService;
    private final BonusService bonusService;

    public ExcelExportServiceImpl(
            DepartmentService departmentService,
            DivisionService divisionService,
            UserService userService,
            OvertimeService overtimeService,
            BonusService bonusService) {
        this.departmentService = departmentService;
        this.divisionService = divisionService;
        this.userService = userService;
        this.overtimeService = overtimeService;
        this.bonusService = bonusService;
    }

    @Override
    @Transactional(readOnly = true)
    public ExcelExportResult export(
            Long departmentId, Long divisionId, YearMonth from, YearMonth to) {
        validatePeriod(from, to);
        Department department = departmentService.findById(departmentId);
        Division division = divisionId == null ? null : divisionService.findById(divisionId);
        if (division != null && !division.getDepartment().getId().equals(departmentId)) {
            throw new DivisionException("Підвідділ не належить вибраному департаменту");
        }
        List<User> users =
                division == null
                        ? userService.findActiveUsersByDepartment(departmentId)
                        : userService.findActiveUsersByDivision(divisionId);
        try (Workbook workbook = new XSSFWorkbook();
                ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            for (YearMonth month = from; !month.isAfter(to); month = month.plusMonths(1)) {
                addMonthSheets(workbook, month, department, division, users);
            }
            workbook.write(output);
            return new ExcelExportResult(
                    filename(department, division, from, to), output.toByteArray());
        } catch (IOException exception) {
            throw new IllegalStateException("Не вдалося сформувати Excel-файл", exception);
        }
    }

    private void addMonthSheets(
            Workbook workbook,
            YearMonth month,
            Department department,
            Division division,
            List<User> users) {
        List<Overtime> approvedOvertimes =
                (division == null
                                ? overtimeService.findDepartmentMonth(department.getId(), month)
                                : overtimeService.findDivisionMonth(division.getId(), month))
                        .stream()
                                .filter(overtime -> overtime.getStatus() == OvertimeStatus.APPROVED)
                                .toList();
        var userIds = users.stream().map(User::getId).collect(Collectors.toSet());
        List<Bonus> approvedBonuses =
                bonusService.findMonth(month).stream()
                        .filter(bonus -> userIds.contains(bonus.getUser().getId()))
                        .filter(bonus -> bonus.getStatus() == BonusStatus.APPROVED)
                        .toList();
        addCalendarSheet(
                workbook, month, department, division, users, approvedOvertimes, approvedBonuses);
        addFinancialSheet(
                workbook, month, department, division, users, approvedOvertimes, approvedBonuses);
    }

    private void addCalendarSheet(
            Workbook workbook,
            YearMonth month,
            Department department,
            Division division,
            List<User> users,
            List<Overtime> overtimes,
            List<Bonus> bonuses) {
        Sheet sheet = workbook.createSheet(month.format(MONTH_FORMAT) + " Календар");
        CellStyle header = headerStyle(workbook);
        int row = 0;
        row = title(sheet, row, "Календар перепрацювань — " + month.format(MONTH_FORMAT), header);
        row = metadata(sheet, row, department, division);
        Row calendarHeader = sheet.createRow(row++);
        int column = 0;
        header(calendarHeader, column++, "Працівник", header);
        for (int day = 1; day <= month.lengthOfMonth(); day++) {
            header(
                    calendarHeader,
                    column++,
                    day + " " + weekday(month.atDay(day).getDayOfWeek()),
                    header);
        }
        header(calendarHeader, column++, "Погоджені бонуси", header);
        header(calendarHeader, column++, "Базова ставка", header);
        header(calendarHeader, column++, "Години перепрацювань", header);
        header(calendarHeader, column++, "Сума перепрацювань", header);
        header(calendarHeader, column, "Сума до сплати", header);
        Map<Long, Map<java.time.LocalDate, Overtime>> byUser =
                overtimes.stream()
                        .collect(
                                Collectors.groupingBy(
                                        overtime -> overtime.getUser().getId(),
                                        Collectors.toMap(
                                                Overtime::getWorkDate, Function.identity())));
        Map<Long, BigDecimal> bonusTotals = bonusTotals(bonuses);
        int workingHours = workingHours(month);
        for (User user : users.stream().sorted(Comparator.comparing(User::getEmail)).toList()) {
            Row excelRow = sheet.createRow(row++);
            excelRow.createCell(0).setCellValue(displayName(user));
            Map<java.time.LocalDate, Overtime> userOvertimes =
                    byUser.getOrDefault(user.getId(), Map.of());
            for (int day = 1; day <= month.lengthOfMonth(); day++) {
                Overtime overtime = userOvertimes.get(month.atDay(day));
                if (overtime != null) excelRow.createCell(day).setCellValue(overtime.getHours());
            }
            BigDecimal salary = salary(user);
            BigDecimal hours = approvedHours(userOvertimes);
            BigDecimal overtimeAmount = overtimeAmount(salary, hours, workingHours);
            BigDecimal bonus = bonusTotals.getOrDefault(user.getId(), BigDecimal.ZERO);
            valuesFrom(
                    excelRow,
                    month.lengthOfMonth() + 1,
                    bonus,
                    salary,
                    hours,
                    overtimeAmount,
                    salary.add(overtimeAmount).add(bonus).setScale(2, RoundingMode.HALF_UP));
        }
        sheet.setColumnWidth(0, 24 * 256);
        for (int day = 1; day <= month.lengthOfMonth(); day++) sheet.setColumnWidth(day, 7 * 256);
        for (int i = month.lengthOfMonth() + 1; i <= month.lengthOfMonth() + 5; i++)
            sheet.autoSizeColumn(i);
        sheet.createFreezePane(1, 4);
    }

    private void addFinancialSheet(
            Workbook workbook,
            YearMonth month,
            Department department,
            Division division,
            List<User> users,
            List<Overtime> overtimes,
            List<Bonus> bonuses) {
        Sheet sheet = workbook.createSheet(month.format(MONTH_FORMAT) + " Фінанси");
        CellStyle header = headerStyle(workbook);
        int row = title(sheet, 0, "Фінансовий підсумок — " + month.format(MONTH_FORMAT), header);
        row = metadata(sheet, row, department, division);
        Row financeHeader = sheet.createRow(row++);
        int column = 0;
        header(financeHeader, column++, "Працівник", header);
        var categories = bonusService.findCategories();
        for (var category : categories) header(financeHeader, column++, category.getName(), header);
        header(financeHeader, column++, "Базова ставка", header);
        header(financeHeader, column++, "Сума перепрацювань", header);
        header(financeHeader, column, "Сума до сплати", header);
        Map<Long, Map<java.time.LocalDate, Overtime>> byUser =
                overtimes.stream()
                        .collect(
                                Collectors.groupingBy(
                                        overtime -> overtime.getUser().getId(),
                                        Collectors.toMap(
                                                Overtime::getWorkDate, Function.identity())));
        int workingHours = workingHours(month);
        for (User user : users.stream().sorted(Comparator.comparing(User::getEmail)).toList()) {
            Row excelRow = sheet.createRow(row++);
            excelRow.createCell(0).setCellValue(displayName(user));
            int valueColumn = 1;
            BigDecimal totalBonus = BigDecimal.ZERO;
            for (var category : categories) {
                BigDecimal categoryAmount =
                        bonuses.stream()
                                .filter(bonus -> bonus.getUser().getId().equals(user.getId()))
                                .filter(
                                        bonus ->
                                                bonus.getCategory()
                                                        .getId()
                                                        .equals(category.getId()))
                                .map(Bonus::getAmount)
                                .reduce(BigDecimal.ZERO, BigDecimal::add);
                excelRow.createCell(valueColumn++).setCellValue(categoryAmount.doubleValue());
                totalBonus = totalBonus.add(categoryAmount);
            }
            BigDecimal salary = salary(user);
            BigDecimal hours = approvedHours(byUser.getOrDefault(user.getId(), Map.of()));
            BigDecimal overtimeAmount = overtimeAmount(salary, hours, workingHours);
            valuesFrom(
                    excelRow,
                    valueColumn,
                    salary,
                    overtimeAmount,
                    salary.add(overtimeAmount).add(totalBonus).setScale(2, RoundingMode.HALF_UP));
        }
        for (int i = 0; i <= categories.size() + 3; i++) sheet.autoSizeColumn(i);
        sheet.createFreezePane(0, 4);
    }

    private Map<Long, BigDecimal> bonusTotals(List<Bonus> bonuses) {
        return bonuses.stream()
                .collect(
                        Collectors.groupingBy(
                                bonus -> bonus.getUser().getId(),
                                Collectors.reducing(
                                        BigDecimal.ZERO, Bonus::getAmount, BigDecimal::add)));
    }

    private BigDecimal salary(User user) {
        return user.getSalary() == null ? BigDecimal.ZERO : user.getSalary();
    }

    private BigDecimal approvedHours(Map<java.time.LocalDate, Overtime> overtimes) {
        return overtimes.values().stream()
                .map(overtime -> BigDecimal.valueOf(overtime.getHours()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private BigDecimal overtimeAmount(BigDecimal salary, BigDecimal hours, int workingHours) {
        return workingHours == 0
                ? BigDecimal.ZERO
                : salary.divide(BigDecimal.valueOf(workingHours), 8, RoundingMode.HALF_UP)
                        .multiply(BigDecimal.valueOf(2))
                        .multiply(hours)
                        .setScale(2, RoundingMode.HALF_UP);
    }

    private String weekday(DayOfWeek day) {
        return switch (day) {
            case MONDAY -> "пн";
            case TUESDAY -> "вт";
            case WEDNESDAY -> "ср";
            case THURSDAY -> "чт";
            case FRIDAY -> "пт";
            case SATURDAY -> "сб";
            case SUNDAY -> "нд";
        };
    }

    private void validatePeriod(YearMonth from, YearMonth to) {
        if (from == null || to == null) throw new IllegalArgumentException("Період є обов'язковим");
        if (from.isAfter(to))
            throw new IllegalArgumentException("Початок періоду не може бути після завершення");
        if (from.plusMonths(23).isBefore(to))
            throw new IllegalArgumentException("Максимальний період експорту — 24 місяці");
    }

    private int workingHours(YearMonth month) {
        return (int)
                        month.atDay(1)
                                .datesUntil(month.plusMonths(1).atDay(1))
                                .filter(
                                        date ->
                                                date.getDayOfWeek() != DayOfWeek.SATURDAY
                                                        && date.getDayOfWeek() != DayOfWeek.SUNDAY)
                                .count()
                * 8;
    }

    private int title(Sheet sheet, int rowIndex, String text, CellStyle style) {
        Row row = sheet.createRow(rowIndex++);
        Cell cell = row.createCell(0);
        cell.setCellValue(text);
        cell.setCellStyle(style);
        return rowIndex;
    }

    private int metadata(Sheet sheet, int row, Department department, Division division) {
        values(sheet.createRow(row++), "Департамент", department.getName());
        values(
                sheet.createRow(row++),
                "Підвідділ",
                division == null ? "Усі підвідділи" : division.getName());
        return row;
    }

    private void fillHeader(Row row, String[] values, CellStyle style) {
        for (int i = 0; i < values.length; i++) {
            Cell cell = row.createCell(i);
            cell.setCellValue(values[i]);
            cell.setCellStyle(style);
        }
    }

    private void header(Row row, int column, String value, CellStyle style) {
        Cell cell = row.createCell(column);
        cell.setCellValue(value);
        cell.setCellStyle(style);
    }

    private void valuesFrom(Row row, int startColumn, Object... values) {
        for (int i = 0; i < values.length; i++) {
            Cell cell = row.createCell(startColumn + i);
            Object value = values[i];
            if (value instanceof Number number) cell.setCellValue(number.doubleValue());
            else cell.setCellValue(value == null ? "" : value.toString());
        }
    }

    private void values(Row row, Object... values) {
        for (int i = 0; i < values.length; i++) {
            Cell cell = row.createCell(i);
            Object value = values[i];
            if (value instanceof Number number) cell.setCellValue(number.doubleValue());
            else cell.setCellValue(value == null ? "" : value.toString());
        }
    }

    private CellStyle headerStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        Font font = workbook.createFont();
        font.setBold(true);
        font.setColor(IndexedColors.WHITE.getIndex());
        style.setFont(font);
        style.setFillForegroundColor(IndexedColors.DARK_BLUE.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        style.setAlignment(HorizontalAlignment.CENTER);
        return style;
    }

    private String displayName(User user) {
        return ((user.getLastName() == null ? "" : user.getLastName())
                        + " "
                        + (user.getFirstName() == null ? "" : user.getFirstName()))
                .trim();
    }

    private String filename(
            Department department, Division division, YearMonth from, YearMonth to) {
        String scope = department.getName() + (division == null ? "" : " " + division.getName());
        return sanitize(
                        "вивантаження "
                                + scope
                                + " "
                                + from.format(MONTH_FORMAT)
                                + "-"
                                + to.format(MONTH_FORMAT))
                + ".xlsx";
    }

    private String sanitize(String value) {
        return value.replaceAll("[\\\\/:*?\"<>|]", "_").replaceAll("\\s+", " ").trim();
    }
}
