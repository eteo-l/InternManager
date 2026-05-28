const internForm = document.querySelector("#internForm");
const clearInternFormBtn = document.querySelector("#clearInternFormBtn");
const showToast = createToast();

function resetInternForm() {
  internForm.reset();
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
    resetInternForm();
    showToast("实习生信息已提交，等待 Mentor 处理");
  } catch (error) {
    showToast(error.message || "提交失败");
  }
}

internForm.addEventListener("submit", submitInternRecord);
clearInternFormBtn.addEventListener("click", resetInternForm);
