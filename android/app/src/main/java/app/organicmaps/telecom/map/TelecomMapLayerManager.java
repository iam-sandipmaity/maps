package app.organicmaps.telecom.map;

import android.content.Context;
import androidx.annotation.NonNull;
import app.organicmaps.sdk.bookmarks.data.BookmarkCategory;
import app.organicmaps.sdk.bookmarks.data.BookmarkManager;
import app.organicmaps.telecom.db.TelecomDatabaseHelper;
import app.organicmaps.telecom.model.TelecomSite;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * Manages the map visualization layer for Telecom Sites.
 * Integrates directly with CoMaps' native BookmarkManager to provide
 * hardware-accelerated, clustered, high-performance rendering of sites.
 */
public class TelecomMapLayerManager {

  public static final String CATEGORY_NAME = "Telecom Sites";

  private static TelecomMapLayerManager sInstance;
  private boolean mIsLayerVisibleMemory = true;
  private long mCategoryIdMemory = -1L;

  public static synchronized TelecomMapLayerManager getInstance() {
    if (sInstance == null) {
      sInstance = new TelecomMapLayerManager();
    }
    return sInstance;
  }

  public boolean isLayerVisible() {
    long catId = getTelecomCategoryId();
    if (catId != -1) {
      return BookmarkManager.INSTANCE.isVisible(catId);
    }
    return mIsLayerVisibleMemory;
  }

  public void setLayerVisible(boolean visible) {
    mIsLayerVisibleMemory = visible;
    long catId = getTelecomCategoryId();
    if (catId != -1) {
      BookmarkManager.INSTANCE.setVisibility(catId, visible);
    }
  }

  public void toggleLayer() {
    setLayerVisible(!isLayerVisible());
  }

  /**
   * Finds the existing BookmarkCategory for Telecom Sites, or returns -1 if not found.
   */
  public long getTelecomCategoryId() {
    if (mCategoryIdMemory != -1) {
      for (BookmarkCategory cat : BookmarkManager.INSTANCE.getCategories()) {
        if (cat.getId() == mCategoryIdMemory) {
          return mCategoryIdMemory;
        }
      }
    }
    for (BookmarkCategory cat : BookmarkManager.INSTANCE.getCategories()) {
      if (CATEGORY_NAME.equalsIgnoreCase(cat.getName())) {
        mCategoryIdMemory = cat.getId();
        return cat.getId();
      }
    }
    return -1L;
  }

  /**
   * Synchronizes all telecom sites with valid coordinates from SQLite into the map's native category.
   * Exports to a temporary KML file and imports via BookmarkManager.
   */
  public synchronized boolean syncSitesToMap(@NonNull Context context) {
    try {
      TelecomDatabaseHelper db = TelecomDatabaseHelper.getInstance(context);
      List<TelecomSite> sites = db.getAllSitesWithCoordinates();
      if (sites.isEmpty()) {
        return false;
      }

      // Check if old telecom category exists, delete it first to ensure fresh sync
      long oldCatId = getTelecomCategoryId();
      if (oldCatId != -1) {
        BookmarkManager.INSTANCE.deleteCategory(oldCatId);
        mCategoryIdMemory = -1L;
      }

      // Generate KML file
      File cacheDir = context.getCacheDir();
      File kmlFile = new File(cacheDir, "telecom_sites_layer.kml");
      generateKml(sites, kmlFile);

      // Load into CoMaps native Drape engine
      BookmarkManager.INSTANCE.loadBookmarksFile(kmlFile.getAbsolutePath(), false);

      // Restore layer visibility state
      long newCatId = getTelecomCategoryId();
      if (newCatId != -1) {
        BookmarkManager.INSTANCE.setVisibility(newCatId, mIsLayerVisibleMemory);
      }

      return true;
    } catch (Exception e) {
      return false;
    }
  }

  private void generateKml(List<TelecomSite> sites, File file) throws Exception {
    try (Writer writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(file), StandardCharsets.UTF_8))) {
      writer.write("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
      writer.write("<kml xmlns=\"http://www.opengis.net/kml/2.2\">\n");
      writer.write("  <Document>\n");
      writer.write("    <name>" + escapeXml(CATEGORY_NAME) + "</name>\n");

      for (TelecomSite site : sites) {
        if (!site.hasValidCoords()) continue;
        writer.write("    <Placemark>\n");
        writer.write("      <name>" + escapeXml(site.getSiteId()) + "</name>\n");

        StringBuilder desc = new StringBuilder();
        if (!site.getSiteName().isEmpty()) {
          desc.append("Site: ").append(site.getSiteName()).append("\n");
        }
        if (!site.getLocation().isEmpty()) {
          desc.append("JC: ").append(site.getLocation()).append("\n");
        }
        if (!site.getTechnicianName().isEmpty()) {
          desc.append("Technician: ").append(site.getTechnicianName());
          if (!site.getTechnicianPhone().isEmpty()) {
            desc.append(" (").append(site.getTechnicianPhone()).append(")");
          }
          desc.append("\n");
        }
        if (!site.getPowerStatus().isEmpty()) {
          desc.append("Power: ").append(site.getPowerStatus()).append("\n");
        }
        if (!site.getNotes().isEmpty()) {
          desc.append("Notes: ").append(site.getNotes()).append("\n");
        }

        writer.write("      <description>" + escapeXml(desc.toString()) + "</description>\n");
        writer.write("      <Point>\n");
        writer.write(String.format(java.util.Locale.US, "        <coordinates>%.6f,%.6f,0</coordinates>\n",
            site.getLongitude(), site.getLatitude()));
        writer.write("      </Point>\n");
        writer.write("    </Placemark>\n");
      }

      writer.write("  </Document>\n");
      writer.write("</kml>\n");
      writer.flush();
    }
  }

  private String escapeXml(String s) {
    if (s == null) return "";
    return s.replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;")
            .replace("'", "&apos;");
  }
}
