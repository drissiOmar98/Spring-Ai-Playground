package com.omar.tool_calling_streaming.tools;

import java.time.LocalDate;
import java.time.format.TextStyle;
import java.util.Locale;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.stereotype.Component;

/**
 * Tools exposed to the model for the travel chat demo: resolving relative
 * dates, and looking up weather and events for a city over a date range.
 *
 * <p>Each method sleeps briefly via {@link #pause()} purely so the
 * "Calling {@code toolName}..." pill has time to render on screen during a
 * demo — remove {@link #pause()} calls for production use.
 *
 * <p>All three methods return canned data; swap the bodies for real API
 * calls (e.g. a weather provider, an events API) without changing the
 * {@code @Tool} contracts below.
 */
@Component
public class TravelTools {

    /**
     * Returns today's date, so the model can resolve relative expressions
     * like "next week" or "tomorrow" without guessing.
     *
     * @return today's date followed by its full weekday name, e.g.
     *         {@code "2026-08-23 (Sunday)"}
     */
    @Tool(description = "Get today's date. Use this to resolve relative dates like 'next week' or 'tomorrow'.")
    public String getCurrentDate() {
        pause();
        LocalDate today = LocalDate.now();
        return today + " (" + today.getDayOfWeek().getDisplayName(TextStyle.FULL, Locale.ENGLISH) + ")";
    }

    /**
     * Returns a weather forecast summary for a city over a date range.
     *
     * @param city      the city to forecast for
     * @param startDate ISO date ({@code yyyy-MM-dd}), inclusive
     * @param endDate   ISO date ({@code yyyy-MM-dd}), inclusive
     * @return a short natural-language forecast summary
     */
    @Tool(description = "Get the weather forecast for a city over a date range (dates in ISO format yyyy-MM-dd)")
    public String getWeatherForecast(String city, String startDate, String endDate) {
        pause();
        return city + ", " + startDate + " to " + endDate + ": highs 78-85\u00B0F, lows 62-68\u00B0F. "
                + "Thunderstorms likely Tuesday and Wednesday, sunny otherwise. "
                + "Windy near the lakefront (gusts 25+ mph).";
    }

    /**
     * Returns notable events happening in a city over a date range.
     *
     * @param city      the city to look up events for
     * @param startDate ISO date ({@code yyyy-MM-dd}), inclusive
     * @param endDate   ISO date ({@code yyyy-MM-dd}), inclusive
     * @return a short natural-language summary of notable events
     */
    @Tool(description = "Get notable events happening in a city during a date range (dates in ISO format yyyy-MM-dd)")
    public String getCityEvents(String city, String startDate, String endDate) {
        pause();
        return "Cubs home series at Wrigley Field (3 games mid-week); "
                + "outdoor evening concert in Grant Park (bring layers, it cools off fast); "
                + "Riverwalk food festival all week.";
    }


    /**
     * Converts an amount from one currency to another.
     *
     * @param amount       the amount to convert
     * @param fromCurrency ISO 4217 currency code, e.g. {@code "USD"}
     * @param toCurrency   ISO 4217 currency code, e.g. {@code "EUR"}
     * @return a short natural-language conversion summary
     */
    @Tool(description = "Convert an amount from one currency to another (ISO 4217 codes, e.g. USD, EUR, JPY)")
    public String convertCurrency(double amount, String fromCurrency, String toCurrency) {
        pause();
        double rate = 0.92; // placeholder — wire up a real FX rate provider
        return String.format("%.2f %s \u2248 %.2f %s (rate used: %.4f, illustrative only)",
                amount, fromCurrency.toUpperCase(Locale.ENGLISH), amount * rate,
                toCurrency.toUpperCase(Locale.ENGLISH), rate);
    }

    /**
     * Suggests what to pack for a city and date range, based on typical
     * conditions and planned activities.
     *
     * @param city      the destination city
     * @param startDate ISO date ({@code yyyy-MM-dd}), inclusive
     * @param endDate   ISO date ({@code yyyy-MM-dd}), inclusive
     * @return a short natural-language packing suggestion
     */
    @Tool(description = "Suggest what to pack for a city and date range (dates in ISO format yyyy-MM-dd)")
    public String getPackingSuggestions(String city, String startDate, String endDate) {
        pause();
        return "Layers for warm days and cool evenings, a light rain jacket for midweek storms, "
                + "comfortable walking shoes, and a light windbreaker for the lakefront. "
                + "Pack one outfit sturdy enough for a stadium visit.";
    }

    /**
     * Returns the current local time and UTC offset for a city.
     *
     * @param city the city to look up
     * @return a short natural-language local-time summary
     */
    @Tool(description = "Get the current local time and UTC offset for a city")
    public String getLocalTime(String city) {
        pause();
        return city + ": local time is currently in Central Time (UTC-5 during daylight saving).";
    }

    /**
     * Returns a rough round-trip flight price estimate between two cities
     * for a date range.
     *
     * @param originCity      the departure city
     * @param destinationCity the arrival city
     * @param startDate       ISO date ({@code yyyy-MM-dd}) of departure
     * @param endDate         ISO date ({@code yyyy-MM-dd}) of return
     * @return a short natural-language price-range estimate
     */
    @Tool(description = "Get a rough round-trip flight price estimate between two cities for a date range "
            + "(dates in ISO format yyyy-MM-dd)")
    public String estimateFlightPrice(String originCity, String destinationCity, String startDate, String endDate) {
        pause();
        return "Round-trip " + originCity + " \u2192 " + destinationCity + " (" + startDate + " to " + endDate
                + "): roughly $280-$420 economy, depending on how far ahead you book. "
                + "Midweek departures tend to run cheaper than weekend ones.";
    }

    /**
     * Estimates typical nightly accommodation cost for a city.
     *
     * @param city     the destination city
     * @param style    lodging style, e.g. {@code "budget"}, {@code "mid-range"}, {@code "luxury"}
     * @return a short natural-language nightly-cost estimate
     */
    @Tool(description = "Get a rough nightly accommodation price estimate for a city and lodging style "
            + "(style is one of: budget, mid-range, luxury)")
    public String estimateAccommodationCost(String city, String style) {
        pause();
        String range = switch (style.toLowerCase(Locale.ENGLISH)) {
            case "luxury" -> "$350-$600/night";
            case "mid-range" -> "$150-$280/night";
            default -> "$70-$130/night";
        };
        return city + " (" + style + "): roughly " + range + ", excluding taxes and fees.";
    }

    /**
     * Blocks briefly so a demo viewer has time to see the "Calling..." pill
     * before the tool result arrives. Not needed outside a live demo.
     */
    private void pause() {
        try {
            Thread.sleep(1500);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}