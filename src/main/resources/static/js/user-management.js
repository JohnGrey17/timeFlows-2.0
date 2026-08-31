const showUserManagementError = (message, title = "Не вдалося виконати дію") => {
  const backdrop = document.createElement("div");
  backdrop.className = "modal-backdrop review-error-modal";
  backdrop.innerHTML = `<section class="modal-panel decision-modal"><div class="modal-header"><h2></h2><button type="button" aria-label="Закрити">×</button></div><p class="error-message"></p><div class="modal-actions"><button type="button">Зрозуміло</button></div></section>`;
  backdrop.querySelector("h2").textContent = title;
  backdrop.querySelector(".error-message").textContent = message || "Сталася помилка. Повторіть дію.";
  const close = () => backdrop.remove();
  backdrop.querySelector(".modal-header button").addEventListener("click", close);
  backdrop.querySelector(".modal-actions button").addEventListener("click", close);
  backdrop.addEventListener("click", (event) => { if (event.target === backdrop) close(); });
  document.body.appendChild(backdrop);
};

document.querySelectorAll("[data-user-management-action]").forEach((form) => {
  form.addEventListener("submit", async (event) => {
    event.preventDefault();
    const submitButton = form.querySelector("button[type='submit']");
    if (submitButton) submitButton.disabled = true;
    try {
      const response = await fetch(form.action, {
        method: "POST",
        body: new FormData(form),
        credentials: "same-origin",
        headers: { "X-Requested-With": "XMLHttpRequest" },
        redirect: "follow"
      });
      if (response.ok) {
        window.location.reload();
        return;
      }
      const error = await response.json().catch(() => null);
      showUserManagementError(error?.message || `Не вдалося виконати дію (HTTP ${response.status})`, form.dataset.errorTitle);
    } catch (_error) {
      showUserManagementError("Не вдалося зв’язатися із сервером. Перевірте з’єднання та повторіть дію.", form.dataset.errorTitle);
    } finally {
      if (submitButton) submitButton.disabled = false;
    }
  });
});
