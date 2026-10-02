const quarterlyForm = document.querySelector("[data-quarterly-form]");

if (quarterlyForm) {
  const storageKey = "timeflows.quarterly-bonus-form";
  const filters = Array.from(quarterlyForm.querySelectorAll("[data-quarter-filter]"));
  const recipients = Array.from(quarterlyForm.querySelectorAll(".quarterly-recipient"));
  const selectAll = quarterlyForm.querySelector("[data-quarter-select-all]");
  const addSelectedButton = quarterlyForm.querySelector("[data-quarter-add-selected]");
  const bonusEmployees = quarterlyForm.querySelector("[data-quarter-bonus-employees]");
  const bonusEmpty = quarterlyForm.querySelector("[data-quarter-bonus-empty]");
  const bonusCount = quarterlyForm.querySelector("[data-quarter-bonus-count]");
  const cartPool = quarterlyForm.querySelector("[data-quarter-cart-pool]");
  const cartCount = quarterlyForm.querySelector("[data-quarter-cart-count]");
  const perPerson = quarterlyForm.querySelector("[data-quarter-per-person]");
  const distributeButton = quarterlyForm.querySelector("[data-quarter-distribute]");
  const empty = quarterlyForm.querySelector("[data-quarter-empty]");
  const filterSummary = quarterlyForm.querySelector("[data-quarter-filter-summary]");
  const yearInput = quarterlyForm.querySelector("[data-quarter-year]");
  const quarterInput = quarterlyForm.querySelector("[data-quarter-number]");
  const poolValue = quarterlyForm.querySelector("[data-quarter-pool]");
  const stateValue = quarterlyForm.querySelector("[data-quarter-state]");
  const resetButton = quarterlyForm.querySelector("[data-quarter-reset]");
  const selectedRecipientIds = new Set();
  let currentPoolCents = null;
  const restoreState = () => {
    try {
      const state = JSON.parse(sessionStorage.getItem(storageKey) || "null");
      if (!state) return;
      yearInput.value = state.year || yearInput.value;
      quarterInput.value = state.quarter || quarterInput.value;
      filters.forEach((input) => {
        input.checked = state.filters?.includes(`${input.dataset.quarterFilter}:${input.value}`) || false;
      });
      recipients.forEach((recipient) => {
        const input = recipient.querySelector("[data-quarter-candidate]");
        input.checked = state.candidates?.includes(input.value) || false;
      });
      (state.selectedRecipients || state.recipients || []).forEach((id) => {
        if (recipients.some((recipient) => recipient.dataset.userId === id)) selectedRecipientIds.add(id);
      });
    } catch (_error) {
      sessionStorage.removeItem(storageKey);
    }
  };
  const saveState = () => {
    sessionStorage.setItem(storageKey, JSON.stringify({
      year: yearInput.value,
      quarter: quarterInput.value,
      filters: filters.filter((input) => input.checked)
        .map((input) => `${input.dataset.quarterFilter}:${input.value}`),
      candidates: recipients.map((recipient) => recipient.querySelector("[data-quarter-candidate]"))
        .filter((input) => input.checked).map((input) => input.value),
      selectedRecipients: Array.from(selectedRecipientIds)
    }));
  };
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

  const formatMoney = (cents) => (cents / 100).toFixed(2);

  const updateBonusCalculation = () => {
    const count = selectedRecipientIds.size;
    cartCount.textContent = count;
    cartPool.textContent = currentPoolCents === null ? "—" : formatMoney(currentPoolCents);
    const amountElements = Array.from(bonusEmployees.querySelectorAll("[data-quarter-recipient-amount]"));
    if (currentPoolCents === null || count === 0) {
      perPerson.textContent = "—";
      amountElements.forEach((element) => { element.textContent = "—"; });
      return;
    }
    const baseCents = Math.floor(currentPoolCents / count);
    const remainderCents = currentPoolCents - baseCents * count;
    perPerson.textContent = formatMoney(baseCents);
    amountElements.forEach((element, index) => {
      element.textContent = formatMoney(baseCents + (index < remainderCents ? 1 : 0));
    });
  };

  const renderBonusList = () => {
    bonusEmployees.replaceChildren();
    selectedRecipientIds.forEach((id) => {
      const recipient = recipients.find((item) => item.dataset.userId === id);
      if (!recipient) return;
      const row = document.createElement("article");
      row.className = "quarterly-bonus-employee";
      const details = document.createElement("div");
      const name = document.createElement("strong");
      const path = document.createElement("span");
      name.textContent = recipient.dataset.userName;
      path.textContent = recipient.dataset.userPath;
      details.append(name, path);
      const amount = document.createElement("strong");
      amount.className = "quarterly-recipient-amount";
      amount.dataset.quarterRecipientAmount = "";
      amount.textContent = "—";
      const remove = document.createElement("button");
      remove.type = "button";
      remove.className = "danger-button";
      remove.textContent = "Видалити";
      remove.addEventListener("click", () => {
        selectedRecipientIds.delete(id);
        renderBonusList();
        saveState();
      });
      const hidden = document.createElement("input");
      hidden.type = "hidden";
      hidden.name = "userIds";
      hidden.value = id;
      row.append(details, amount, remove, hidden);
      bonusEmployees.appendChild(row);
    });
    bonusCount.textContent = selectedRecipientIds.size;
    bonusEmpty.hidden = selectedRecipientIds.size > 0;
    distributeButton.disabled = selectedRecipientIds.size === 0;
    updateBonusCalculation();
  };

  const loadQuarterSummary = async () => {
    stateValue.textContent = "Завантаження…";
    resetButton.disabled = true;
    try {
      const query = new URLSearchParams({year: yearInput.value, quarter: quarterInput.value});
      const response = await fetch(`/api/bonuses/quarterly/summary?${query}`, {credentials: "same-origin"});
      if (!response.ok) throw new Error();
      const summary = await response.json();
      currentPoolCents = Math.round(Number(summary.pool) * 100);
      poolValue.textContent = summary.pool;
      stateValue.textContent = summary.isDistributed
        ? `Уже розподілено: ${summary.distributed} · отримувачів: ${summary.recipientCount}`
        : summary.pool > 0 ? "Сума доступна для розподілу" : "Погоджених KPI за цей квартал немає";
      resetButton.disabled = !summary.isDistributed;
      updateBonusCalculation();
    } catch (_error) {
      currentPoolCents = null;
      poolValue.textContent = "—";
      stateValue.textContent = "Не вдалося завантажити стан кварталу";
      updateBonusCalculation();
    }
  };

  const updateSelectAll = () => {
    const visibleCheckboxes = visibleRecipients().map((recipient) => recipient.querySelector("[data-quarter-candidate]"));
    const checked = visibleCheckboxes.filter((input) => input.checked).length;
    selectAll.checked = visibleCheckboxes.length > 0 && checked === visibleCheckboxes.length;
    selectAll.indeterminate = checked > 0 && checked < visibleCheckboxes.length;
    selectAll.disabled = visibleCheckboxes.length === 0;
    addSelectedButton.disabled = checked === 0;
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

  filters.forEach((input) => input.addEventListener("change", () => {
    applyFilters();
    saveState();
  }));
  recipients.forEach((recipient) => recipient.querySelector("[data-quarter-candidate]")
    .addEventListener("change", () => {
      updateSelectAll();
      saveState();
    }));
  selectAll.addEventListener("change", () => {
    visibleRecipients().forEach((recipient) => {
      recipient.querySelector("[data-quarter-candidate]").checked = selectAll.checked;
    });
    updateSelectAll();
    saveState();
  });
  addSelectedButton.addEventListener("click", () => {
    visibleRecipients().forEach((recipient) => {
      const input = recipient.querySelector("[data-quarter-candidate]");
      if (input.checked) selectedRecipientIds.add(recipient.dataset.userId);
      input.checked = false;
    });
    renderBonusList();
    updateSelectAll();
    saveState();
  });
  yearInput.addEventListener("change", loadQuarterSummary);
  quarterInput.addEventListener("change", loadQuarterSummary);
  resetButton.addEventListener("click", (event) => {
    if (!window.confirm("Скинути поточний розподіл квартального бонусу? Усі створені квартальні нарахування цього кварталу буде видалено.")) {
      event.preventDefault();
    }
  });
  restoreState();
  quarterlyForm.addEventListener("submit", saveState);
  renderBonusList();
  applyFilters();
  loadQuarterSummary();
}

document.querySelectorAll("[data-quarter-message-close]").forEach((button) => {
  button.addEventListener("click", () => button.closest("[data-quarter-message]")?.remove());
});
