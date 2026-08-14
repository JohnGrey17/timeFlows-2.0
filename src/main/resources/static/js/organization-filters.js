document.querySelectorAll("[data-organization-department]").forEach((departmentSelect) => {
    const scope = departmentSelect.closest("form") || document;
    const divisionSelect = scope.querySelector("[data-organization-division]");
    if (!divisionSelect) return;
    const options = Array.from(divisionSelect.options);
    const filter = () => {
        const departmentId = departmentSelect.value;
        options.forEach((option) => {
            option.hidden = Boolean(option.value) && option.dataset.departmentId !== departmentId;
        });
        if (divisionSelect.selectedOptions[0]?.hidden) divisionSelect.value = "";
    };
    departmentSelect.addEventListener("change", filter);
    if (departmentSelect.hasAttribute("data-auto-submit")) {
        departmentSelect.addEventListener("change", () => scope.submit());
        divisionSelect.addEventListener("change", () => scope.submit());
    }
    filter();
});
