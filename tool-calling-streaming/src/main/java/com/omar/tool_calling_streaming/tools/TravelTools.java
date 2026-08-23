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