package com.billsplit.service;

import com.billsplit.model.Expense;
import com.billsplit.model.Settlement;
import com.billsplit.model.TransactionShare;
import com.billsplit.model.User;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Ports the original client-side settlement minimizer.
 */
public final class SettlementCalculator {
    private static final BigDecimal SETTLED_BALANCE = new BigDecimal("1.00");
    private static final BigDecimal THRESHOLD = new BigDecimal("0.10");

    public List<Settlement> calculate(List<User> members, List<Expense> expenses) {
        Map<Long, BigDecimal> balances = new HashMap<>();
        Map<Long, String> names = new HashMap<>();
        for (User member : members) {
            balances.put(member.getId(), BigDecimal.ZERO);
            names.put(member.getId(), member.getName());
        }

        for (Expense expense : expenses) {
            for (TransactionShare share : expense.getUsers()) {
                balances.merge(share.getUserId(), share.getLent().subtract(share.getBorrow()), BigDecimal::add);
                names.putIfAbsent(share.getUserId(), share.getName() == null ? "Unknown" : share.getName());
            }
        }

        List<Balance> getsBack = new ArrayList<>();
        List<Balance> paysBack = new ArrayList<>();
        List<Settlement> transactions = new ArrayList<>();

        for (Map.Entry<Long, BigDecimal> entry : balances.entrySet()) {
            BigDecimal amount = entry.getValue().setScale(2, RoundingMode.HALF_UP);
            long id = entry.getKey();
            String name = names.getOrDefault(id, "Unknown");
            if (amount.abs().compareTo(SETTLED_BALANCE) < 0) {
                transactions.add(new Settlement(name, "No one", BigDecimal.ZERO.setScale(2, RoundingMode.UNNECESSARY), true));
            } else if (amount.compareTo(BigDecimal.ZERO) < 0) {
                paysBack.add(new Balance(id, name, amount.abs()));
            } else if (amount.compareTo(BigDecimal.ZERO) > 0) {
                getsBack.add(new Balance(id, name, amount));
            }
        }

        getsBack.sort(Comparator.comparing((Balance b) -> b.amount).reversed());
        paysBack.sort(Comparator.comparing((Balance b) -> b.amount).reversed());

        while (!getsBack.isEmpty() && !paysBack.isEmpty()) {
            Balance creditor = getsBack.get(0);
            Balance debtor = paysBack.get(0);
            if (creditor.amount.compareTo(THRESHOLD) <= 0) {
                getsBack.remove(0);
                continue;
            }

            BigDecimal settleAmount = creditor.amount.min(debtor.amount).setScale(2, RoundingMode.HALF_UP);
            transactions.add(new Settlement(
                    debtor.name,
                    creditor.name,
                    settleAmount,
                    settleAmount.compareTo(SETTLED_BALANCE) < 0));

            creditor.amount = creditor.amount.subtract(settleAmount);
            debtor.amount = debtor.amount.subtract(settleAmount);
            if (creditor.amount.compareTo(THRESHOLD) <= 0) {
                getsBack.remove(0);
            }
            if (debtor.amount.compareTo(THRESHOLD) <= 0) {
                paysBack.remove(0);
            }
        }

        return transactions;
    }

    private static final class Balance {
        final long id;
        final String name;
        BigDecimal amount;

        Balance(long id, String name, BigDecimal amount) {
            this.id = id;
            this.name = name;
            this.amount = amount;
        }
    }
}
