package com.hfdcbank.dto;

import java.math.BigDecimal;
import java.util.List;

/** All figures are computed from MySQL by AccountService - nothing is hardcoded. */
public record DashboardResponse(
        String fullName,
        String accountNumber,
        String accountType,
        BigDecimal balance,
        BigDecimal totalDeposits,
        BigDecimal totalWithdrawals,
        long transactionCount,
        List<TransactionResponse> recentTransactions,
        List<MonthlySummary> monthlySummary
) {
    /** One bar of the monthly analytics chart. */
    public record MonthlySummary(String month, BigDecimal deposits, BigDecimal withdrawals) {
    }
}
