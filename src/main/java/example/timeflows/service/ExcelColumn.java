package example.timeflows.service;

public enum ExcelColumn {
    EMPLOYEE("Працівник"),
    EMAIL("Email"),
    DEPARTMENT("Департамент"),
    DIRECTORATE("Управління"),
    DIVISION("Відділ"),
    SUBDIVISION("Підвідділ"),
    TAGS("Теги"),
    OVERTIME_HOURS("Години перепрацювань"),
    BONUS_TOTAL("Погоджені бонуси");

    private final String label;

    ExcelColumn(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
