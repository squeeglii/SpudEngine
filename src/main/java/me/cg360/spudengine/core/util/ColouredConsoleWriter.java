package me.cg360.spudengine.core.util;


import org.tinylog.Level;
import org.tinylog.core.LogEntry;
import org.tinylog.writers.AbstractFormatPatternWriter;

import java.util.Map;

public class ColouredConsoleWriter extends AbstractFormatPatternWriter {

    public static final String ANSI_RESET = "\u001B[0m";

    private String ansiColour;
    private Level logLevel;

    public ColouredConsoleWriter(Map<String, String> properties) {
        super(properties);

        this.logLevel = Level.valueOf(this.getStringValue("level").toUpperCase());
        this.ansiColour = this.getStringValue("colour");
    }

    @Override
    public void write(LogEntry logEntry) throws Exception {
        if (logEntry.getLevel().ordinal() != this.logLevel.ordinal())
            return;
        String color = this.ansiColour == null
                ? ANSI_RESET // RESET
                : "\u001b[%sm".formatted(this.ansiColour);

        String newLine = color + this.render(logEntry) + ANSI_RESET;
        System.out.print(newLine);
    }

    @Override
    public void flush() throws Exception {
        System.out.flush();
    }

    @Override
    public void close() throws Exception {

    }

}
