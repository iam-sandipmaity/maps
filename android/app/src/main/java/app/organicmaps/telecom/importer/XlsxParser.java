package app.organicmaps.telecom.importer;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserFactory;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * Pure Java streaming XLSX (OpenXML) parser.
 * Requires zero external heavy libraries (such as Apache POI) and runs natively on Android.
 * Supports shared strings table, cell coordinate mapping, and composite 2-row header consolidation.
 */
public class XlsxParser {

  @NonNull
  public static List<List<String>> parse(@NonNull InputStream inputStream, @NonNull File cacheDir) throws IOException {
    File tempFile = File.createTempFile("telecom_import_", ".xlsx", cacheDir);
    try {
      try (FileOutputStream fos = new FileOutputStream(tempFile)) {
        byte[] buffer = new byte[8192];
        int read;
        while ((read = inputStream.read(buffer)) != -1) {
          fos.write(buffer, 0, read);
        }
      }
      return parse(tempFile);
    } finally {
      if (tempFile.exists()) {
        //noinspection ResultOfMethodCallIgnored
        tempFile.delete();
      }
    }
  }

  @NonNull
  public static List<List<String>> parse(@NonNull File file) throws IOException {
    try (ZipFile zipFile = new ZipFile(file)) {
      List<String> sharedStrings = parseSharedStrings(zipFile);
      ZipEntry sheetEntry = findFirstSheetEntry(zipFile);
      if (sheetEntry == null) {
        throw new IOException("No valid worksheet found in XLSX archive");
      }
      return parseSheet(zipFile, sheetEntry, sharedStrings);
    } catch (Exception e) {
      if (e instanceof IOException) {
        throw (IOException) e;
      }
      throw new IOException("Failed to parse XLSX workbook: " + e.getMessage(), e);
    }
  }

  @Nullable
  private static ZipEntry findFirstSheetEntry(@NonNull ZipFile zipFile) {
    // Prefer sheet1.xml or first sheet found in xl/worksheets/
    ZipEntry entry = zipFile.getEntry("xl/worksheets/sheet1.xml");
    if (entry != null) {
      return entry;
    }
    Enumeration<? extends ZipEntry> entries = zipFile.entries();
    while (entries.hasMoreElements()) {
      ZipEntry e = entries.nextElement();
      if (e.getName().startsWith("xl/worksheets/sheet") && e.getName().endsWith(".xml")) {
        return e;
      }
    }
    return null;
  }

  @NonNull
  private static List<String> parseSharedStrings(@NonNull ZipFile zipFile) {
    ZipEntry sharedStringsEntry = zipFile.getEntry("xl/sharedStrings.xml");
    if (sharedStringsEntry == null) {
      return Collections.emptyList();
    }

    List<String> strings = new ArrayList<>();
    try (InputStream is = zipFile.getInputStream(sharedStringsEntry)) {
      XmlPullParserFactory factory = XmlPullParserFactory.newInstance();
      factory.setNamespaceAware(false);
      XmlPullParser parser = factory.newPullParser();
      parser.setInput(new InputStreamReader(is, StandardCharsets.UTF_8));

      StringBuilder currentString = new StringBuilder();
      boolean insideSi = false;
      boolean insideT = false;

      int eventType = parser.getEventType();
      while (eventType != XmlPullParser.END_DOCUMENT) {
        String name = parser.getName();
        if (eventType == XmlPullParser.START_TAG) {
          if ("si".equalsIgnoreCase(name)) {
            insideSi = true;
            currentString.setLength(0);
          } else if (insideSi && "t".equalsIgnoreCase(name)) {
            insideT = true;
          }
        } else if (eventType == XmlPullParser.TEXT) {
          if (insideSi && insideT) {
            currentString.append(parser.getText());
          }
        } else if (eventType == XmlPullParser.END_TAG) {
          if ("t".equalsIgnoreCase(name)) {
            insideT = false;
          } else if ("si".equalsIgnoreCase(name)) {
            insideSi = false;
            strings.add(currentString.toString());
          }
        }
        eventType = parser.next();
      }
    } catch (Exception ignored) {
      // Return whatever shared strings were parsed
    }
    return strings;
  }

  @NonNull
  private static List<List<String>> parseSheet(@NonNull ZipFile zipFile,
                                               @NonNull ZipEntry sheetEntry,
                                               @NonNull List<String> sharedStrings) throws Exception {
    List<List<String>> rawRows = new ArrayList<>();
    try (InputStream is = zipFile.getInputStream(sheetEntry)) {
      XmlPullParserFactory factory = XmlPullParserFactory.newInstance();
      factory.setNamespaceAware(false);
      XmlPullParser parser = factory.newPullParser();
      parser.setInput(new InputStreamReader(is, StandardCharsets.UTF_8));

      Map<Integer, String> currentRowCells = new HashMap<>();
      int maxColInRow = -1;
      int maxColsOverall = 0;
      String currentCellRef = null;
      String currentCellType = null;
      StringBuilder currentVal = new StringBuilder();
      boolean insideV = false;
      boolean insideIs = false;
      boolean insideT = false;

      int eventType = parser.getEventType();
      while (eventType != XmlPullParser.END_DOCUMENT) {
        String name = parser.getName();
        if (eventType == XmlPullParser.START_TAG) {
          if ("row".equalsIgnoreCase(name)) {
            currentRowCells.clear();
            maxColInRow = -1;
          } else if ("c".equalsIgnoreCase(name)) {
            currentCellRef = parser.getAttributeValue(null, "r");
            currentCellType = parser.getAttributeValue(null, "t");
            currentVal.setLength(0);
          } else if ("v".equalsIgnoreCase(name)) {
            insideV = true;
          } else if ("is".equalsIgnoreCase(name)) {
            insideIs = true;
          } else if (insideIs && "t".equalsIgnoreCase(name)) {
            insideT = true;
          }
        } else if (eventType == XmlPullParser.TEXT) {
          if (insideV || insideT) {
            currentVal.append(parser.getText());
          }
        } else if (eventType == XmlPullParser.END_TAG) {
          if ("v".equalsIgnoreCase(name)) {
            insideV = false;
          } else if ("t".equalsIgnoreCase(name)) {
            insideT = false;
          } else if ("is".equalsIgnoreCase(name)) {
            insideIs = false;
          } else if ("c".equalsIgnoreCase(name)) {
            if (currentCellRef != null) {
              int colIdx = columnRefToIndex(currentCellRef);
              String textVal = currentVal.toString().trim();
              if ("s".equalsIgnoreCase(currentCellType) && !textVal.isEmpty()) {
                try {
                  int sIndex = Integer.parseInt(textVal);
                  if (sIndex >= 0 && sIndex < sharedStrings.size()) {
                    textVal = sharedStrings.get(sIndex);
                  }
                } catch (NumberFormatException ignored) {}
              }
              currentRowCells.put(colIdx, textVal);
              if (colIdx > maxColInRow) {
                maxColInRow = colIdx;
              }
            }
          } else if ("row".equalsIgnoreCase(name)) {
            if (!currentRowCells.isEmpty()) {
              if (maxColInRow + 1 > maxColsOverall) {
                maxColsOverall = maxColInRow + 1;
              }
              List<String> rowList = new ArrayList<>(maxColInRow + 1);
              for (int c = 0; c <= maxColInRow; c++) {
                String val = currentRowCells.get(c);
                rowList.add(val != null ? val : "");
              }
              rawRows.add(rowList);
            }
          }
        }
        eventType = parser.next();
      }

      // Pad all rows to uniform column count
      for (List<String> r : rawRows) {
        while (r.size() < maxColsOverall) {
          r.add("");
        }
      }
    }

    if (rawRows.isEmpty()) {
      return Collections.emptyList();
    }

    // Check if row 1 (0-indexed) has subheaders that should be merged into composite headers
    return consolidateHeaders(rawRows);
  }

  @NonNull
  private static List<List<String>> consolidateHeaders(@NonNull List<List<String>> rawRows) {
    if (rawRows.size() < 2) {
      return rawRows;
    }

    List<String> row0 = rawRows.get(0);
    List<String> row1 = rawRows.get(1);

    // Heuristic: Check if row 1 contains typical subheader terms (e.g. "make", "rating", "model", "status", "count")
    boolean isComposite = false;
    for (String s : row1) {
      String lower = s.toLowerCase();
      if (lower.contains("make") || lower.contains("rating") || lower.contains("model") ||
          lower.contains("backup") || lower.contains("count") || lower.contains("status")) {
        isComposite = true;
        break;
      }
    }

    if (!isComposite) {
      return rawRows;
    }

    List<String> mergedHeader = new ArrayList<>(row0.size());
    for (int i = 0; i < row0.size(); i++) {
      String h1 = i < row0.size() ? row0.get(i).trim() : "";
      String h2 = i < row1.size() ? row1.get(i).trim() : "";
      if (!h2.isEmpty() && !h1.isEmpty() && !h1.toLowerCase().contains(h2.toLowerCase())) {
        mergedHeader.add(h1 + " " + h2);
      } else if (!h2.isEmpty()) {
        mergedHeader.add(h2);
      } else {
        mergedHeader.add(h1);
      }
    }

    List<List<String>> consolidated = new ArrayList<>(rawRows.size());
    consolidated.add(mergedHeader);
    for (int i = 2; i < rawRows.size(); i++) {
      consolidated.add(rawRows.get(i));
    }
    return consolidated;
  }

  public static int columnRefToIndex(@NonNull String cellRef) {
    int result = 0;
    for (int i = 0; i < cellRef.length(); i++) {
      char c = cellRef.charAt(i);
      if (c >= 'A' && c <= 'Z') {
        result = result * 26 + (c - 'A' + 1);
      } else if (c >= 'a' && c <= 'z') {
        result = result * 26 + (c - 'a' + 1);
      } else {
        break;
      }
    }
    return Math.max(0, result - 1);
  }
}
