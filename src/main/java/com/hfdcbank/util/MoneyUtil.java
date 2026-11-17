package com.hfdcbank.util;

import java.math.BigDecimal;
import java.math.RoundingMode;

/** Central place for financial-number rules. */
public final class MoneyUtil {

    private MoneyUtil() {
    }

    /**
     * Financial values always have exactly two decimal places. Rounding is
     * HALF_UP (the banking convention), never the default HALF_EVEN.
     */
    public static BigDecimal money(BigDecimal amount) {
        return amount.setScale(2, RoundingMode.HALF_UP);
    }

    public static boolean isPositive(BigDecimal amount) {
        return amount != null && amount.compareTo(BigDecimal.ZERO) > 0;
    }
}
