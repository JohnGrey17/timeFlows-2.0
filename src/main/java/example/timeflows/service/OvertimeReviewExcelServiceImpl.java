package example.timeflows.service;

import example.timeflows.controller.dto.DivisionOvertimeRow;
import example.timeflows.model.BonusCategory;
import example.timeflows.model.BonusType;
import example.timeflows.model.Overtime;
import example.timeflows.model.Role;
import example.timeflows.model.User;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

@Service
public class OvertimeReviewExcelServiceImpl implements OvertimeReviewExcelService {
    @Override
    @SuppressWarnings("unchecked")
    public ExcelExportService.ExcelExportResult exportSummary(Map<String, Object> page) {
        List<DivisionOvertimeRow> rows = (List<DivisionOvertimeRow>) page.get("divisionRows");
        List<BonusCategory> categories = (List<BonusCategory>) page.get("categories");
        Map<Long, Map<Long, BigDecimal>> categoryTotals =
                (Map<Long, Map<Long, BigDecimal>>) page.get("categoryTotalsByUser");
        Map<Long, Map<BonusType, BigDecimal>> typeTotals =
                (Map<Long, Map<BonusType, BigDecimal>>) page.get("typeTotalsByUser");
        Map<Long, BigDecimal> hours = (Map<Long, BigDecimal>) page.get("overviewHoursByUser");
        List<Overtime> filteredOvertimes =
                (List<Overtime>) page.getOrDefault("filteredOvertimes", List.of());
        Set<Long> usersWithOvertime =
                filteredOvertimes.stream()
                        .map(overtime -> overtime.getUser().getId())
                        .collect(Collectors.toSet());
        YearMonth month = (YearMonth) page.get("selectedMonth");
        boolean projectManagerView = Boolean.TRUE.equals(page.get("projectManagerView"));
        boolean hasProjectManagers = Boolean.TRUE.equals(page.get("hasProjectManagers"));
        try (Workbook workbook = new XSSFWorkbook();
                ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("Підсумок " + month);
            CellStyle header = headerStyle(workbook);
            Row headerRow = sheet.createRow(0);
            int column = 0;
            cell(headerRow, column++, "Співробітник", header);
            for (BonusCategory category : categories) {
                cell(headerRow, column++, category.getName(), header);
            }
            if (hasProjectManagers) cell(headerRow, column++, "KPI", header);
            if (!projectManagerView) cell(headerRow, column++, "Квартальний бонус", header);
            cell(headerRow, column, "Години перепрацювань", header);

            int rowIndex = 1;
            List<DivisionOvertimeRow> exportRows =
                    rows.stream()
                            .filter(
                                    value ->
                                            hasData(
                                                    value.user().getId(),
                                                    categories,
                                                    categoryTotals,
                                                    typeTotals,
                                                    hours,
                                                    usersWithOvertime))
                            .sorted(
                                    Comparator.comparing(
                                                    (DivisionOvertimeRow value) ->
                                                            directorateName(value.user()),
                                                    String.CASE_INSENSITIVE_ORDER)
                                            .thenComparing(
                                                    value -> divisionName(value.user()),
                                                    String.CASE_INSENSITIVE_ORDER)
                                            .thenComparing(
                                                    value ->
                                                            value.user().getSubdivision() == null
                                                                    ? ""
                                                                    : value.user()
                                                                            .getSubdivision()
                                                                            .getName(),
                                                    String.CASE_INSENSITIVE_ORDER)
                                            .thenComparing(
                                                    value -> fullName(value.user()),
                                                    String.CASE_INSENSITIVE_ORDER))
                            .toList();
            List<DivisionOvertimeRow> directorateManagers =
                    exportRows.stream()
                            .filter(
                                    value ->
                                            value.user()
                                                    .getRoles()
                                                    .contains(Role.DIRECTORATE_MANAGER))
                            .toList();
            if (!directorateManagers.isEmpty()) {
                rowIndex = groupRow(sheet, rowIndex, column, "Керівники управлінь", header);
                for (DivisionOvertimeRow value : directorateManagers) {
                    rowIndex =
                            dataRow(
                                    sheet,
                                    rowIndex,
                                    value,
                                    categories,
                                    categoryTotals,
                                    typeTotals,
                                    hours,
                                    hasProjectManagers,
                                    projectManagerView);
                }
            }

            String currentDivision = null;
            String currentSubdivision = null;
            for (DivisionOvertimeRow value : exportRows) {
                User user = value.user();
                if (user.getRoles().contains(Role.DIRECTORATE_MANAGER)) continue;
                String division =
                        user.getDivision() == null ? "Без відділу" : user.getDivision().getName();
                if (!division.equals(currentDivision)) {
                    currentDivision = division;
                    currentSubdivision = null;
                    String divisionManager =
                            user.getDivision() == null
                                    ? ""
                                    : fullName(user.getDivision().getManager());
                    rowIndex =
                            groupRow(
                                    sheet,
                                    rowIndex,
                                    column,
                                    "Відділ: " + division + managerSuffix(divisionManager),
                                    header);
                }
                String subdivision =
                        user.getSubdivision() == null
                                ? "Без напряму"
                                : user.getSubdivision().getName();
                if (!subdivision.equals(currentSubdivision)) {
                    currentSubdivision = subdivision;
                    rowIndex = groupRow(sheet, rowIndex, column, "Напрям: " + subdivision, header);
                }
                rowIndex =
                        dataRow(
                                sheet,
                                rowIndex,
                                value,
                                categories,
                                categoryTotals,
                                typeTotals,
                                hours,
                                hasProjectManagers,
                                projectManagerView);
            }
            for (int index = 0; index <= column; index++) sheet.autoSizeColumn(index);
            sheet.createFreezePane(1, 1);
            workbook.write(output);
            return new ExcelExportService.ExcelExportResult(
                    "перевірка-перепрацювань-" + month + ".xlsx", output.toByteArray());
        } catch (IOException exception) {
            throw new IllegalStateException("Не вдалося сформувати Excel", exception);
        }
    }

    private int dataRow(
            Sheet sheet,
            int rowIndex,
            DivisionOvertimeRow value,
            List<BonusCategory> categories,
            Map<Long, Map<Long, BigDecimal>> categoryTotals,
            Map<Long, Map<BonusType, BigDecimal>> typeTotals,
            Map<Long, BigDecimal> hours,
            boolean hasProjectManagers,
            boolean projectManagerView) {
        Row row = sheet.createRow(rowIndex);
        Long userId = value.user().getId();
        int column = 0;
        row.createCell(column++).setCellValue(fullName(value.user()));
        for (BonusCategory category : categories) {
            number(
                    row,
                    column++,
                    categoryTotals
                            .getOrDefault(userId, Map.of())
                            .getOrDefault(category.getId(), BigDecimal.ZERO));
        }
        if (hasProjectManagers) {
            number(
                    row,
                    column++,
                    typeTotals
                            .getOrDefault(userId, Map.of())
                            .getOrDefault(BonusType.KPI, BigDecimal.ZERO));
        }
        if (!projectManagerView) {
            number(
                    row,
                    column++,
                    typeTotals
                            .getOrDefault(userId, Map.of())
                            .getOrDefault(BonusType.QUARTERLY, BigDecimal.ZERO));
        }
        number(row, column, hours.getOrDefault(userId, BigDecimal.ZERO));
        return rowIndex + 1;
    }

    private boolean hasData(
            Long userId,
            List<BonusCategory> categories,
            Map<Long, Map<Long, BigDecimal>> categoryTotals,
            Map<Long, Map<BonusType, BigDecimal>> typeTotals,
            Map<Long, BigDecimal> hours,
            Set<Long> usersWithOvertime) {
        boolean hasCategoryBonus =
                categories.stream()
                        .anyMatch(
                                category ->
                                        categoryTotals
                                                        .getOrDefault(userId, Map.of())
                                                        .getOrDefault(
                                                                category.getId(), BigDecimal.ZERO)
                                                        .signum()
                                                != 0);
        boolean hasTypedBonus =
                typeTotals.getOrDefault(userId, Map.of()).values().stream()
                        .anyMatch(value -> value.signum() != 0);
        return usersWithOvertime.contains(userId)
                || hasCategoryBonus
                || hasTypedBonus
                || hours.getOrDefault(userId, BigDecimal.ZERO).signum() != 0;
    }

    private int groupRow(Sheet sheet, int rowIndex, int lastColumn, String title, CellStyle style) {
        Row row = sheet.createRow(rowIndex);
        cell(row, 0, title, style);
        sheet.addMergedRegion(new CellRangeAddress(rowIndex, rowIndex, 0, lastColumn));
        return rowIndex + 1;
    }

    private String fullName(User user) {
        if (user == null) return "";
        return ((user.getLastName() == null ? "" : user.getLastName())
                        + " "
                        + (user.getFirstName() == null ? "" : user.getFirstName())
                        + " "
                        + (user.getPatronymic() == null ? "" : user.getPatronymic()))
                .trim();
    }

    private String divisionName(User user) {
        return user.getDivision() == null ? "" : user.getDivision().getName();
    }

    private String directorateName(User user) {
        return user.getDivision() == null || user.getDivision().getDirectorate() == null
                ? ""
                : user.getDivision().getDirectorate().getName();
    }

    private String managerSuffix(String manager) {
        return manager.isBlank() ? " (керівник не призначений)" : " (керівник: " + manager + ")";
    }

    private void number(Row row, int column, BigDecimal value) {
        row.createCell(column).setCellValue(value.doubleValue());
    }

    private void cell(Row row, int column, String value, CellStyle style) {
        var cell = row.createCell(column);
        cell.setCellValue(value);
        cell.setCellStyle(style);
    }

    private CellStyle headerStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        Font font = workbook.createFont();
        font.setBold(true);
        font.setColor(IndexedColors.WHITE.getIndex());
        style.setFont(font);
        style.setFillForegroundColor(IndexedColors.ORANGE.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        return style;
    }
}
