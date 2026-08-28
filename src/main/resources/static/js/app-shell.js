const layout = document.querySelector(".app-layout");
const toggle = document.querySelector("[data-sidebar-toggle]");
if (layout && toggle) {
    if (localStorage.getItem("timeflows.sidebar.collapsed") === "true") layout.classList.add("sidebar-collapsed");
    toggle.addEventListener("click", () => {
        layout.classList.toggle("sidebar-collapsed");
        localStorage.setItem("timeflows.sidebar.collapsed", String(layout.classList.contains("sidebar-collapsed")));
    });
}

// Keep contextual help next to the heading it explains on every module.
document.querySelectorAll(".section-title-with-help").forEach((title) => {
    const heading = title.querySelector("h1, h2");
    const tip = title.querySelector(":scope > .help-tip");
    if (heading && tip) heading.insertAdjacentElement("afterend", tip);
});

if (location.pathname === "/api/overtime") {
    const heading = document.querySelector(".topbar h1");
    if (heading && !heading.parentElement.querySelector(".help-tip")) {
        const tip = document.createElement("span");
        tip.className = "help-tip";
        tip.tabIndex = 0;
        tip.textContent = "?";
        tip.dataset.tooltip = "Тут ви можете переглядати свої перепрацювання за місяць, додавати нові записи, редагувати їх або видаляти, доки це дозволяє поточний статус.";
        heading.parentElement.classList.add("title-help-inline");
        heading.insertAdjacentElement("afterend", tip);
    }
}

// Bonus decisions use a focused dialog instead of expanding the table row.
document.querySelectorAll(".bonus-history-actions .decision-row form").forEach((form) => {
    const submit = form.querySelector("button");
    const originalInput = form.querySelector("input[name='comment']");
    if (!submit || !originalInput) return;
    const reject = submit.classList.contains("danger-button");
    const opener = document.createElement("button");
    opener.type = "button";
    opener.className = submit.className;
    opener.textContent = submit.textContent;
    opener.addEventListener("click", () => {
        const backdrop = document.createElement("div");
        backdrop.className = "modal-backdrop";
        backdrop.innerHTML = `<section class="modal-panel decision-modal"><div class="modal-header"><h2>${reject ? "Відхилити бонус" : "Погодити бонус"}</h2><button type="button" aria-label="Закрити">×</button></div><label>${reject ? "Причина відхилення" : "Коментар (необов’язково)"}<textarea maxlength="1000" ${reject ? "required" : ""}></textarea></label><div class="modal-actions"><button type="button" class="${reject ? "danger-button" : ""}">Зберегти</button></div></section>`;
        const close = () => backdrop.remove();
        backdrop.querySelector(".modal-header button").addEventListener("click", close);
        backdrop.addEventListener("click", event => { if (event.target === backdrop) close(); });
        backdrop.querySelector(".modal-actions button").addEventListener("click", () => {
            const value = backdrop.querySelector("textarea").value.trim();
            if (reject && !value) { backdrop.querySelector("textarea").reportValidity(); return; }
            originalInput.value = value;
            form.submit();
        });
        document.body.append(backdrop);
        backdrop.querySelector("textarea").focus();
    });
    // Replace only the visible submit button. Rebuilding the whole form here used to remove
    // Spring Security's hidden CSRF input and caused every bonus decision to fail with 403.
    submit.replaceWith(opener);
    originalInput.hidden = true;
});
