const reviewModal = document.getElementById("reviewOvertimeModal");
const divisionOvertimeModal = document.getElementById("divisionOvertimeModal");
const divisionOvertimeForm = divisionOvertimeModal?.querySelector("[data-division-overtime-form]");
document.querySelectorAll("[data-employee-id][data-work-date].division-overtime-create").forEach((button) => {
    button.addEventListener("click", (event) => {
        event.stopPropagation();
        divisionOvertimeForm.reset();
        divisionOvertimeForm.elements.employeeId.value = button.dataset.employeeId;
        divisionOvertimeForm.elements.workDate.value = button.dataset.workDate;
        divisionOvertimeModal.querySelector("[data-division-overtime-employee]").textContent =
            `${button.dataset.employeeName} · ${button.dataset.workDate}`;
        divisionOvertimeModal.querySelector("[data-division-overtime-error]").textContent = "";
        divisionOvertimeModal.hidden = false;
        divisionOvertimeForm.elements.hours.focus();
    });
});
document.querySelector("[data-division-overtime-close]")?.addEventListener("click", () => divisionOvertimeModal.hidden = true);
divisionOvertimeModal?.addEventListener("click", (event) => {
    if (event.target === divisionOvertimeModal) divisionOvertimeModal.hidden = true;
});
divisionOvertimeForm?.addEventListener("submit", async (event) => {
    event.preventDefault();
    const error = divisionOvertimeModal.querySelector("[data-division-overtime-error]");
    error.textContent = "";
    const employeeId = divisionOvertimeForm.elements.employeeId.value;
    const response = await fetch(`/api/overtimes/users/${employeeId}`, {
        method: "POST",
        headers: reviewJsonHeaders(),
        body: JSON.stringify({
            workDate: divisionOvertimeForm.elements.workDate.value,
            hours: Number(divisionOvertimeForm.elements.hours.value),
            description: divisionOvertimeForm.elements.description.value
        })
    });
    if (response.ok) {
        window.location.reload();
        return;
    }
    const body = await response.json().catch(() => null);
    error.textContent = body?.validationErrors
        ? Object.values(body.validationErrors)[0]
        : body?.message || `Помилка HTTP ${response.status}`;
});
function reviewJsonHeaders() {
    const headers = {"Content-Type": "application/json"};
    const csrfCookie = document.cookie.split("; ").find((cookie) => cookie.startsWith("XSRF-TOKEN="));
    if (csrfCookie) headers["X-XSRF-TOKEN"] = decodeURIComponent(csrfCookie.substring("XSRF-TOKEN=".length));
    return headers;
}
const savedFilterModal = document.getElementById("saveOvertimeFilterModal");
document.querySelector("[data-saved-filter-open]")?.addEventListener("click", () => {
    savedFilterModal.hidden = false;
    savedFilterModal.querySelector("input[name='name']")?.focus();
});
document.querySelector("[data-saved-filter-close]")?.addEventListener("click", () => savedFilterModal.hidden = true);
savedFilterModal?.addEventListener("click", (event) => {
    if (event.target === savedFilterModal) savedFilterModal.hidden = true;
});
document.querySelector("[data-saved-filter-select]")?.addEventListener("change", (event) => {
    const selectedOption = event.currentTarget.selectedOptions[0];
    if (selectedOption?.dataset.url) window.location.assign(selectedOption.dataset.url);
});
const showReviewError = (message) => {
    const backdrop = document.createElement("div");
    backdrop.className = "modal-backdrop review-error-modal";
    backdrop.innerHTML = `<section class="modal-panel decision-modal"><div class="modal-header"><h2>Не вдалося виконати дію</h2><button type="button" aria-label="Закрити">×</button></div><p class="error-message"></p><div class="modal-actions"><button type="button">Зрозуміло</button></div></section>`;
    backdrop.querySelector(".error-message").textContent = message || "Сталася помилка. Оновіть сторінку та повторіть дію.";
    const close = () => backdrop.remove();
    backdrop.querySelector(".modal-header button").addEventListener("click", close);
    backdrop.querySelector(".modal-actions button").addEventListener("click", close);
    backdrop.addEventListener("click", (event) => { if (event.target === backdrop) close(); });
    document.body.append(backdrop);
};
document.querySelectorAll(".overtime-info-trigger[data-hours]:not([data-hours=''])").forEach((cell) => cell.addEventListener("click", (event) => {
    if (event.target.closest("form")) return;
    if (!reviewModal.querySelector("[data-info-resubmission]")) {
        const details = reviewModal.querySelector(".overtime-details");
        details.insertAdjacentHTML("beforeend", "<dt>Причина повторного погодження</dt><dd data-info-resubmission></dd>");
    }
    const values = {user: cell.dataset.user, date: cell.dataset.date, hours: cell.dataset.hours, description: cell.dataset.description, status: cell.dataset.status, comment: cell.dataset.comment || "—", resubmission: cell.dataset.resubmissionReason || "—"};
    Object.entries(values).forEach(([key, value]) => { const target = reviewModal.querySelector(`[data-info-${key}]`); if (target) target.textContent = value; });
    reviewModal.querySelectorAll("[data-decision-overtime-id]").forEach((input) => input.value = cell.dataset.overtimeId);
    const admin = reviewModal.dataset.admin === "true";
    const canReview = admin ? cell.dataset.status === "APPROVED_MANAGER" : cell.dataset.status === "CHECKING";
    reviewModal.querySelector("[data-review-actions]").hidden = !canReview;
    reviewModal.hidden = false;
}));
document.querySelector("[data-review-close]")?.addEventListener("click", () => reviewModal.hidden = true);
reviewModal?.addEventListener("click", (event) => { if (event.target === reviewModal) reviewModal.hidden = true; });
reviewModal?.querySelectorAll("[data-review-actions] form").forEach((form) => form.addEventListener("submit", async (event) => {
    event.preventDefault();
    const response = await fetch(form.action, {method: "POST", body: new FormData(form)});
    if (response.ok) {
        window.location.assign(response.url || "/api/overtime/review");
        return;
    }
    let message;
    try { message = (await response.json()).message; } catch (_) { message = `Помилка HTTP ${response.status}`; }
    reviewModal.hidden = true;
    showReviewError(message);
}));
document.querySelector("[data-bulk-approve]")?.addEventListener("submit", async (event) => {
    event.preventDefault();
    if (!window.confirm("Погодити всі доступні перепрацювання у поточній вибірці?")) return;
    const form = event.currentTarget;
    const button = document.querySelector(`[form="${form.id}"]`);
    if (button) button.disabled = true;
    try {
        const response = await fetch(form.action, {method: "POST", body: new FormData(form), credentials: "same-origin"});
        if (response.ok) {
            window.location.assign(response.url || "/api/overtime/review");
            return;
        }
        let message;
        try { message = (await response.json()).message; } catch (_) { message = `Помилка HTTP ${response.status}`; }
        showReviewError(message);
    } catch (_) {
        showReviewError("Не вдалося зв’язатися із сервером. Повторіть дію.");
    } finally {
        if (button) button.disabled = false;
    }
});
let bonusCategories;
const bonusStatusLabels = {
    PENDING: "Очікує підтвердження адміністратором",
    APPROVED: "Погоджено адміністратором",
    REJECTED: "Відхилено адміністратором"
};
document.querySelectorAll("[data-bonus-modal]").forEach((button) => button.addEventListener("click", async () => {
    const bonusModal = document.getElementById(button.dataset.bonusModal);
    const createForm = bonusModal.querySelector(".bonus-create-inline");
    const projectManager = createForm?.dataset.projectManager === "true";
    if (!projectManager) bonusCategories ||= await fetch("/api/bonus-categories").then(response => response.json());
    bonusModal.querySelectorAll(".bonus-modal-item").forEach((item) => {
        const status = item.querySelector("small")?.textContent.match(/(PENDING|APPROVED|REJECTED)$/)?.[1];
        if (status) {
            item.classList.add(`bonus-status-${status.toLowerCase()}`);
            const label = item.querySelector("small");
            label.textContent = label.textContent.replace(status, bonusStatusLabels[status]);
        }
        const action = item.querySelector("form[action*='/api/bonuses/']")?.action;
        const id = action?.match(/\/api\/bonuses\/(\d+)/)?.[1];
        if (id && !item.dataset.categoryLoaded) fetch(`/api/bonuses/${id}/details`).then(r=>r.json()).then(detail=>{
            item.dataset.categoryLoaded="true";
            const description=item.querySelector("div:first-child span"); if(description) description.textContent=`${detail.category}${description.textContent.trim()?` — ${description.textContent}`:""}`;
            item.querySelectorAll("select[name='categoryId']").forEach(select=>select.value=String(detail.categoryId));
        });
    });
    bonusModal.querySelectorAll("form[action*='/api/bonuses'].bonus-create-inline").forEach((form) => {
        if (form.dataset.projectManager === "true") return;
        if (form.querySelector("[name='categoryId']")) return;
        const select = document.createElement("select"); select.name = "categoryId"; select.required = true;
        select.innerHTML = '<option value="">Оберіть категорію</option>' + bonusCategories.map(c => `<option value="${c.id}">${c.name}</option>`).join("");
        form.querySelector("[name='amount']")?.before(select);
        const description = form.querySelector("[name='description']"); if (description) { description.placeholder = "Додатковий опис (необов’язково)"; description.required = false; }
        const amount = form.querySelector("[name='amount']"); if (amount) { amount.placeholder = "Сума бонусу, грн"; amount.setAttribute("aria-label", "Сума бонусу"); }
    });
    bonusModal.hidden = false;
}));
document.querySelectorAll(".bonus-review-modal").forEach((modal) => {
    modal.querySelector("[data-bonus-close]")?.addEventListener("click", () => modal.hidden = true);
    modal.addEventListener("click", (event) => { if (event.target === modal) modal.hidden = true; });
});
document.querySelector("[data-bonus-modal][data-open='true']")?.click();

document.querySelectorAll("[data-summary-modal]").forEach((button) => button.addEventListener("click", () => {
    const modal = document.getElementById(button.dataset.summaryModal);
    if (modal) modal.hidden = false;
}));
document.querySelectorAll(".summary-detail-modal").forEach((modal) => {
    modal.querySelectorAll(".summary-detail-list article").forEach((item) => {
        const status = item.querySelector("small")?.textContent.match(/(PENDING|APPROVED|REJECTED)/)?.[1];
        if (status) {
            item.classList.add(`bonus-status-${status.toLowerCase()}`);
            const label = item.querySelector("small");
            label.textContent = label.textContent.replace(status, bonusStatusLabels[status]);
        }
    });
    modal.querySelector("[data-summary-close]")?.addEventListener("click", () => modal.hidden = true);
    modal.addEventListener("click", event => { if (event.target === modal) modal.hidden = true; });
});

document.querySelectorAll("form[data-calendar-filter] select[name='month'], form[data-calendar-filter] select[name='year']")
    .forEach((select) => select.addEventListener("change", () => select.form.requestSubmit()));
