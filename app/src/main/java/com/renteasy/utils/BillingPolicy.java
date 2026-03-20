package com.renteasy.utils;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * Stores and applies the owner's chosen billing policy for leftover time.
 *
 * DAILY rentals with leftover hours:
 *   EXACT_HOURS   → charge leftover hrs at (dailyRate / 24)
 *   ROUND_UP_DAY  → any leftover hours = charge one full extra day
 *   GRACE_PERIOD  → if leftover < graceHours → free; else → full extra day
 *
 * MONTHLY rentals with leftover hours (after counting months → days):
 *   EXACT_HOURS   → charge leftover hrs at (hourlyRate)
 *   ROUND_UP_DAY  → any leftover hours = charge one full extra day
 *
 * HOURLY rentals always round up to next full hour (no policy choice needed).
 */
public class BillingPolicy {

    public enum OveragePolicy { EXACT_HOURS, ROUND_UP_DAY, GRACE_PERIOD }

    private static final String PREF          = "renteasy_prefs";
    private static final String KEY_DAILY_POL = "daily_overage_policy";
    private static final String KEY_MON_POL   = "monthly_overage_policy";
    private static final String KEY_GRACE_HRS = "grace_hours";

    // ── Save / Load ──────────────────────────────────────────────────────

    public static void saveDailyPolicy(Context ctx, OveragePolicy p) {
        prefs(ctx).edit().putString(KEY_DAILY_POL, p.name()).apply();
    }

    public static void saveMonthlyPolicy(Context ctx, OveragePolicy p) {
        prefs(ctx).edit().putString(KEY_MON_POL, p.name()).apply();
    }

    public static void saveGraceHours(Context ctx, int hours) {
        prefs(ctx).edit().putInt(KEY_GRACE_HRS, hours).apply();
    }

    public static OveragePolicy getDailyPolicy(Context ctx) {
        String s = prefs(ctx).getString(KEY_DAILY_POL, OveragePolicy.EXACT_HOURS.name());
        try { return OveragePolicy.valueOf(s); } catch (Exception e) { return OveragePolicy.EXACT_HOURS; }
    }

    public static OveragePolicy getMonthlyPolicy(Context ctx) {
        String s = prefs(ctx).getString(KEY_MON_POL, OveragePolicy.EXACT_HOURS.name());
        try { return OveragePolicy.valueOf(s); } catch (Exception e) { return OveragePolicy.EXACT_HOURS; }
    }

    public static int getGraceHours(Context ctx) {
        return prefs(ctx).getInt(KEY_GRACE_HRS, 6);
    }

    private static SharedPreferences prefs(Context ctx) {
        return ctx.getSharedPreferences(PREF, Context.MODE_PRIVATE);
    }

    // ── Cost calculation helpers ─────────────────────────────────────────

    /**
     * Calculate cost for a DAILY rental given elapsed milliseconds.
     * Applies the stored daily overage policy.
     */
    public static double calcDailyCost(Context ctx, double dailyRate, long ms) {
        if (ms <= 0) return 0;
        long millisPerDay  = 24L * 60 * 60 * 1000;
        long millisPerHour =       60L * 60 * 1000;

        long fullDays   = ms / millisPerDay;
        long remMs      = ms % millisPerDay;
        long remHours   = (remMs + millisPerHour - 1) / millisPerHour; // ceil

        double hourlyRate = dailyRate / 24.0;
        OveragePolicy pol = getDailyPolicy(ctx);

        switch (pol) {
            case ROUND_UP_DAY:
                // Any leftover → charge full extra day
                long totalDays = fullDays + (remMs > 0 ? 1 : 0);
                return totalDays * dailyRate;

            case GRACE_PERIOD:
                int grace = getGraceHours(ctx);
                if (remHours <= grace) {
                    // Within grace → free
                    return fullDays * dailyRate;
                } else {
                    // Past grace → full extra day
                    return (fullDays + 1) * dailyRate;
                }

            case EXACT_HOURS:
            default:
                return (fullDays * dailyRate) + (remHours * hourlyRate);
        }
    }

    /**
     * Calculate cost for a MONTHLY rental given elapsed milliseconds.
     * Applies the stored monthly overage policy.
     */
    public static double calcMonthlyCost(Context ctx, double monthlyRate, long ms) {
        if (ms <= 0) return 0;
        long millisPerMonth = 30L * 24 * 60 * 60 * 1000;
        long millisPerDay   =       24L * 60 * 60 * 1000;
        long millisPerHour  =            60L * 60 * 1000;

        double dailyRate  = monthlyRate / 30.0;
        double hourlyRate = dailyRate   / 24.0;

        long fullMonths  = ms / millisPerMonth;
        long remAfterMo  = ms % millisPerMonth;
        long fullDays    = remAfterMo / millisPerDay;
        long remAfterDay = remAfterMo % millisPerDay;
        long remHours    = (remAfterDay + millisPerHour - 1) / millisPerHour;

        OveragePolicy pol = getMonthlyPolicy(ctx);

        switch (pol) {
            case ROUND_UP_DAY:
                // Leftover hours → charge full extra day
                long extraDay = (remAfterDay > 0) ? 1 : 0;
                return (fullMonths * monthlyRate) + ((fullDays + extraDay) * dailyRate);

            case EXACT_HOURS:
            default:
                return (fullMonths * monthlyRate)
                     + (fullDays   * dailyRate)
                     + (remHours   * hourlyRate);
        }
    }

    /** Human-readable description of the current daily policy */
    public static String getDailyPolicyLabel(Context ctx) {
        switch (getDailyPolicy(ctx)) {
            case ROUND_UP_DAY:  return "Round up to next full day";
            case GRACE_PERIOD:  return "Grace period: " + getGraceHours(ctx) + " hrs free, then full day";
            default:            return "Charge exact leftover hours";
        }
    }

    /** Human-readable description of the current monthly policy */
    public static String getMonthlyPolicyLabel(Context ctx) {
        switch (getMonthlyPolicy(ctx)) {
            case ROUND_UP_DAY:  return "Round up leftover hours to full day";
            default:            return "Charge exact leftover hours";
        }
    }
}
