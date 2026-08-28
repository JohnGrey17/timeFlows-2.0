const quarterlyForm = document.querySelector("[data-quarterly-form]");

if (quarterlyForm) {
  const filters = Array.from(quarterlyForm.querySelectorAll("[data-quarter-filter]"));
  const recipients = Array.from(quarterlyForm.querySelectorAll(".quarterly-recipient"));
  const selectAll = quarterlyForm.querySelector("[data-quarter-select-all]");
  const empty = quarterlyForm.querySelector("[data-quarter-empty]");
  const filterSummary = quarterlyForm.querySelector("[data-quarter-filter-summary]");
  const yearInput = quarterlyForm.querySelector("[data-quarter-year]");
  const quarterInput = quarterlyForm.querySelector("[data-quarter-number]");
  const poolValue = quarterlyForm.querySelector("[data-quarter-pool]");
  const stateValue = quarterlyForm.querySelector("[data-quarter-state]");
  const resetButton = quarterlyForm.querySelector("[data-quarter-reset]");
  const filterTypeLabels = {
    department: "Департамент",
    directorate: "Управління",
    division: "Відділ",
    subdivision: "Підвідділ"
  };
  quarterlyForm.querySelectorAll(".quarterly-multi-filter").forEach((details) => {
    details.querySelector("summary").dataset.baseLabel = details.querySelector("summary").textContent.trim();
  });

  const selectedValues = (type) => new Set(filters
    .filter((input) => input.dataset.quarterFilter === type && input.checked)
    .map((input) => input.value));

  const visibleRecipients = () => recipients.filter((recipient) => !recipient.hidden);

  const loadQuarterSummary = async () => {
    stateValue.textContent = "Завантаження…";
    resetButton.disabled = true;
    try {
      const query = new URLSearchParams({year: yearInput.value, quarter: quarterInput.value});
      const response = await fetch(`/api/bonuses/quarterly/summary?${query}`, {credentials: "same-origin"});
      if (!response.ok) throw new Error();
      const summary = await response.json();
      poolValue.textContent = summary.pool;
      stateValue.textContent = summary.isDistributed
        ? `Уже розподілено: ${summary.distributed} · отримувачів: ${summary.recipientCount}`
        : summary.pool > 0 ? "Сума доступна для розподілу" : "Погоджених KPI за цей квартал немає";
      resetButton.disabled = !summary.isDistributed;
    } catch (_error) {
      poolValue.textContent = "—";
      stateValue.textContent = "Не вдалося завантажити стан кварталу";
    }
  };

  const updateSelectAll = () => {
    const visibleCheckboxes = visibleRecipients().map((recipient) => recipient.querySelector("input[name='userIds']"));
    const checked = visibleCheckboxes.filter((input) => input.checked).length;
    selectAll.checked = visibleCheckboxes.length > 0 && checked === visibleCheckboxes.length;
    selectAll.indeterminate = checked > 0 && checked < visibleCheckboxes.length;
    selectAll.disabled = visibleCheckboxes.length === 0;
  };

  const applyFilters = () => {
    const selected = {
      department: selectedValues("department"),
      directorate: selectedValues("directorate"),
      division: selectedValues("division"),
      subdivision: selectedValues("subdivision")
    };
    recipients.forEach((recipient) => {
      recipient.hidden = Object.entries(selected).some(([type, values]) =>
        values.size > 0 && !values.has(recipient.dataset[`${type}Id`] || ""));
    });
    filterSummary.replaceChildren();
    const selectedInputs = filters.filter((input) => input.checked);
    if (selectedInputs.length === 0) {
      const message = document.createElement("span");
      message.textContent = "Організаційні фільтри не вибрані — показано всіх доступних співробітників.";
      filterSummary.appendChild(message);
    } else {
      selectedInputs.forEach((input) => {
        const chip = document.createElement("button");
        chip.type = "button";
        chip.className = "quarterly-filter-chip";
        const name = input.closest("label").querySelector("span").textContent.trim();
        chip.textContent = `${filterTypeLabels[input.dataset.quarterFilter]}: ${name} ×`;
        chip.addEventListener("click", () => {
          input.checked = false;
          applyFilters();
        });
        filterSummary.appendChild(chip);
      });
    }
    quarterlyForm.querySelectorAll(".quarterly-multi-filter").forEach((details) => {
      const summary = details.querySelector("summary");
      const count = details.querySelectorAll("input[data-quarter-filter]:checked").length;
      summary.textContent = count > 0 ? `${summary.dataset.baseLabel} (${count})` : summary.dataset.baseLabel;
    });
    empty.hidden = visibleRecipients().length > 0;
    updateSelectAll();
  };

  filters.forEach((input) => input.addEventListener("change", applyFilters));
  recipients.forEach((recipient) => recipient.querySelector("input[name='userIds']")
    .addEventListener("change", updateSelectAll));
  selectAll.addEventListener("change", () => {
    visibleRecipients().forEach((recipient) => {
      recipient.querySelector("input[name='userIds']").checked = selectAll.checked;
    });
    updateSelectAll();
  });
  yearInput.addEventListener("change", loadQuarterSummary);
  quarterInput.addEventListener("change", loadQuarterSummary);
  resetButton.addEventListener("click", (event) => {
    if (!window.confirm("Скинути поточний розподіл квартального бонусу? Усі створені квартальні нарахування цього кварталу буде видалено.")) {
      event.preventDefault();
    }
  });
  applyFilters();
  loadQuarterSummary();
}

document.querySelectorAll("[data-quarter-message-close]").forEach((button) => {
  button.addEventListener("click", () => button.closest("[data-quarter-message]")?.remove());
});
