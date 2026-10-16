package com.billsplit.web;

import com.billsplit.model.Expense;
import com.billsplit.model.Settlement;
import com.billsplit.model.TransactionShare;
import com.billsplit.model.Trip;
import com.billsplit.model.User;
import com.billsplit.service.SummaryService;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@WebServlet(urlPatterns = {"/api/trips", "/api/trips/*"})
public final class TripsServlet extends HttpServlet {
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        try {
            long userId = Sessions.requireUserId(req);
            String path = req.getPathInfo();
            AppContext app = AppContext.from(getServletContext());
            if (path == null || "/".equals(path)) {
                JSONArray trips = new JSONArray();
                for (Trip trip : app.getTripService().listTrips(userId)) {
                    trips.put(toTrip(trip));
                }
                JsonSupport.write(resp, 200, trips);
                return;
            }
            ParsedPath parsed = ParsedPath.parse(path);
            if (parsed.users) {
                JSONArray users = new JSONArray();
                for (User user : app.getTripService().listMembers(userId, parsed.tripId)) {
                    users.put(toMember(user));
                }
                JsonSupport.write(resp, 200, users);
                return;
            }
            if (parsed.transactions) {
                List<Expense> expenses = app.getTransactionService().list(userId, parsed.tripId);
                List<User> members = app.getTripService().listMembers(userId, parsed.tripId);
                JSONArray items = new JSONArray();
                expenses.forEach(expense -> items.put(toExpense(expense)));
                JSONArray settlements = new JSONArray();
                for (Settlement settlement : app.getSettlementCalculator().calculate(members, expenses)) {
                    if (settlement.getAmount().compareTo(BigDecimal.ZERO) <= 0
                            || "No one".equals(settlement.getTo())) {
                        continue;
                    }
                    settlements.put(new JSONObject()
                            .put("from", settlement.getFrom())
                            .put("to", settlement.getTo())
                            .put("amount", settlement.getAmount().toPlainString())
                            .put("settled", settlement.isSettled()));
                }
                JsonSupport.write(resp, 200, new JSONObject()
                        .put("transactions", items)
                        .put("settlements", settlements));
                return;
            }
            JsonSupport.error(resp, 404, "Not found.");
        } catch (RuntimeException e) {
            JsonSupport.handle(resp, e);
        }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        try {
            long userId = Sessions.requireUserId(req);
            String path = req.getPathInfo();
            AppContext app = AppContext.from(getServletContext());
            JSONObject body = JsonSupport.read(req);
            if (path == null || "/".equals(path)) {
                Trip trip = app.getTripService().createTrip(userId, body.optString("name", null));
                JsonSupport.write(resp, 201, new JSONObject()
                        .put("message", "Trip created successfully")
                        .put("trip", toTrip(trip)));
                return;
            }
            ParsedPath parsed = ParsedPath.parse(path);
            if (parsed.users) {
                User user = app.getTripService().addMember(userId, parsed.tripId, body.optString("username", null));
                JsonSupport.write(resp, 201, new JSONObject()
                        .put("message", "User added to trip successfully.")
                        .put("user", toMember(user)));
                return;
            }
            if (parsed.transactions) {
                Expense expense = parseExpense(body);
                long id = app.getTransactionService().create(userId, parsed.tripId, expense);
                JsonSupport.write(resp, 201, new JSONObject()
                        .put("message", "Transaction added successfully!")
                        .put("transactionId", id));
                return;
            }
            if (parsed.summary) {
                List<Expense> expenses = app.getTransactionService().list(userId, parsed.tripId);
                SummaryService.SummaryResult result = app.getSummaryService().summarize(expenses);
                int status = result.configured() ? 200 : 200;
                JsonSupport.write(resp, status, new JSONObject()
                        .put("configured", result.configured())
                        .put("summary", result.text()));
                return;
            }
            JsonSupport.error(resp, 404, "Not found.");
        } catch (RuntimeException e) {
            JsonSupport.handle(resp, e);
        }
    }

    private static Expense parseExpense(JSONObject body) {
        BigDecimal amount = JsonSupport.decimal(body, "amount", BigDecimal.ZERO);
        String note = body.optString("note", "");
        LocalDate date = LocalDate.now();
        if (body.has("date") && !body.isNull("date") && !body.optString("date").isBlank()) {
            String raw = body.optString("date");
            try {
                date = LocalDate.parse(raw.substring(0, Math.min(10, raw.length())));
            } catch (DateTimeParseException e) {
                throw new com.billsplit.service.AppException(400, "Invalid expense date.");
            }
        }
        List<TransactionShare> shares = new ArrayList<>();
        JSONArray users = body.optJSONArray("users");
        if (users != null) {
            for (int i = 0; i < users.length(); i++) {
                JSONObject u = users.getJSONObject(i);
                shares.add(new TransactionShare(
                        JsonSupport.requiredLong(u, "id"),
                        u.optString("name", null),
                        JsonSupport.decimal(u, "borrow", BigDecimal.ZERO),
                        JsonSupport.decimal(u, "lent", BigDecimal.ZERO)));
            }
        }
        return new Expense(null, amount, note, date, shares);
    }

    private static JSONObject toTrip(Trip trip) {
        JSONObject json = new JSONObject().put("id", trip.getId()).put("name", trip.getName());
        if (trip.getStartDate() != null) {
            json.put("start_date", trip.getStartDate().format(
                    DateTimeFormatter.ofPattern("EEE MMM dd yyyy", Locale.ENGLISH)));
        } else {
            json.put("start_date", JSONObject.NULL);
        }
        return json;
    }

    private static JSONObject toMember(User user) {
        return new JSONObject().put("id", user.getId()).put("name", user.getName());
    }

    private static JSONObject toExpense(Expense expense) {
        JSONArray users = new JSONArray();
        for (TransactionShare share : expense.getUsers()) {
            users.put(new JSONObject()
                    .put("id", share.getUserId())
                    .put("name", share.getName())
                    .put("lent", share.getLent())
                    .put("borrow", share.getBorrow()));
        }
        return new JSONObject()
                .put("id", expense.getId())
                .put("amount", expense.getAmount())
                .put("note", expense.getNote())
                .put("date", expense.getDate() == null ? JSONObject.NULL : expense.getDate().toString())
                .put("users", users);
    }

    private static final class ParsedPath {
        final long tripId;
        final boolean users;
        final boolean transactions;
        final boolean summary;

        private ParsedPath(long tripId, boolean users, boolean transactions, boolean summary) {
            this.tripId = tripId;
            this.users = users;
            this.transactions = transactions;
            this.summary = summary;
        }

        static ParsedPath parse(String path) {
            String[] parts = path.split("/");
            List<String> tokens = new ArrayList<>();
            for (String part : parts) {
                if (!part.isBlank()) {
                    tokens.add(part);
                }
            }
            if (tokens.isEmpty()) {
                throw new com.billsplit.service.AppException(404, "Not found.");
            }
            long tripId;
            try {
                tripId = Long.parseLong(tokens.get(0));
            } catch (NumberFormatException e) {
                throw new com.billsplit.service.AppException(400, "Trip ID is required.");
            }
            String resource = tokens.size() > 1 ? tokens.get(1) : "";
            return new ParsedPath(
                    tripId,
                    "users".equals(resource),
                    "transactions".equals(resource),
                    "summary".equals(resource));
        }
    }
}
