package io.github.terseprompts.fastmcp.example.tools;

import io.github.terseprompts.fastmcp.annotations.McpTool;

/**
 * Unit conversion tools.
 * These will be auto-discovered by package scanning.
 */
public class ConversionTools {

    @McpTool(description = "Convert Celsius to Fahrenheit")
    public double celsiusToFahrenheit(double celsius) {
        return (celsius * 9/5) + 32;
    }

    @McpTool(description = "Convert Fahrenheit to Celsius")
    public double fahrenheitToCelsius(double fahrenheit) {
        return (fahrenheit - 32) * 5/9;
    }

    @McpTool(description = "Convert kilometers to miles")
    public double kmToMiles(double km) {
        return km * 0.621371;
    }

    @McpTool(description = "Convert miles to kilometers")
    public double milesToKm(double miles) {
        return miles / 0.621371;
    }

    @McpTool(description = "Convert kilograms to pounds")
    public double kgToPounds(double kg) {
        return kg * 2.20462;
    }

    @McpTool(description = "Convert pounds to kilograms")
    public double poundsToKg(double pounds) {
        return pounds / 2.20462;
    }
}
