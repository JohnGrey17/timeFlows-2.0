const organizationTreeStateKey = "timeflows.organization.expanded";
const treeDetails = Array.from(document.querySelectorAll("details[data-tree-key]"));

const saveOrganizationTreeState = () => {
  const expanded = treeDetails.filter((details) => details.open).map((details) => details.dataset.treeKey);
  sessionStorage.setItem(organizationTreeStateKey, JSON.stringify(expanded));
};

try {
  const expanded = new Set(JSON.parse(sessionStorage.getItem(organizationTreeStateKey) || "[]"));
  treeDetails.forEach((details) => {
    details.open = expanded.has(details.dataset.treeKey);
    details.addEventListener("toggle", saveOrganizationTreeState);
  });
  document.querySelectorAll("form").forEach((form) => {
    form.addEventListener("submit", saveOrganizationTreeState);
  });
} catch (error) {
  sessionStorage.removeItem(organizationTreeStateKey);
}
