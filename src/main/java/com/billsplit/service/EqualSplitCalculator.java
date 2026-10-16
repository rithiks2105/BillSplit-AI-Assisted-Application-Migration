package com.billsplit.service;

import com.billsplit.model.TransactionShare;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

/**
 * Splits an amount equally across selected members using 2 decimal places.
 * Leftover cents are assigned to the first members so the total matches.
 */
public final class EqualSplitCalculator {
    public List<TransactionShare> split(BigDecimal total, List<Long> selectedMemberIds) {
        if (total == null || total.compareTo(BigDecimal.ZERO) <= 0) {
            throw new AppException(400, "Paid amount must be greater than zero before splitting equally.");
        }
        if (selectedMemberIds == null || selectedMemberIds.isEmpty()) {
            throw new AppException(400, "Select at least one member to split equally.");
        }

        BigDecimal scaledTotal = total.setScale(2, RoundingMode.HALF_UP);
        int n = selectedMemberIds.size();
        BigDecimal share = scaledTotal.divide(BigDecimal.valueOf(n), 2, RoundingMode.DOWN);
        BigDecimal remainder = scaledTotal.subtract(share.multiply(BigDecimal.valueOf(n)));
        BigDecimal penny = new BigDecimal("0.01");

        List<TransactionShare> result = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            BigDecimal amount = share;
            if (remainder.compareTo(BigDecimal.ZERO) > 0) {
                amount = amount.add(penny);
                remainder = remainder.subtract(penny);
            }
            result.add(new TransactionShare(selectedMemberIds.get(i), null, amount, BigDecimal.ZERO));
        }
        return result;
    }
}
