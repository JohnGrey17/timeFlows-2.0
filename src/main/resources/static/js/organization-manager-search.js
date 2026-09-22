document.querySelectorAll("[data-manager-assignment]").forEach((form) => {
    const search = form.querySelector("[data-manager-search]");
    const select = form.querySelector("[data-manager-select]");
    if (!search || !select) return;

    const employeeOptions = Array.from(select.options).filter((option) => option.value);
    const normalize = (value) => value.trim().toLocaleLowerCase("uk-UA");
    const results = document.createElement("div");
    results.className = "manager-search-options";
    results.setAttribute("role", "listbox");
    results.hidden = true;
    search.parentElement.append(results);
    search.setAttribute("role", "combobox");
    search.setAttribute("aria-autocomplete", "list");
    search.setAttribute("aria-expanded", "false");

    const closeResults = () => {
        results.hidden = true;
        search.setAttribute("aria-expanded", "false");
    };

    const choose = (option) => {
        select.value = option.value;
        search.value = option.textContent.trim();
        search.setCustomValidity("");
        closeResults();
    };

    const renderResults = () => {
        const query = normalize(search.value);
        const matches = employeeOptions.filter((option) =>
            normalize(option.textContent).includes(query)
        );
        results.replaceChildren();

        matches.forEach((option) => {
            const item = document.createElement("button");
            item.type = "button";
            item.className = "manager-search-option";
            item.setAttribute("role", "option");
            item.textContent = option.textContent.trim();
            item.addEventListener("mousedown", (event) => event.preventDefault());
            item.addEventListener("click", () => choose(option));
            results.append(item);
        });

        if (!matches.length) {
            const empty = document.createElement("span");
            empty.className = "manager-search-empty";
            empty.textContent = "Співробітників не знайдено";
            results.append(empty);
        }
        results.hidden = false;
        search.setAttribute("aria-expanded", "true");
    };

    const selected = employeeOptions.find((option) => option.selected);
    if (selected) search.value = selected.textContent.trim();

    search.addEventListener("input", () => {
        select.value = "";
        search.setCustomValidity("");
        renderResults();
    });
    search.addEventListener("focus", () => {
        search.select();
        renderResults();
    });
    search.addEventListener("keydown", (event) => {
        const first = results.querySelector("button");
        if (event.key === "ArrowDown" && first) {
            event.preventDefault();
            first.focus();
        }
        if (event.key === "Escape") closeResults();
    });
    search.addEventListener("blur", closeResults);
    form.addEventListener("submit", (event) => {
        if (select.value) return;
        event.preventDefault();
        search.setCustomValidity("Оберіть співробітника зі списку");
        search.reportValidity();
    });
});
