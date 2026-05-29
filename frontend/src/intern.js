const internForm = document.querySelector("#internForm");
const clearInternFormBtn = document.querySelector("#clearInternFormBtn");
const internFormHeading = document.querySelector("#internFormHeading");
const submissionReceipt = document.querySelector("#submissionReceipt");
const submissionReceiptList = document.querySelector("#submissionReceiptList");
const closeReceiptBtn = document.querySelector("#closeReceiptBtn");
const showToast = createToast();

function resetInternForm() {
  internForm.reset();
}

function renderReceipt(data) {
  submissionReceiptList.innerHTML = [
    ["姓名", data.name],
    ["年级", data.grade],
    ["性别", data.gender],
    ["学校", data.school],
    ["开始时间", data.startDate],
    ["结束时间", data.endDate],
    ["部门", data.department],
    ["园区", data.campus],
    ["导师", data.mentor],
    ["备注", data.note || "未填写"]
  ]
    .map(([label, value]) => `
      <div class="receipt-row">
        <span>${label}</span>
        <strong>${escapeHtml(value)}</strong>
      </div>
    `)
    .join("");
}

function showReceipt(data) {
  renderReceipt(data);
  internForm.hidden = true;
  internFormHeading.hidden = true;
  submissionReceipt.hidden = false;
  submissionReceipt.scrollIntoView({ behavior: "smooth", block: "start" });
}

function closeReceipt() {
  submissionReceipt.hidden = true;
  internForm.hidden = false;
  internFormHeading.hidden = false;
  resetInternForm();
  internForm.scrollIntoView({ behavior: "smooth", block: "start" });
}

async function submitInternRecord(event) {
  event.preventDefault();

  const data = getInternFormData(internForm);
  const errorMessage = validateRecord(data);

  if (errorMessage) {
    showToast(errorMessage);
    return;
  }

  try {
    await apiFetch("/interns", {
      method: "POST",
      body: JSON.stringify(data)
    });
    showToast("提交成功");
    showReceipt(data);
  } catch (error) {
    showToast(error.message || "提交失败");
  }
}

internForm.addEventListener("submit", submitInternRecord);
clearInternFormBtn.addEventListener("click", resetInternForm);
closeReceiptBtn.addEventListener("click", closeReceipt);
