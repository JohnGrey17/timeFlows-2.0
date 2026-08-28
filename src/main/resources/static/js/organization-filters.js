document.querySelectorAll("[data-organization-department]").forEach((departmentSelect) => {
  const scope = departmentSelect.closest("form") || document;
  const directorateSelect = scope.querySelector("[data-organization-directorate]");
  const divisionSelect = scope.querySelector("[data-organization-division]");
  const subdivisionSelect = scope.querySelector("[data-organization-subdivision]");

  const filterOptions = (select, attribute, parentValue) => {
    if (!select) return;
    Array.from(select.options).forEach((option) => {
      option.hidden = Boolean(option.value) && option.dataset[attribute] !== parentValue;
    });
    if (select.selectedOptions[0]?.hidden) select.value = "";
  };

  const filterSubdivisions = () => {
    filterOptions(subdivisionSelect, "divisionId", divisionSelect?.value || "");
  };

  const filterDivisions = () => {
    filterOptions(divisionSelect, "directorateId", directorateSelect?.value || "");
    filterSubdivisions();
  };

  const filterDirectorates = () => {
    filterOptions(directorateSelect, "departmentId", departmentSelect.value);
    filterDivisions();
  };

  departmentSelect.addEventListener("change", filterDirectorates);
  directorateSelect?.addEventListener("change", filterDivisions);
  divisionSelect?.addEventListener("change", filterSubdivisions);

  if (departmentSelect.hasAttribute("data-auto-submit")) {
    [departmentSelect, directorateSelect, divisionSelect, subdivisionSelect]
      .filter(Boolean)
      .forEach((select) => select.addEventListener("change", () => scope.submit()));
  }

  filterDirectorates();
});

document.querySelectorAll(".organization-assignment").forEach((form) => {
  const division = form.querySelector("[data-assignment-division]");
  const subdivision = form.querySelector("[data-assignment-subdivision]");
  if (!division || !subdivision) return;
  const options = Array.from(subdivision.options);
  const filter = () => {
    options.forEach((option) => {
      option.hidden = Boolean(option.value) && option.dataset.divisionId !== division.value;
    });
    if (subdivision.selectedOptions[0]?.hidden) subdivision.value = "";
  };
  division.addEventListener("change", filter);
  filter();
});

document.querySelectorAll("[data-filter-auto-submit]").forEach((control) => {
  control.addEventListener("change", () => control.form?.requestSubmit());
});
