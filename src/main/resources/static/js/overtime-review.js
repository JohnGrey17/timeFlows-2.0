const reviewModal = document.getElementById("reviewOvertimeModal");
document.querySelectorAll(".overtime-info-trigger[data-hours]:not([data-hours=''])").forEach((cell) => cell.addEventListener("click", (event) => {
    if (event.target.closest("form")) return;
    if (!reviewModal.querySelector("[data-info-resubmission]")) {
        const details = reviewModal.querySelector(".overtime-details");
        details.insertAdjacentHTML("beforeend", "<dt>Причина повторного погодження</dt><dd data-info-resubmission></dd>");
    }
    const values = {user: cell.dataset.user, date: cell.dataset.date, hours: cell.dataset.hours, description: cell.dataset.description, status: cell.dataset.status, comment: cell.dataset.comment || "—", resubmission: cell.dataset.resubmissionReason || "—"};
    Object.entries(values).forEach(([key, value]) => { const target = reviewModal.querySelector(`[data-info-${key}]`); if (target) target.textContent = value; });
    reviewModal.querySelectorAll("[data-decision-overtime-id]").forEach((input) => input.value = cell.dataset.overtimeId);
    reviewModal.querySelector("[data-review-actions]").hidden = cell.dataset.status !== "PENDING";
    reviewModal.hidden = false;
}));
document.querySelector("[data-review-close]")?.addEventListener("click", () => reviewModal.hidden = true);
reviewModal?.addEventListener("click", (event) => { if (event.target === reviewModal) reviewModal.hidden = true; });
let bonusCategories;
document.querySelectorAll("[data-bonus-modal]").forEach((button) => button.addEventListener("click", async () => {
    const bonusModal = document.getElementById(button.dataset.bonusModal);
    bonusCategories ||= await fetch("/api/bonus-categories").then(response => response.json());
    bonusModal.querySelectorAll(".bonus-modal-item").forEach((item) => {
        const status = item.querySelector("small")?.textContent.match(/(PENDING|APPROVED|REJECTED)$/)?.[1];
        if (status) item.classList.add(`bonus-status-${status.toLowerCase()}`);
        item.querySelectorAll("form[action$='/update'], form[action$='/delete']").forEach(form => form.remove());
        const action = item.querySelector("form[action*='/api/bonuses/']")?.action;
        const id = action?.match(/\/api\/bonuses\/(\d+)/)?.[1];
        if (id && !item.dataset.categoryLoaded) fetch(`/api/bonuses/${id}/details`).then(r=>r.json()).then(detail=>{
            item.dataset.categoryLoaded="true";
            const description=item.querySelector("div:first-child span"); if(description) description.textContent=`${detail.category}${description.textContent.trim()?` — ${description.textContent}`:""}`;
            item.querySelectorAll("select[name='categoryId']").forEach(select=>select.value=String(detail.categoryId));
        });
    });
    bonusModal.querySelectorAll("form[action*='/api/bonuses']").forEach((form) => {
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
    modal.querySelector("[data-summary-close]")?.addEventListener("click", () => modal.hidden = true);
    modal.addEventListener("click", event => { if (event.target === modal) modal.hidden = true; });
});
