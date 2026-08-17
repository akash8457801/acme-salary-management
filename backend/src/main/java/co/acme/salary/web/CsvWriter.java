package co.acme.salary.web;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.UncheckedIOException;
import java.io.Writer;
import java.nio.charset.StandardCharsets;

/**
 * Minimal RFC 4180 CSV writer.
 *
 * <p>Hand-written rather than pulled in as a dependency: the export needs quoting and a BOM, and
 * that is about forty lines. The BOM is there because the file's first stop is Excel, which
 * otherwise mangles every non-ASCII name in the directory.
 */
final class CsvWriter implements AutoCloseable {

    private static final char DELIMITER = ',';
    private static final String LINE_END = "\r\n";
    private static final String UTF8_BOM = "﻿";

    private final Writer writer;

    CsvWriter(OutputStream outputStream) {
        this.writer = new BufferedWriter(new OutputStreamWriter(outputStream, StandardCharsets.UTF_8));
        write(UTF8_BOM);
    }

    void writeRow(String... values) {
        StringBuilder row = new StringBuilder();
        for (int i = 0; i < values.length; i++) {
            if (i > 0) {
                row.append(DELIMITER);
            }
            row.append(escape(values[i]));
        }
        write(row.append(LINE_END).toString());
    }

    private String escape(String value) {
        if (value == null) {
            return "";
        }
        boolean needsQuoting = value.indexOf(DELIMITER) >= 0
                || value.indexOf('"') >= 0
                || value.indexOf('\n') >= 0
                || value.indexOf('\r') >= 0;
        return needsQuoting ? '"' + value.replace("\"", "\"\"") + '"' : value;
    }

    private void write(String text) {
        try {
            writer.write(text);
        } catch (IOException e) {
            throw new UncheckedIOException("Failed writing CSV export", e);
        }
    }

    @Override
    public void close() {
        try {
            writer.flush();
        } catch (IOException e) {
            throw new UncheckedIOException("Failed flushing CSV export", e);
        }
    }
}
