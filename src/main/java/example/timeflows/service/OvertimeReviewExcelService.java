package example.timeflows.service;

import java.util.Map;

public interface OvertimeReviewExcelService {
    ExcelExportService.ExcelExportResult exportSummary(Map<String, Object> page);
}
