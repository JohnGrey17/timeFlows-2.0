package example.timeflows.service;

import java.time.YearMonth;

public interface ExcelExportService {
    ExcelExportResult export(Long departmentId, Long divisionId, YearMonth from, YearMonth to);

    record ExcelExportResult(String filename, byte[] content) {}
}
