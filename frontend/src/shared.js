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
const CLIENT_TRANSFER_KEY_B64 =
  getRuntimeConfigValue("clientTransferKeyB64") || "Q2xpZW50VHJhbnNmZXJLZXkyMDI2RGVtb1ZhbHVlISE=";

const internFields = [
  "name",
  "phone",
  "idNumber",
  "grade",
  "gender",
  "emergencyPhone",
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

function normalizePhone(value) {
  return value.replace(/\D/g, "");
}

function getInternFormData(form) {
  return internFields.reduce((data, field) => {
    const element = form.elements[field];
    const value = element.value.trim();
    data[field] = field.includes("Phone") || field === "phone" ? normalizePhone(value) : value;
    return data;
  }, {});
}

function validateRecord(data) {
  if (!/^1\d{10}$/.test(data.phone)) {
    return "请填写正确的实习生手机号";
  }

  if (!/^1\d{10}$/.test(data.emergencyPhone)) {
    return "请填写正确的紧急联系人手机号";
  }

  if (!/^[0-9Xx]{15,18}$/.test(data.idNumber)) {
    return "身份证号格式不正确";
  }

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

function decodeBase64ToBytes(base64) {
  const raw = atob(base64);
  return Uint8Array.from(raw, (char) => char.charCodeAt(0));
}

let hasWarnedAboutTransportCryptoFallback = false;

function getBrowserCrypto() {
  const webCrypto = globalThis.crypto;

  if (!webCrypto || !webCrypto.subtle || typeof webCrypto.getRandomValues !== "function") {
    return null;
  }

  return webCrypto;
}

function warnTransportCryptoFallback() {
  if (hasWarnedAboutTransportCryptoFallback) {
    return;
  }

  hasWarnedAboutTransportCryptoFallback = true;
  console.warn("Web Crypto API is unavailable in the current context, falling back to plaintext transport.");
}

async function encryptClientSensitiveField(value) {
  const raw = String(value || "").trim();
  if (!raw) {
    return raw;
  }

  const webCrypto = getBrowserCrypto();
  if (!webCrypto) {
    warnTransportCryptoFallback();
    return raw;
  }

  try {
    const key = await webCrypto.subtle.importKey(
      "raw",
      decodeBase64ToBytes(CLIENT_TRANSFER_KEY_B64),
      { name: "AES-GCM" },
      false,
      ["encrypt"]
    );

    const iv = webCrypto.getRandomValues(new Uint8Array(12));
    const encrypted = await webCrypto.subtle.encrypt(
      { name: "AES-GCM", iv },
      key,
      new TextEncoder().encode(raw)
    );

    const merged = new Uint8Array(iv.length + encrypted.byteLength);
    merged.set(iv, 0);
    merged.set(new Uint8Array(encrypted), iv.length);

    let text = "";
    merged.forEach((byte) => {
      text += String.fromCharCode(byte);
    });

    return "client::" + btoa(text);
  } catch (error) {
    warnTransportCryptoFallback();
    return raw;
  }
}

async function encryptSensitiveFieldsForTransport(data) {
  return {
    ...data,
    phone: await encryptClientSensitiveField(data.phone),
    idNumber: await encryptClientSensitiveField(data.idNumber),
    emergencyPhone: await encryptClientSensitiveField(data.emergencyPhone)
  };
}
