document.querySelectorAll("[data-kpi-quarter-toggle]").forEach((button) => {
    button.addEventListener("click", () => {
        const quarter = button.dataset.kpiQuarterToggle;
        const cells = document.querySelectorAll(`[data-kpi-quarter="${quarter}"]`);
        const collapsed = !button.closest("th").classList.contains("quarter-collapsed");
        cells.forEach((cell) => cell.classList.toggle("quarter-collapsed", collapsed));
        button.querySelector("[data-kpi-quarter-icon]").textContent = collapsed ? "+" : "−";
        button.setAttribute("aria-expanded", String(!collapsed));
    });
    button.setAttribute("aria-expanded", "true");
});
