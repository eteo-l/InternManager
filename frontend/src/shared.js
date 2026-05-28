function normalizeBaseUrl(value) {
  return String(value || "").trim().replace(/\/+$/, "");
}

function getRuntimeConfigValue(key) {
  if (typeof window === "undefined" || typeof window.__APP_CONFIG__ !== "object" || window.__APP_CONFIG__ === null) {
    return "";
  }

  return String(window.__APP_CONFIG__[key] || "").trim();
}

const API_BASE_URL = normalizeBaseUrl(getRuntimeConfigValue("apiBaseUrl")) || "/api";

const internFields = [
  "name",
  "grade",
  "gender",
  "school",
  "startDate",
  "endDate",
  "department",
  "campus",
  "mentor",
  "note"
];

const formStatusLabels = {
  pending: "待处理",
  approved: "已确认",
  rejected: "已打回"
};

const resourceStatusLabels = {
  unopened: "未开通",
  opened: "已开通",
  disabled: "不开通",
  pending: "未开通",
  rejected: "不开通"
};

const employmentStatusLabels = {
  active: "在职",
  left: "已离职"
};

function getInternFormData(form) {
  return internFields.reduce((data, field) => {
    const element = form.elements[field];
    data[field] = element.value.trim();
    return data;
  }, {});
}

function validateRecord(data) {
  if (!data.startDate || !data.endDate) {
    return "请填写实习开始时间和实习结束时间";
  }

  if (new Date(data.endDate) < new Date(data.startDate)) {
    return "实习结束时间不能早于开始时间";
  }

  return "";
}

function escapeHtml(value) {
  const replacements = {
    "&": "&amp;",
    "<": "&lt;",
    ">": "&gt;",
    '"': "&quot;",
    "'": "&#39;"
  };

  return String(value || "").replace(/[&<>"']/g, (char) => replacements[char]);
}

function formatDate(value) {
  return value ? value.replaceAll("-", ".") : "未填写";
}

function getStatusBadgeClass(status) {
  if (status === "approved" || status === "opened") {
    return "approved";
  }

  if (status === "disabled" || status === "rejected") {
    return "rejected";
  }

  return "pending";
}

function getFormStatusLabel(status) {
  return formStatusLabels[status] || formStatusLabels.pending;
}

function getResourceStatusLabel(status) {
  return resourceStatusLabels[status] || resourceStatusLabels.pending;
}

function getEmploymentStatusLabel(status) {
  return employmentStatusLabels[status] || employmentStatusLabels.active;
}

function createToast() {
  const toast = document.querySelector("#toast");

  return function showToast(message) {
    toast.textContent = message;
    toast.classList.add("show");

    window.clearTimeout(showToast.timer);
    showToast.timer = window.setTimeout(() => {
      toast.classList.remove("show");
    }, 2200);
  };
}

async function apiFetch(path, options = {}) {
  const response = await fetch(`${API_BASE_URL}${path}`, {
    credentials: "include",
    headers: {
      "Content-Type": "application/json",
      ...(options.headers || {})
    },
    ...options
  });

  if (response.status === 204) {
    return null;
  }

  const data = await response.json().catch(() => ({}));

  if (!response.ok) {
    throw new Error(data.message || "请求失败");
  }

  return data;
}
