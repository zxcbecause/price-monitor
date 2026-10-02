package io.github.zxcbecause.pricemonitor.domain;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;

/**
 * Prices are stored as kopecks ({@code long}) to avoid floating point errors.
 * The API speaks rubles ({@link BigDecimal}).
 */
public final class Money {

    private static final BigDecimal HUNDRED = BigDecimal.valueOf(100);

    private Money() {
    }

    public static long toKopecks(BigDecimal rubles) {
        return rubles.multiply(HUNDRED).setScale(0, RoundingMode.HALF_UP).longValueExact();
    }

    public static BigDecimal toRubles(long kopecks) {
        return BigDecimal.valueOf(kopecks).divide(HUNDRED, 2, RoundingMode.UNNECESSARY);
    }

    public static Long toKopecksOrNull(BigDecimal rubles) {
        return rubles == null ? null : toKopecks(rubles);
    }

    public static BigDecimal toRublesOrNull(Long kopecks) {
        return kopecks == null ? null : toRubles(kopecks);
    }

    /** {@code 123456} -> {@code "1 234,56 ₽"}, {@code 100000} -> {@code "1 000 ₽"}. */
    public static String format(long kopecks) {
        DecimalFormatSymbols symbols = new DecimalFormatSymbols(Locale.ROOT);
        symbols.setGroupingSeparator(' ');
        symbols.setDecimalSeparator(',');
        DecimalFormat format = new DecimalFormat("#,##0.##", symbols);
        return format.format(toRubles(kopecks)) + " ₽";
    }
}
