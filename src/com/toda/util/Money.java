// src/com/toda/util/Money.
package com.toda.util;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;

/** Peso helpers: centavo rounding and "₱1,240.00" formatting. */
public final class Money {
    private Money() {
    }

    public static double round(double amount) {
        return BigDecimal.valueOf(amount).setScale(2, RoundingMode.HALF_UP).doubleValue();
    }

    public static String format(double amount) {
        DecimalFormat df = new DecimalFormat("#,##0.00", DecimalFormatSymbols.getInstance(Locale.US));
        String body = df.format(Math.abs(round(amount)));
        return (amount < 0 ? "-₱" : "₱") + body;
    }
}
