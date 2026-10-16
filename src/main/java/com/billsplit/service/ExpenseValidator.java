package com.billsplit.service;

import com.billsplit.model.Expense;
import com.billsplit.model.TransactionShare;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Set;

public final class ExpenseValidator {
    private static final BigDecimal ZERO = BigDecimal.ZERO.setScale(2, RoundingMode.UNNECESSARY);

    public void validate(Expense expense, Set<Long> tripMemberIds) {
        if (expense.getNote() == null || expense.getNote().isBlank()) {
            throw new AppException(400, "Enter a description to add the expense.");
        }
        if (expense.getNote().trim().length() > 1000) {
            throw new AppException(400, "Expense description is too long.");
        }
        if (expense.getAmount() == null || expense.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new AppException(400, "Enter some amounts to add the expense.");
        }
        if (expense.getUsers() == null || expense.getUsers().isEmpty()) {
            throw new AppException(400, "All transaction details are required.");
        }

        BigDecimal paid = BigDecimal.ZERO;
        BigDecimal split = BigDecimal.ZERO;
        for (TransactionShare share : expense.getUsers()) {
            if (share.getBorrow().compareTo(BigDecimal.ZERO) < 0 || share.getLent().compareTo(BigDecimal.ZERO) < 0) {
                throw new AppException(400, "Share amounts cannot be negative.");
            }
            if (tripMemberIds != null && !tripMemberIds.contains(share.getUserId())) {
                throw new AppException(400, "Every payer and split participant must belong to the trip.");
            }
            paid = paid.add(share.getLent());
            split = split.add(share.getBorrow());
        }

        BigDecimal amount = expense.getAmount().setScale(2, RoundingMode.HALF_UP);
        paid = paid.setScale(2, RoundingMode.HALF_UP);
        split = split.setScale(2, RoundingMode.HALF_UP);

        if (paid.compareTo(ZERO) == 0 && split.compareTo(ZERO) == 0) {
            throw new AppException(400, "Enter some amounts to add the expense.");
        }
        if (paid.compareTo(split) != 0) {
            throw new AppException(400, "Total amount paid must match total amount split.");
        }
        if (paid.compareTo(amount) != 0) {
            throw new AppException(400, "Expense amount must equal the total paid and split.");
        }
    }
}
