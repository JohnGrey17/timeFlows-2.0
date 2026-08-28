package example.timeflows.service;

import example.timeflows.controller.dto.DivisionOvertimeRow;
import example.timeflows.model.BonusCategory;
import example.timeflows.model.BonusType;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.List;
import java.util.Map;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
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
            for (DivisionOvertimeRow value : rows) {
                Row row = sheet.createRow(rowIndex++);
                Long userId = value.user().getId();
                int valueColumn = 0;
                row.createCell(valueColumn++)
                        .setCellValue(
                                ((value.user().getLastName() == null
                                                        ? ""
                                                        : value.user().getLastName())
                                                + " "
                                                + (value.user().getFirstName() == null
                                                        ? ""
                                                        : value.user().getFirstName()))
                                        .trim());
                for (BonusCategory category : categories) {
                    number(
                            row,
                            valueColumn++,
                            categoryTotals
                                    .getOrDefault(userId, Map.of())
                                    .getOrDefault(category.getId(), BigDecimal.ZERO));
                }
                if (hasProjectManagers) {
                    number(
                            row,
                            valueColumn++,
                            typeTotals
                                    .getOrDefault(userId, Map.of())
                                    .getOrDefault(BonusType.KPI, BigDecimal.ZERO));
                }
                if (!projectManagerView) {
                    number(
                            row,
                            valueColumn++,
                            typeTotals
                                    .getOrDefault(userId, Map.of())
                                    .getOrDefault(BonusType.QUARTERLY, BigDecimal.ZERO));
                }
                number(row, valueColumn, hours.getOrDefault(userId, BigDecimal.ZERO));
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
