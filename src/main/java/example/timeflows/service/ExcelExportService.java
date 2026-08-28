package example.timeflows.service;

import java.time.YearMonth;
import java.util.Set;

public interface ExcelExportService {
    ExcelExportResult export(Long departmentId, Long divisionId, YearMonth from, YearMonth to);

    ExcelExportResult export(
            Long departmentId,
            Long directorateId,
            Long divisionId,
            Long subdivisionId,
            YearMonth from,
            YearMonth to);

    ExcelExportResult export(
            Long departmentId,
            Long directorateId,
            Long divisionId,
            Long subdivisionId,
            YearMonth from,
            YearMonth to,
            Set<ExcelColumn> columns);

    record ExcelExportResult(String filename, byte[] content) {}
}
