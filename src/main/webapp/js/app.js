(function () {
  const apiBase = (() => {
    const path = window.location.pathname.replace(/\/index\.html$/, "").replace(/\/$/, "");
    return path + "/api";
  })();

  const state = {
    user: null,
    trips: [],
    selectedTrip: null,
    members: [],
    expenses: [],
    isLogin: true,
    expenseDraft: null,
    splitType: null,
    splitEqually: false,
    selectedSplitMembers: [],
    serverSettlements: [],
  };

  const els = {
    authView: document.getElementById("auth-view"),
    appView: document.getElementById("app-view"),
    logoutBtn: document.getElementById("logout-btn"),
    authTitle: document.getElementById("auth-title"),
    authError: document.getElementById("auth-error"),
    authForm: document.getElementById("auth-form"),
    authEmail: document.getElementById("auth-email"),
    authUsername: document.getElementById("auth-username"),
    authPassword: document.getElementById("auth-password"),
    authSubmit: document.getElementById("auth-submit"),
    authToggle: document.getElementById("auth-toggle"),
    tripList: document.getElementById("trip-list"),
    tripError: document.getElementById("trip-error"),
    tripTitle: document.getElementById("trip-title"),
    tripEmpty: document.getElementById("trip-empty"),
    tripContent: document.getElementById("trip-content"),
    settlements: document.getElementById("settlements"),
    expenseList: document.getElementById("expense-list"),
    overlay: document.getElementById("modal-overlay"),
    modal: document.getElementById("modal"),
    dashboardView: document.getElementById("dashboard-view"),
    summaryView: document.getElementById("summary-view"),
    summaryBox: document.getElementById("summary-box"),
  };

  async function api(path, options) {
    options = options || {};
    const response = await fetch(apiBase + path, {
      credentials: "same-origin",
      ...options,
      headers: { "Content-Type": "application/json", ...(options.headers || {}) },
    });
    const text = await response.text();
    let data = null;
    try {
      data = text ? JSON.parse(text) : null;
    } catch (e) {
      data = { error: text };
    }
    if (!response.ok) {
      const message = (data && (data.error || data.message)) || "Request failed";
      throw new Error(message);
    }
    return data;
  }

  function showAuth(isLogin) {
    state.isLogin = isLogin;
    els.authView.classList.remove("hidden");
    els.appView.classList.add("hidden");
    els.logoutBtn.classList.add("hidden");
    els.authTitle.textContent = isLogin ? "Login" : "Sign Up";
    els.authSubmit.textContent = isLogin ? "Login" : "Sign Up";
    els.authToggle.textContent = isLogin
      ? "Don't have an account? Sign Up"
      : "Already have an account? Login";
    els.authEmail.classList.toggle("hidden", isLogin);
    els.authEmail.required = !isLogin;
    els.authPassword.minLength = isLogin ? 1 : 8;
    els.authError.textContent = "";
  }

  function showApp() {
    els.authView.classList.add("hidden");
    els.appView.classList.remove("hidden");
    els.logoutBtn.classList.remove("hidden");
  }

  function round2(n) {
    return Math.round((Number(n) || 0) * 100) / 100;
  }

  function calculateSettlements(members, expenses) {
    const balances = new Map();
    members.forEach((m) => balances.set(m.id, 0));
    expenses.forEach((expense) => {
      (expense.users || []).forEach((user) => {
        balances.set(user.id, (balances.get(user.id) || 0) + Number(user.lent || 0) - Number(user.borrow || 0));
      });
    });
    const getsBack = [];
    const paysBack = [];
    const transactions = [];
    balances.forEach((amount, id) => {
      const name = members.find((m) => m.id === id)?.name || "Unknown";
      if (Math.abs(amount) < 1) {
        transactions.push({ from: name, to: "No one", amount: "0.00" });
      } else if (amount < 0) {
        paysBack.push({ id, name, amount: Math.abs(amount) });
      } else if (amount > 0) {
        getsBack.push({ id, name, amount });
      }
    });
    getsBack.sort((a, b) => b.amount - a.amount);
    paysBack.sort((a, b) => b.amount - a.amount);
    const threshold = 0.1;
    while (getsBack.length && paysBack.length) {
      const creditor = getsBack[0];
      const debtor = paysBack[0];
      if (creditor.amount <= threshold) {
        getsBack.shift();
        continue;
      }
      const settleAmount = Math.min(creditor.amount, debtor.amount);
      transactions.push({
        from: debtor.name,
        to: creditor.name,
        amount: settleAmount.toFixed(2),
      });
      creditor.amount -= settleAmount;
      debtor.amount -= settleAmount;
      if (creditor.amount <= threshold) getsBack.shift();
      if (debtor.amount <= threshold) paysBack.shift();
    }
    return transactions;
  }

  function renderTrips() {
    const addRow = `
      <li class="li1 add-trip-row">
        <input id="new-trip-name" class="input-i1" type="text" placeholder="Enter a Place." maxlength="255" />
        <button type="button" id="add-trip-btn">Add</button>
      </li>`;
    const items = state.trips
      .map((trip) => {
        const selected = state.selectedTrip && state.selectedTrip.id === trip.id;
        return `<li class="li1">
          <button class="button-b1" data-trip-id="${trip.id}">
            <p><strong>${escapeHtml(trip.name)}  </strong><span class="trip-meta">${escapeHtml(trip.start_date || "")}</span></p>
            <p>${selected ? "-" : "+"}</p>
          </button>
        </li>`;
      })
      .join("");
    els.tripList.innerHTML = items + addRow;
    document.getElementById("add-trip-btn").addEventListener("click", addTrip);
    els.tripList.querySelectorAll("[data-trip-id]").forEach((btn) => {
      btn.addEventListener("click", () => selectTrip(Number(btn.getAttribute("data-trip-id"))));
    });
  }

  function renderTripDetail() {
    if (!state.selectedTrip) {
      els.tripTitle.textContent = "Dashboard";
      els.tripEmpty.classList.remove("hidden");
      els.tripContent.classList.add("hidden");
      return;
    }
    els.tripTitle.textContent = state.selectedTrip.name;
    els.tripEmpty.classList.add("hidden");
    els.tripContent.classList.remove("hidden");
    renderSettlements();
    renderExpenses();
  }

  function realPayments(settlements) {
    return (settlements || []).filter((s) => {
      const amount = Number(s.amount);
      return s.to !== "No one" && Number.isFinite(amount) && amount >= 0.1;
    });
  }

  function renderSettlements() {
    const settlements = realPayments(
      state.serverSettlements && state.serverSettlements.length
        ? state.serverSettlements
        : calculateSettlements(state.members, state.expenses)
    );
    if (!settlements.length) {
      els.settlements.innerHTML = "<p>All expenses are settled!</p>";
      return;
    }
    els.settlements.innerHTML =
      "<ul>" +
      settlements
        .map((s) => `<li>${escapeHtml(s.from)} pays ₹${escapeHtml(String(s.amount))} to ${escapeHtml(s.to)}</li>`)
        .join("") +
      "</ul>";
  }

  function renderExpenses() {
    els.expenseList.innerHTML = state.expenses
      .map((entry, index) => {
        return `<li class="expense-item li1">
          <button class="expense-button" data-expense-index="${index}">${escapeHtml(entry.note || "Expense")}</button>
          <div class="expense-details hidden" id="expense-detail-${index}"></div>
        </li>`;
      })
      .join("");
    els.expenseList.querySelectorAll(".expense-button").forEach((btn) => {
      btn.addEventListener("click", () => toggleExpense(Number(btn.getAttribute("data-expense-index"))));
    });
  }

  function toggleExpense(index) {
    const entry = state.expenses[index];
    const detail = document.getElementById("expense-detail-" + index);
    const open = !detail.classList.contains("hidden") && detail.innerHTML !== "";
    document.querySelectorAll(".expense-details").forEach((el) => {
      el.classList.add("hidden");
      el.innerHTML = "";
    });
    document.querySelectorAll(".expense-button").forEach((el) => el.classList.remove("hidden"));
    if (open) {
      return;
    }
    const paidBy = (entry.users || []).filter((u) => Number(u.lent) > 0);
    const splitTo = (entry.users || []).filter((u) => Number(u.borrow) > 0);
    detail.innerHTML = `<h3>${escapeHtml(entry.note)}</h3>
      ${paidBy.map((p) => `<p><strong>${escapeHtml(p.name)}</strong> paid ₹${escapeHtml(String(p.lent))}.</p>`).join("")}
      <p><strong>Split to:</strong> ${splitTo
        .map((r) => `<strong>${escapeHtml(r.name)}</strong> (₹${escapeHtml(String(r.borrow))})`)
        .join(", ")}</p>
      <button type="button" class="close-button" data-close="${index}">Done</button>`;
    detail.classList.remove("hidden");
    els.expenseList.querySelector(`[data-expense-index="${index}"]`).classList.add("hidden");
    detail.querySelector(".close-button").addEventListener("click", () => toggleExpense(index));
  }

  function escapeHtml(value) {
    return String(value)
      .replace(/&/g, "&amp;")
      .replace(/</g, "&lt;")
      .replace(/>/g, "&gt;")
      .replace(/"/g, "&quot;");
  }

  function setTripError(message) {
    if (els.tripError) {
      els.tripError.textContent = message || "";
    }
  }

  async function loadTrips() {
    setTripError("");
    const data = await api("/trips");
    state.trips = Array.isArray(data) ? data : data.trips || [];
    renderTrips();
    renderTripDetail();
  }

  async function selectTrip(id, options) {
    const allowToggle = !options || options.toggle !== false;
    if (allowToggle && state.selectedTrip && state.selectedTrip.id === id) {
      state.selectedTrip = null;
      state.members = [];
      state.expenses = [];
      state.serverSettlements = [];
      renderTrips();
      renderTripDetail();
      return;
    }
    setTripError("");
    try {
      state.selectedTrip = state.trips.find((t) => t.id === id);
      const [members, expenses] = await Promise.all([
        api("/trips/" + id + "/users"),
        api("/trips/" + id + "/transactions"),
      ]);
      state.members = Array.isArray(members) ? members : members.users || [];
      if (Array.isArray(expenses)) {
        state.expenses = expenses;
        state.serverSettlements = [];
      } else {
        state.expenses = (expenses && expenses.transactions) || [];
        state.serverSettlements = (expenses && expenses.settlements) || [];
      }
      renderTrips();
      renderTripDetail();
    } catch (e) {
      setTripError(e.message);
    }
  }

  async function addTrip() {
    const input = document.getElementById("new-trip-name");
    const name = input.value.trim();
    if (!name) return;
    setTripError("");
    try {
      const data = await api("/trips", { method: "POST", body: JSON.stringify({ name }) });
      const trip = data.trip;
      if (!trip || trip.id == null) {
        throw new Error("Trip was created but the server did not return it.");
      }
      state.trips.push(trip);
      input.value = "";
      await selectTrip(trip.id, { toggle: false });
    } catch (e) {
      setTripError(e.message);
    }
  }

  function closeModal() {
    els.overlay.classList.add("hidden");
    els.modal.innerHTML = "";
    state.splitType = null;
  }

  function openAddFriend() {
    els.modal.innerHTML = `
      <input id="friend-username" class="input-i1" type="text" placeholder="Enter a username to add." maxlength="255" />
      <button type="button" id="friend-done">Done</button>
      <p id="friend-error" class="error-message"></p>
      <button type="button" id="friend-cancel">Cancel</button>`;
    els.overlay.classList.remove("hidden");
    document.getElementById("friend-cancel").addEventListener("click", closeModal);
    document.getElementById("friend-done").addEventListener("click", async () => {
      const username = document.getElementById("friend-username").value.trim();
      try {
        const data = await api("/trips/" + state.selectedTrip.id + "/users", {
          method: "POST",
          body: JSON.stringify({ username }),
        });
        state.members.push(data.user);
        closeModal();
      } catch (e) {
        document.getElementById("friend-error").textContent = e.message;
      }
    });
  }

  function openAddExpense() {
    state.expenseDraft = {
      note: "",
      users: state.members.map((m) => ({ id: m.id, name: m.name, lent: 0, borrow: 0 })),
    };
    state.splitType = null;
    state.splitEqually = false;
    state.selectedSplitMembers = [];
    renderExpenseModal();
    els.overlay.classList.remove("hidden");
  }

  function paidAmount() {
    return round2(state.expenseDraft.users.reduce((sum, u) => sum + Number(u.lent || 0), 0));
  }

  function splitAmount() {
    return round2(state.expenseDraft.users.reduce((sum, u) => sum + Number(u.borrow || 0), 0));
  }

  function renderExpenseModal() {
    const paid = paidAmount();
    const split = splitAmount();
    if (!state.splitType) {
      els.modal.innerHTML = `
        <div class="add-expense">
          <input id="expense-note" class="expense-note" type="text" placeholder="Enter a Description." maxlength="1000" value="${escapeHtml(state.expenseDraft.note)}" />
          <p class="split-totals">Paid Amount: ₹${paid} | Split Amount: ₹${split}</p>
          <div class="split-buttons">
            <div>Paid by: <button type="button" id="select-paid">Select</button></div>
            <div>Split to: <button type="button" id="select-split">Select</button></div>
          </div>
          <p id="expense-error" class="error-message"></p>
          <div style="display:flex;justify-content:space-around;margin-top:20px">
            <button type="button" id="expense-cancel">Cancel</button>
            <button type="button" id="expense-done">Done</button>
          </div>
        </div>`;
      document.getElementById("select-paid").addEventListener("click", () => {
        state.expenseDraft.note = document.getElementById("expense-note").value;
        state.splitType = "PaidBy";
        renderExpenseModal();
      });
      document.getElementById("select-split").addEventListener("click", () => {
        state.expenseDraft.note = document.getElementById("expense-note").value;
        state.splitType = "SplitTo";
        renderExpenseModal();
      });
      document.getElementById("expense-cancel").addEventListener("click", closeModal);
      document.getElementById("expense-done").addEventListener("click", submitExpense);
      return;
    }

    const rows = state.members
      .map((member) => {
        const user = state.expenseDraft.users.find((u) => u.id === member.id);
        const amount = state.splitType === "PaidBy" ? user.lent : user.borrow;
        const checked = state.selectedSplitMembers.includes(member.id) ? "checked" : "";
        const disabled =
          state.splitType === "SplitTo" && (!state.selectedSplitMembers.includes(member.id) || state.splitEqually)
            ? "disabled"
            : "";
        return `<li class="li2">
          <div class="split-item">
            <p>${escapeHtml(member.name)}</p>
            ${state.splitType === "SplitTo" ? `<input type="checkbox" data-member-id="${member.id}" ${checked} />` : ""}
            <div class="split-input">
              <p>₹</p>
              <input type="number" step="0.01" data-amount-id="${member.id}" value="${amount || ""}" ${disabled} />
            </div>
          </div>
        </li>`;
      })
      .join("");

    els.modal.innerHTML = `
      <div class="split-container">
        <h3>${state.splitType}</h3>
        <ul class="ul2">${rows}</ul>
        ${
          state.splitType === "SplitTo"
            ? `<label><input type="checkbox" id="split-equally" ${state.splitEqually ? "checked" : ""} /> Split Equally</label>`
            : ""
        }
        <p class="split-totals">Paid Amount: ₹${paid} | Split Amount: ₹${split}</p>
        <div style="display:flex;justify-content:space-around;margin-top:20px">
          <button type="button" id="split-back">Back</button>
          <button type="button" id="split-done">Done</button>
        </div>
      </div>`;

    els.modal.querySelectorAll("[data-amount-id]").forEach((input) => {
      input.addEventListener("input", () => {
        const id = Number(input.getAttribute("data-amount-id"));
        const value = Number(input.value || 0);
        const user = state.expenseDraft.users.find((u) => u.id === id);
        if (state.splitType === "PaidBy") user.lent = value;
        else user.borrow = value;
        const totals = els.modal.querySelector(".split-totals");
        if (totals) totals.textContent = `Paid Amount: ₹${paidAmount()} | Split Amount: ₹${splitAmount()}`;
      });
    });
    els.modal.querySelectorAll("[data-member-id]").forEach((box) => {
      box.addEventListener("change", () => {
        const id = Number(box.getAttribute("data-member-id"));
        if (box.checked) state.selectedSplitMembers.push(id);
        else state.selectedSplitMembers = state.selectedSplitMembers.filter((x) => x !== id);
        if (state.splitEqually) applyEqualSplit();
        renderExpenseModal();
      });
    });
    const equal = document.getElementById("split-equally");
    if (equal) {
      equal.addEventListener("change", () => {
        state.splitEqually = equal.checked;
        if (state.splitEqually) applyEqualSplit();
        renderExpenseModal();
      });
    }
    document.getElementById("split-back").addEventListener("click", () => {
      state.splitType = null;
      renderExpenseModal();
    });
    document.getElementById("split-done").addEventListener("click", () => {
      state.splitType = null;
      renderExpenseModal();
    });
  }

  function applyEqualSplit() {
    const total = paidAmount();
    const members = state.selectedSplitMembers;
    if (!members.length) return;
    const equal = Math.floor((total / members.length) * 100) / 100;
    let remainder = round2(total - equal * members.length);
    state.expenseDraft.users.forEach((user) => {
      if (!members.includes(user.id)) {
        user.borrow = 0;
        return;
      }
      let amount = equal;
      if (remainder >= 0.01) {
        amount = round2(amount + 0.01);
        remainder = round2(remainder - 0.01);
      }
      user.borrow = amount;
    });
  }

  async function submitExpense() {
    const noteEl = document.getElementById("expense-note");
    if (noteEl) state.expenseDraft.note = noteEl.value;
    const error = document.getElementById("expense-error");
    if (!state.expenseDraft.note.trim()) {
      error.textContent = "Enter a Description to Add the Expense.";
      return;
    }
    const paid = paidAmount();
    const split = splitAmount();
    if (paid === 0 && split === 0) {
      error.textContent = "Enter some amounts to Add the Expense.";
      return;
    }
    if (paid !== split) {
      error.textContent = "Total amount paid must match total amount split.";
      return;
    }
    try {
      await api("/trips/" + state.selectedTrip.id + "/transactions", {
        method: "POST",
        body: JSON.stringify({
          amount: paid,
          note: state.expenseDraft.note.trim(),
          date: new Date().toISOString().split("T")[0],
          users: state.expenseDraft.users.filter((u) => u.lent > 0 || u.borrow > 0),
        }),
      });
      closeModal();
      await selectTrip(state.selectedTrip.id, { toggle: false });
    } catch (e) {
      error.textContent = e.message;
    }
  }

  async function summarize() {
    if (!state.selectedTrip) return;
    els.dashboardView.classList.add("hidden");
    els.summaryView.classList.remove("hidden");
    els.summaryBox.textContent = "Loading summary...";
    try {
      const data = await api("/trips/" + state.selectedTrip.id + "/summary", {
        method: "POST",
        body: JSON.stringify({}),
      });
      els.summaryBox.textContent = data.summary || "No summary available.";
    } catch (e) {
      els.summaryBox.textContent = e.message;
    }
  }

  els.authToggle.addEventListener("click", () => showAuth(!state.isLogin));
  els.authForm.addEventListener("submit", async (event) => {
    event.preventDefault();
    els.authError.textContent = "";
    const payload = {
      username: els.authUsername.value.trim(),
      password: els.authPassword.value,
    };
    if (!state.isLogin) payload.email = els.authEmail.value.trim();
    try {
      const path = state.isLogin ? "/login" : "/signup";
      state.user = await api(path, { method: "POST", body: JSON.stringify(payload) });
      showApp();
    } catch (e) {
      els.authError.textContent = e.message;
      return;
    }
    try {
      await loadTrips();
    } catch (e) {
      setTripError(e.message);
      renderTrips();
    }
  });
  els.logoutBtn.addEventListener("click", async () => {
    try {
      await api("/logout", { method: "POST", body: "{}" });
    } catch (e) {
      /* still return to login */
    }
    state.user = null;
    state.trips = [];
    state.selectedTrip = null;
    state.members = [];
    state.expenses = [];
    state.serverSettlements = [];
    setTripError("");
    showAuth(true);
  });
  document.getElementById("add-friend-btn").addEventListener("click", openAddFriend);
  document.getElementById("add-expense-btn").addEventListener("click", openAddExpense);
  document.getElementById("summarize-btn").addEventListener("click", summarize);
  document.getElementById("summary-done").addEventListener("click", () => {
    els.summaryView.classList.add("hidden");
    els.dashboardView.classList.remove("hidden");
  });
  els.overlay.addEventListener("click", (event) => {
    if (event.target === els.overlay) closeModal();
  });

  async function boot() {
    try {
      state.user = await api("/me");
      showApp();
    } catch (e) {
      showAuth(true);
      return;
    }
    try {
      await loadTrips();
    } catch (e) {
      setTripError(e.message);
      renderTrips();
    }
  }

  boot();
})();
