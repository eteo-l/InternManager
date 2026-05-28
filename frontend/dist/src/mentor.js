let records = [];
let activeFilter = "all";

const tokenForm = document.querySelector("#tokenForm");
const mentorTokenInput = document.querySelector("#mentorToken");
const authShell = document.querySelector("#authShell");
const mentorShell = document.querySelector("#mentorShell");
const mentorAccessLayout = document.querySelector("#mentorAccessLayout");
const mentorForm = document.querySelector("#mentorForm");
const mentorEditor = document.querySelector("#mentorEditor");
const tableBody = document.querySelector("#internTableBody");
const emptyState = document.querySelector("#emptyState");
const searchInput = document.querySelector("#searchInput");
const campusFilter = document.querySelector("#campusFilter");
const exportBtn = document.querySelector("#exportBtn");
const resetBtn = document.querySelector("#resetBtn");
const cancelMentorEditBtn = document.querySelector("#cancelMentorEditBtn");
const showToast = createToast();

function lockMentorAccess() {
  authShell.hidden = false;
  mentorAccessLayout.classList.remove("authenticated");
  mentorShell.classList.add("locked");
  mentorShell.setAttribute("aria-hidden", "true");
}

function unlockMentorAccess() {
  authShell.hidden = true;
  mentorAccessLayout.classList.add("authenticated");
  mentorShell.classList.remove("locked");
  mentorShell.removeAttribute("aria-hidden");
  reloadRecords();
}

async function handleTokenSubmit(event) {
  event.preventDefault();

  try {
    await apiFetch("/mentor/auth/login", {
      method: "POST",
      body: JSON.stringify({
        token: mentorTokenInput.value.trim()
      })
    });

    mentorTokenInput.value = "";
    unlockMentorAccess();
    showToast("Token 验证通过");
  } catch (error) {
    showToast(error.message || "Token 不正确");
    mentorTokenInput.select();
  }
}

function fillMentorForm(record) {
  mentorForm.elements.id.value = record.id;

  internFields.forEach((field) => {
    mentorForm.elements[field].value = record[field] || "";
  });

  mentorForm.elements.status.value = record.status;
  mentorForm.elements.accessStatus.value = record.accessStatus;
  mentorForm.elements.networkStatus.value = record.networkStatus;
}

function resetMentorForm() {
  mentorForm.reset();
  mentorForm.elements.id.value = "";
  mentorEditor.hidden = true;
}

function renderMetrics() {
  const approved = records.filter((record) => record.status === "approved").length;
  const rejected = records.filter((record) => record.status === "rejected").length;
  const departments = new Set(records.map((record) => record.department).filter(Boolean)).size;

  document.querySelector("#totalCount").textContent = records.length;
  document.querySelector("#approvedCount").textContent = approved;
  document.querySelector("#rejectedCount").textContent = rejected;
  document.querySelector("#departmentCount").textContent = departments;
}

function renderCampusFilter() {
  const selected = campusFilter.value;
  const campuses = [...new Set(records.map((record) => record.campus).filter(Boolean))];

  campusFilter.innerHTML = '<option value="all">全部园区</option>';
  campuses.forEach((campus) => {
    const option = document.createElement("option");
    option.value = campus;
    option.textContent = campus;
    campusFilter.append(option);
  });

  campusFilter.value = campuses.includes(selected) ? selected : "all";
}

function getFilteredRecords() {
  const keyword = searchInput.value.trim().toLowerCase();
  const campus = campusFilter.value;

  return records.filter((record) => {
    const matchesStatus = activeFilter === "all" || record.status === activeFilter;
    const matchesCampus = campus === "all" || record.campus === campus;
    const searchableText = [
      record.name,
      record.school,
      record.grade,
      record.department,
      record.campus,
      record.mentor,
      record.startDate,
      record.endDate,
      record.accessStatus,
      record.networkStatus
    ]
      .join(" ")
      .toLowerCase();

    return matchesStatus && matchesCampus && searchableText.includes(keyword);
  });
}

function renderActionButtons(record) {
  const isApproved = record.status === "approved";
  const approveClass = isApproved ? "table-action success locked" : "table-action success";
  const rejectClass = isApproved ? "table-action danger locked" : "table-action danger";

  return `
    <div class="row-actions">
      <button class="table-action" type="button" data-action="edit" data-id="${record.id}">编辑</button>
      <button class="${approveClass}" type="button" data-action="approve" data-id="${record.id}" ${isApproved ? "disabled" : ""}>${isApproved ? "已确认" : "确认"}</button>
      <button class="${rejectClass}" type="button" data-action="reject" data-id="${record.id}" ${isApproved ? "disabled" : ""}>打回</button>
    </div>
  `;
}

function renderTable() {
  const filteredRecords = getFilteredRecords();
  tableBody.innerHTML = "";

  filteredRecords.forEach((record) => {
    const row = document.createElement("tr");
    row.innerHTML = `
      <td class="person-cell">
        <strong>${escapeHtml(record.name)}</strong>
      </td>
      <td>${escapeHtml(record.gender)}</td>
      <td>
        <strong>${escapeHtml(record.school)}</strong>
        <span class="subtle">${escapeHtml(record.grade)}</span>
      </td>
      <td>
        <strong>${formatDate(record.startDate)}</strong>
        <span class="subtle">至 ${formatDate(record.endDate)}</span>
      </td>
      <td>
        <strong>${escapeHtml(record.department)}</strong>
        <span class="subtle">${escapeHtml(record.campus)}</span>
      </td>
      <td>${escapeHtml(record.mentor)}</td>
      <td><span class="badge ${getStatusBadgeClass(record.accessStatus)}">${getResourceStatusLabel(record.accessStatus)}</span></td>
      <td><span class="badge ${getStatusBadgeClass(record.networkStatus)}">${getResourceStatusLabel(record.networkStatus)}</span></td>
      <td><span class="badge ${getStatusBadgeClass(record.status)}">${getFormStatusLabel(record.status)}</span></td>
      <td>${renderActionButtons(record)}</td>
    `;
    tableBody.append(row);
  });

  emptyState.hidden = filteredRecords.length > 0;
}

function render() {
  renderMetrics();
  renderCampusFilter();
  renderTable();
}

async function reloadRecords() {
  try {
    records = await apiFetch("/mentor/interns");
    render();
  } catch (error) {
    records = [];
    lockMentorAccess();
    if (error.message) {
      showToast(error.message);
    }
  }
}

function editRecord(id) {
  const record = records.find((item) => item.id === id);

  if (!record) {
    return;
  }

  fillMentorForm(record);
  mentorEditor.hidden = false;
  mentorEditor.scrollIntoView({ behavior: "smooth", block: "start" });
  showToast("已载入该实习生信息");
}

async function saveMentorRecord(event) {
  event.preventDefault();

  const id = mentorForm.elements.id.value;
  const data = getInternFormData(mentorForm);
  const errorMessage = validateRecord(data);

  if (errorMessage) {
    showToast(errorMessage);
    return;
  }

  try {
    await apiFetch(`/mentor/interns/${id}`, {
      method: "PUT",
      body: JSON.stringify({
        status: mentorForm.elements.status.value,
        accessStatus: mentorForm.elements.accessStatus.value,
        networkStatus: mentorForm.elements.networkStatus.value,
        intern: data
      })
    });
    resetMentorForm();
    await reloadRecords();
    showToast("Mentor 修改已保存");
  } catch (error) {
    showToast(error.message || "保存失败");
  }
}

function approveRecord(id) {
  apiFetch(`/mentor/interns/${id}/approve`, {
    method: "POST"
  })
    .then(() => reloadRecords())
    .then(() => {
      showToast("已确认该实习生信息");
    })
    .catch((error) => {
      showToast(error.message || "确认失败");
    });
}

function rejectRecord(id) {
  apiFetch(`/mentor/interns/${id}`, {
    method: "DELETE"
  })
    .then(() => {
      if (mentorForm.elements.id.value === id) {
        resetMentorForm();
      }
      return reloadRecords();
    })
    .then(() => {
      showToast("已打回并移除该实习生记录");
    })
    .catch((error) => {
      showToast(error.message || "打回失败");
    });
}

function exportRecords() {
  const data = JSON.stringify(records, null, 2);
  const blob = new Blob([data], { type: "application/json;charset=utf-8" });
  const url = URL.createObjectURL(blob);
  const link = document.createElement("a");

  link.href = url;
  link.download = `intern-records-${new Date().toISOString().slice(0, 10)}.json`;
  link.click();
  URL.revokeObjectURL(url);
  showToast("数据已导出为 JSON");
}

function resetAllRecords() {
  if (!confirm("确定清空所有本地演示数据吗？")) {
    return;
  }

  apiFetch("/mentor/interns", {
    method: "DELETE"
  })
    .then(() => {
      resetMentorForm();
      return reloadRecords();
    })
    .then(() => {
      showToast("本地数据已清空");
    })
    .catch((error) => {
      showToast(error.message || "清空失败");
    });
}

tokenForm.addEventListener("submit", handleTokenSubmit);
mentorForm.addEventListener("submit", saveMentorRecord);
cancelMentorEditBtn.addEventListener("click", resetMentorForm);
searchInput.addEventListener("input", renderTable);
campusFilter.addEventListener("change", renderTable);
exportBtn.addEventListener("click", exportRecords);
resetBtn.addEventListener("click", resetAllRecords);

document.querySelectorAll(".segment").forEach((button) => {
  button.addEventListener("click", () => {
    document.querySelectorAll(".segment").forEach((item) => item.classList.remove("active"));
    button.classList.add("active");
    activeFilter = button.dataset.filter;
    renderTable();
  });
});

tableBody.addEventListener("click", (event) => {
  const button = event.target.closest("button");

  if (!button || button.disabled) {
    return;
  }

  const { action, id } = button.dataset;

  if (action === "edit") {
    editRecord(id);
  }

  if (action === "approve") {
    approveRecord(id);
  }

  if (action === "reject") {
    rejectRecord(id);
  }
});

apiFetch("/mentor/auth/session")
  .then((session) => {
    if (session.authenticated) {
      unlockMentorAccess();
    } else {
      lockMentorAccess();
    }
  })
  .catch(() => {
    lockMentorAccess();
  });
