const modal = document.getElementById("overtimeModal");
const form = document.getElementById("overtimeForm");
const overtimeId = document.getElementById("overtimeId");
const workDate = document.getElementById("workDate");
const displayDate = document.getElementById("displayDate");
const hours = document.getElementById("hours");
const description = document.getElementById("description");
const modalError = document.getElementById("modalError");
const statusNote = document.getElementById("statusNote");
const deleteButton = document.getElementById("deleteOvertime");
const saveButton = document.getElementById("saveOvertime");
const resubmitButton = document.getElementById("resubmitOvertime");
const resubmissionGroup = document.getElementById("resubmissionGroup");
const resubmissionReason = document.getElementById("resubmissionReason");

document.querySelectorAll(".calendar-day").forEach((day) => {
    day.addEventListener("click", () => openOvertimeModal(day));
});

document.querySelectorAll("[data-close-modal]").forEach((button) => {
    button.addEventListener("click", closeModal);
});

form.addEventListener("submit", async (event) => {
    event.preventDefault();
    modalError.textContent = "";

    const id = overtimeId.value;
    const response = await fetch(id ? `/api/overtimes/${id}` : "/api/overtimes", {
        method: id ? "PUT" : "POST",
        headers: mutationHeaders(),
        body: JSON.stringify({
            workDate: workDate.value,
            hours: Number(hours.value),
            description: description.value
        })
    });

    await handleMutationResponse(response);
});

resubmitButton.addEventListener("click", async () => {
    modalError.textContent = "";
    const id = overtimeId.value;
    const response = await fetch(`/api/overtimes/${id}/resubmit`, {
        method: "POST",
        headers: mutationHeaders(),
        body: JSON.stringify({
            workDate: workDate.value,
            hours: Number(hours.value),
            description: description.value,
            resubmissionReason: resubmissionReason.value
        })
    });
    await handleMutationResponse(response);
});

deleteButton.addEventListener("click", async () => {
    modalError.textContent = "";
    const id = overtimeId.value;
    if (!id) {
        closeModal();
        return;
    }

    const response = await fetch(`/api/overtimes/${id}`, {
        method: "DELETE",
        headers: mutationHeaders(false)
    });
    await handleMutationResponse(response);
});

function openOvertimeModal(day) {
    if (day.classList.contains("muted-day")) {
        return;
    }

    const status = day.dataset.status;
    const rejected = status === "REJECTED";
    const locked = status === "APPROVED" || rejected;

    overtimeId.value = day.dataset.id || "";
    workDate.value = day.dataset.date;
    displayDate.value = day.dataset.date;
    hours.value = day.dataset.hours || "";
    description.value = day.dataset.description || "";
    resubmissionReason.value = "";
    modalError.textContent = "";
    statusNote.textContent = buildStatusText(status, day.dataset.managerComment);

    hours.disabled = locked && !rejected;
    description.disabled = locked && !rejected;
    saveButton.hidden = locked;
    deleteButton.hidden = locked || !overtimeId.value;
    resubmitButton.hidden = !rejected;
    resubmissionGroup.hidden = !rejected;
    modal.hidden = false;
}

function closeModal() {
    modal.hidden = true;
}

function buildStatusText(status, managerComment) {
    if (status === "APPROVED") {
        return "Погоджено керівником.";
    }
    if (status === "REJECTED") {
        return managerComment ? `Відхилено. Причина: ${managerComment}` : "Відхилено керівником.";
    }
    if (status === "PENDING") {
        return "Очікує погодження керівника.";
    }
    return "";
}

async function handleMutationResponse(response) {
    if (response.ok) {
        window.location.reload();
        return;
    }

    const error = await response.json().catch(() => null);
    const validationMessage = error?.validationErrors
        ? Object.values(error.validationErrors)[0]
        : null;
    if (validationMessage || error?.message) {
        modalError.textContent = validationMessage || error.message;
        return;
    }
    if (response.status === 403) {
        modalError.textContent = "Сторінка застаріла або немає дозволу. Оновіть сторінку та повторіть дію.";
        return;
    }
    modalError.textContent = `Не вдалося виконати дію (HTTP ${response.status})`;
}

function mutationHeaders(includeJson = true) {
    const headers = {};
    const csrfCookie = document.cookie
        .split("; ")
        .find((cookie) => cookie.startsWith("XSRF-TOKEN="));

    if (includeJson) {
        headers["Content-Type"] = "application/json";
    }
    if (csrfCookie) {
        headers["X-XSRF-TOKEN"] = decodeURIComponent(csrfCookie.substring("XSRF-TOKEN=".length));
    }
    return headers;
}
