package com.renteasy.utils;

import android.content.Context;
import android.content.SharedPreferences;

public class CurrencyManager {

    private static final String PREF_NAME     = "renteasy_prefs";
    private static final String KEY_SYMBOL    = "currency_symbol";
    private static final String KEY_CODE      = "currency_code";

    // Common currencies: symbol, code, name
    public static final String[][] CURRENCIES = {
        {"₱", "PHP", "Philippine Peso"},
        {"$", "USD", "US Dollar"},
        {"€", "EUR", "Euro"},
        {"£", "GBP", "British Pound"},
        {"¥", "JPY", "Japanese Yen"},
        {"₹", "INR", "Indian Rupee"},
        {"Rp", "IDR", "Indonesian Rupiah"},
        {"RM", "MYR", "Malaysian Ringgit"},
        {"฿", "THB", "Thai Baht"},
        {"₩", "KRW", "Korean Won"},
        {"د.إ", "AED", "UAE Dirham"},
        {"﷼", "SAR", "Saudi Riyal"},
        {"kr", "NOK", "Norwegian Krone"},
        {"Fr", "CHF", "Swiss Franc"},
        {"R$", "BRL", "Brazilian Real"},
        {"$", "AUD", "Australian Dollar"},
        {"$", "CAD", "Canadian Dollar"},
        {"$", "SGD", "Singapore Dollar"},
    };

    public static void save(Context ctx, String symbol, String code) {
        ctx.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
           .edit()
           .putString(KEY_SYMBOL, symbol)
           .putString(KEY_CODE, code)
           .apply();
    }

    public static String getSymbol(Context ctx) {
        return ctx.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
                  .getString(KEY_SYMBOL, "₱");
    }

    public static String getCode(Context ctx) {
        return ctx.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
                  .getString(KEY_CODE, "PHP");
    }

    /** Format an amount with the saved currency symbol, e.g. "₱1,250.00" */
    public static String format(Context ctx, double amount) {
        return getSymbol(ctx) + String.format("%.2f", amount);
    }
}
