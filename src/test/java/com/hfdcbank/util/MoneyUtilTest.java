package com.hfdcbank.util;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Money rules are tiny but critical: every amount must end up with exactly two
 * decimal places and use HALF_UP rounding (never the JVM default HALF_EVEN).
 */
class MoneyUtilTest {

    @Test
    void moneyAlwaysUsesTwoDecimalPlaces() {
        assertThat(MoneyUtil.money(new BigDecimal("10"))).isEqualByComparingTo("10.00");
        assertThat(MoneyUtil.money(new BigDecimal("10.5")).scale()).isEqualTo(2);
        assertThat(MoneyUtil.money(new BigDecimal("10.50")).scale()).isEqualTo(2);
    }

    @Test
    void moneyRoundsHalfUpLikeABank() {
        assertThat(MoneyUtil.money(new BigDecimal("10.005"))).isEqualByComparingTo("10.01");
        assertThat(MoneyUtil.money(new BigDecimal("10.004"))).isEqualByComparingTo("10.00");
        assertThat(MoneyUtil.money(new BigDecimal("10.015"))).isEqualByComparingTo("10.02");
        assertThat(MoneyUtil.money(new BigDecimal("-10.005"))).isEqualByComparingTo("-10.01");
    }

    @Test
    void positiveAmountsAreDetected() {
        assertThat(MoneyUtil.isPositive(new BigDecimal("0.01"))).isTrue();
        assertThat(MoneyUtil.isPositive(new BigDecimal("1000000"))).isTrue();
    }

    @Test
    void zeroNegativeAndNullAreNotPositive() {
        assertThat(MoneyUtil.isPositive(BigDecimal.ZERO)).isFalse();
        assertThat(MoneyUtil.isPositive(new BigDecimal("-0.01"))).isFalse();
        assertThat(MoneyUtil.isPositive(null)).isFalse();
    }
}
