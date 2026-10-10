package app.organicmaps.telecom.importer;

import androidx.annotation.NonNull;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * RFC 4180 compliant CSV parser.
 * Handles embedded commas, escaped quotes, multiline fields, and UTF-8 byte order marks (BOM).
 */
public class CsvParser {

  public static List<List<String>> parse(@NonNull InputStream inputStream) throws IOException {
    // Strip UTF-8 BOM if present
    BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8));
    return parse(reader);
  }

  public static List<List<String>> parse(@NonNull String csvContent) throws IOException {
    return parse(new BufferedReader(new StringReader(csvContent)));
  }

  public static List<List<String>> parse(@NonNull Reader reader) throws IOException {
    List<List<String>> rows = new ArrayList<>();
    List<String> currentRow = new ArrayList<>();
    StringBuilder currentField = new StringBuilder();

    boolean inQuotes = false;
    int prevChar = -1;
    int ch;

    // Check for UTF-8 BOM (0xFEFF) at start
    reader.mark(1);
    ch = reader.read();
    if (ch != 0xFEFF && ch != -1) {
      reader.reset();
    }

    while ((ch = reader.read()) != -1) {
      char c = (char) ch;

      if (inQuotes) {
        if (c == '"') {
          // Check if this is an escaped quote ("") or end of quote
          reader.mark(1);
          int nextCh = reader.read();
          if (nextCh == '"') {
            // Escaped quote
            currentField.append('"');
          } else {
            // End of quoted block
            inQuotes = false;
            reader.reset();
          }
        } else {
          currentField.append(c);
        }
      } else {
        if (c == '"') {
          inQuotes = true;
        } else if (c == ',') {
          currentRow.add(currentField.toString().trim());
          currentField.setLength(0);
        } else if (c == '\r') {
          // Ignore \r, will handle \n next
        } else if (c == '\n') {
          currentRow.add(currentField.toString().trim());
          currentField.setLength(0);
          if (!isRowEmpty(currentRow)) {
            rows.add(new ArrayList<>(currentRow));
          }
          currentRow.clear();
        } else {
          currentField.append(c);
        }
      }
      prevChar = ch;
    }

    // Flush last field and row if any content remains
    if (currentField.length() > 0 || !currentRow.isEmpty()) {
      currentRow.add(currentField.toString().trim());
      if (!isRowEmpty(currentRow)) {
        rows.add(currentRow);
      }
    }

    return rows;
  }

  private static boolean isRowEmpty(List<String> row) {
    if (row.isEmpty()) return true;
    for (String s : row) {
      if (s != null && !s.trim().isEmpty()) {
        return false;
      }
    }
    return true;
  }
}
