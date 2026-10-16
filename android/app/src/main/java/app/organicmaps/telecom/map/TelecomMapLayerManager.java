package app.organicmaps.telecom.map;

import android.content.Context;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import app.organicmaps.sdk.bookmarks.data.BookmarkCategory;
import app.organicmaps.sdk.bookmarks.data.BookmarkManager;
import app.organicmaps.sdk.util.concurrency.UiThread;
import app.organicmaps.telecom.db.TelecomDatabaseHelper;
import app.organicmaps.telecom.model.TelecomSite;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * Manages the map visualization layer for Telecom Sites.
 * Integrates directly with CoMaps' native BookmarkManager to provide
 * hardware-accelerated, clustered, high-performance rendering of sites.
 */
public class TelecomMapLayerManager {

  public static final String CATEGORY_NAME = "Telecom Sites";
  public static final String CATEGORY_ROUTE_NAME = "Planned Visit Route";
  private static final String PREFS_NAME = "telecom_prefs";
  private static final String KEY_LAYER_VISIBLE = "telecom_layer_visible";

  private static TelecomMapLayerManager sInstance;
  private boolean mIsLayerVisibleMemory = true; // Enabled by default
  private long mCategoryIdMemory = -1L;
  private long mRouteCategoryIdMemory = -1L;

  public interface OnSyncCompleteListener {
    void onSyncComplete(boolean success, int count);
  }

  public static synchronized TelecomMapLayerManager getInstance() {
    if (sInstance == null) {
      sInstance = new TelecomMapLayerManager();
    }
    return sInstance;
  }

  public boolean isLayerVisible(@NonNull Context context) {
    long catId = getTelecomCategoryId();
    if (catId != -1) {
      return BookmarkManager.INSTANCE.isVisible(catId);
    }
    return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        .getBoolean(KEY_LAYER_VISIBLE, true); // Enabled by default
  }

  public boolean isLayerVisible() {
    long catId = getTelecomCategoryId();
    if (catId != -1) {
      return BookmarkManager.INSTANCE.isVisible(catId);
    }
    return mIsLayerVisibleMemory;
  }

  public void setLayerVisible(@NonNull Context context, boolean visible) {
    mIsLayerVisibleMemory = visible;
    context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        .edit()
        .putBoolean(KEY_LAYER_VISIBLE, visible)
        .apply();
    UiThread.run(() -> {
      long catId = getTelecomCategoryId();
      if (catId != -1) {
        BookmarkManager.INSTANCE.setVisibility(catId, visible);
      }
    });
  }

  public void setLayerVisible(boolean visible) {
    mIsLayerVisibleMemory = visible;
    UiThread.run(() -> {
      long catId = getTelecomCategoryId();
      if (catId != -1) {
        BookmarkManager.INSTANCE.setVisibility(catId, visible);
      }
    });
  }

  public void toggleLayer(@NonNull Context context) {
    setLayerVisible(context, !isLayerVisible(context));
  }

  private boolean isTelecomCategoryName(@Nullable String name) {
    if (name == null) return false;
    return CATEGORY_NAME.equalsIgnoreCase(name) || name.startsWith(CATEGORY_NAME);
  }

  private boolean isRouteCategoryName(@Nullable String name) {
    if (name == null) return false;
    return CATEGORY_ROUTE_NAME.equalsIgnoreCase(name) || name.startsWith(CATEGORY_ROUTE_NAME);
  }

  /**
   * Finds the existing BookmarkCategory for Telecom Sites, or returns -1 if not found.
   */
  public long getTelecomCategoryId() {
    List<BookmarkCategory> categories = BookmarkManager.INSTANCE.getCategories();
    if (mCategoryIdMemory != -1) {
      for (BookmarkCategory cat : categories) {
        if (cat.getId() == mCategoryIdMemory) {
          return mCategoryIdMemory;
        }
      }
    }
    for (BookmarkCategory cat : categories) {
      if (isTelecomCategoryName(cat.getName())) {
        mCategoryIdMemory = cat.getId();
        return cat.getId();
      }
    }
    return -1L;
  }

  /**
   * Finds the existing BookmarkCategory for Planned Visit Route, or returns -1 if not found.
   */
  public long getRouteCategoryId() {
    List<BookmarkCategory> categories = BookmarkManager.INSTANCE.getCategories();
    if (mRouteCategoryIdMemory != -1) {
      for (BookmarkCategory cat : categories) {
        if (cat.getId() == mRouteCategoryIdMemory) {
          return mRouteCategoryIdMemory;
        }
      }
    }
    for (BookmarkCategory cat : categories) {
      if (isRouteCategoryName(cat.getName())) {
        mRouteCategoryIdMemory = cat.getId();
        return cat.getId();
      }
    }
    return -1L;
  }

  /**
   * Purges all existing categories matching the Telecom Sites name to prevent duplicate layers.
   */
  public void purgeExistingTelecomCategories() {
    List<BookmarkCategory> categories = BookmarkManager.INSTANCE.getCategories();
    for (BookmarkCategory cat : categories) {
      if (isTelecomCategoryName(cat.getName())) {
        BookmarkManager.INSTANCE.deleteCategory(cat.getId());
      }
    }
    mCategoryIdMemory = -1L;
  }

  /**
   * Purges all existing categories matching the Visit Route name.
   */
  public void purgeExistingRouteCategories() {
    List<BookmarkCategory> categories = BookmarkManager.INSTANCE.getCategories();
    for (BookmarkCategory cat : categories) {
      if (isRouteCategoryName(cat.getName())) {
        BookmarkManager.INSTANCE.deleteCategory(cat.getId());
      }
    }
    mRouteCategoryIdMemory = -1L;
  }

  /**
   * Synchronizes all telecom sites with valid coordinates from SQLite into the map's native category.
   * Auto-loads the bundled dataset if the database is empty.
   */
  public synchronized boolean syncSitesToMap(@NonNull Context context) {
    syncSitesToMap(context, null, null);
    return true;
  }

  /**
   * Synchronizes a given list of sites (e.g., filtered by radius) into the map's native category.
   */
  public synchronized void syncSitesToMap(@NonNull Context context,
                                          @Nullable List<TelecomSite> sitesToDisplay,
                                          @Nullable OnSyncCompleteListener listener) {
    new Thread(() -> {
      try {
        TelecomDatabaseHelper db = TelecomDatabaseHelper.getInstance(context);
        List<TelecomSite> sites = sitesToDisplay != null ? sitesToDisplay : db.getAllSitesWithCoordinates();
        if (sites.isEmpty()) {
          if (listener != null) {
            UiThread.run(() -> listener.onSyncComplete(false, 0));
          }
          return;
        }

        File cacheDir = context.getCacheDir();
        File kmlFile = new File(cacheDir, "telecom_sites_layer.kml");
        generateKml(sites, kmlFile);

        UiThread.run(() -> {
          try {
            purgeExistingTelecomCategories();

            BookmarkManager.BookmarksLoadingListener loadingListener = new BookmarkManager.BookmarksLoadingListener() {
              @Override
              public void onBookmarksLoadingFinished() {
                BookmarkManager.INSTANCE.removeLoadingListener(this);
                long newCatId = getTelecomCategoryId();
                if (newCatId != -1) {
                  BookmarkManager.INSTANCE.setVisibility(newCatId, true);
                }
                setLayerVisible(context, true);
                if (listener != null) {
                  listener.onSyncComplete(true, sites.size());
                }
              }

              @Override
              public void onBookmarksFileImportFailed() {
                BookmarkManager.INSTANCE.removeLoadingListener(this);
                if (listener != null) {
                  listener.onSyncComplete(false, 0);
                }
              }
            };

            BookmarkManager.INSTANCE.addLoadingListener(loadingListener);
            BookmarkManager.INSTANCE.loadBookmarksFile(kmlFile.getAbsolutePath(), true);
          } catch (Exception e) {
            if (listener != null) {
              listener.onSyncComplete(false, 0);
            }
          }
        });

      } catch (Exception e) {
        if (listener != null) {
          UiThread.run(() -> listener.onSyncComplete(false, 0));
        }
      }
    }).start();
  }

  /**
   * Synchronizes sites within a specific radius (in meters) from a center coordinate to the map.
   */
  public synchronized void syncRadiusSitesToMap(@NonNull Context context,
                                                double centerLat,
                                                double centerLon,
                                                double radiusMeters,
                                                @Nullable OnSyncCompleteListener listener) {
    new Thread(() -> {
      TelecomDatabaseHelper db = TelecomDatabaseHelper.getInstance(context);
      List<TelecomSite> sites = db.getSitesWithinRadius(centerLat, centerLon, radiusMeters);
      syncSitesToMap(context, sites, listener);
    }).start();
  }

  /**
   * Syncs ordered visit plan stops as a highlighted route layer on the map with numbered pins.
   */
  public synchronized boolean syncRouteToMap(@NonNull Context context,
                                             double startLat,
                                             double startLon,
                                             @NonNull String startLabel,
                                             @NonNull List<TelecomSite> stops,
                                             boolean hasReturn,
                                             double returnLat,
                                             double returnLon,
                                             @NonNull String returnLabel,
                                             @Nullable OnSyncCompleteListener listener) {
    new Thread(() -> {
      try {
        File cacheDir = context.getCacheDir();
        File routeKmlFile = new File(cacheDir, "telecom_visit_route.kml");
        generateRouteKml(startLat, startLon, startLabel, stops, hasReturn, returnLat, returnLon, returnLabel, routeKmlFile);

        UiThread.run(() -> {
          try {
            purgeExistingRouteCategories();

            BookmarkManager.BookmarksLoadingListener loadingListener = new BookmarkManager.BookmarksLoadingListener() {
              @Override
              public void onBookmarksLoadingFinished() {
                BookmarkManager.INSTANCE.removeLoadingListener(this);
                long newRouteCatId = getRouteCategoryId();
                if (newRouteCatId != -1) {
                  BookmarkManager.INSTANCE.setVisibility(newRouteCatId, true);
                }
                if (listener != null) {
                  listener.onSyncComplete(true, stops.size());
                }
              }

              @Override
              public void onBookmarksFileImportFailed() {
                BookmarkManager.INSTANCE.removeLoadingListener(this);
                if (listener != null) {
                  listener.onSyncComplete(false, 0);
                }
              }
            };

            BookmarkManager.INSTANCE.addLoadingListener(loadingListener);
            BookmarkManager.INSTANCE.loadBookmarksFile(routeKmlFile.getAbsolutePath(), true);
          } catch (Exception e) {
            if (listener != null) {
              listener.onSyncComplete(false, 0);
            }
          }
        });
      } catch (Exception e) {
        if (listener != null) {
          UiThread.run(() -> listener.onSyncComplete(false, 0));
        }
      }
    }).start();
    return true;
  }

  public synchronized boolean syncRouteToMap(@NonNull Context context,
                                             double startLat,
                                             double startLon,
                                             @NonNull String startLabel,
                                             @NonNull List<TelecomSite> stops,
                                             boolean hasReturn,
                                             double returnLat,
                                             double returnLon,
                                             @NonNull String returnLabel) {
    return syncRouteToMap(context, startLat, startLon, startLabel, stops, hasReturn, returnLat, returnLon, returnLabel, null);
  }

  public synchronized boolean syncRouteToMap(@NonNull Context context,
                                             double startLat,
                                             double startLon,
                                             @NonNull String startLabel,
                                             @NonNull List<TelecomSite> stops,
                                             boolean returnToStart) {
    return syncRouteToMap(context, startLat, startLon, startLabel, stops, returnToStart, startLat, startLon, startLabel);
  }

  private void generateKml(List<TelecomSite> sites, File file) throws Exception {
    try (Writer writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(file), StandardCharsets.UTF_8))) {
      writer.write("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
      writer.write("<kml xmlns=\"http://www.opengis.net/kml/2.2\">\n");
      writer.write("  <Document>\n");
      writer.write("    <name>" + escapeXml(CATEGORY_NAME) + "</name>\n");
      writer.write("    <visibility>1</visibility>\n");

      // Define default placemark style (Blue mark for telecom towers)
      writer.write("    <Style id=\"placemark-blue\">\n");
      writer.write("      <IconStyle>\n");
      writer.write("        <Icon>\n");
      writer.write("          <href>https://comaps.at/placemarks/placemark-blue.png</href>\n");
      writer.write("        </Icon>\n");
      writer.write("      </IconStyle>\n");
      writer.write("    </Style>\n");

      for (TelecomSite site : sites) {
        if (!site.hasValidCoords()) continue;
        writer.write("    <Placemark>\n");

        String displayName = site.getSiteId();
        if (site.hasDistance()) {
          displayName = "[" + site.getFormattedDistance() + "] " + displayName;
        }
        if (!site.getSiteName().isEmpty()) {
          displayName = displayName + " - " + site.getSiteName();
        }

        writer.write("      <name>" + escapeXml(displayName) + "</name>\n");
        writer.write("      <styleUrl>#placemark-blue</styleUrl>\n");
        writer.write("      <visibility>1</visibility>\n");
        writer.write("      <ExtendedData xmlns:mwm=\"https://comaps.app\">\n");
        writer.write("        <mwm:visibility>1</mwm:visibility>\n");
        writer.write("      </ExtendedData>\n");

        StringBuilder desc = new StringBuilder();
        if (site.hasDistance()) {
          desc.append("Distance: ").append(site.getFormattedDistance()).append("\n");
        }
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

  private void generateRouteKml(double startLat,
                                double startLon,
                                @NonNull String startLabel,
                                @NonNull List<TelecomSite> stops,
                                boolean hasReturn,
                                double returnLat,
                                double returnLon,
                                @NonNull String returnLabel,
                                File file) throws Exception {
    try (Writer writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(file), StandardCharsets.UTF_8))) {
      writer.write("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
      writer.write("<kml xmlns=\"http://www.opengis.net/kml/2.2\">\n");
      writer.write("  <Document>\n");
      writer.write("    <name>" + escapeXml(CATEGORY_ROUTE_NAME) + "</name>\n");
      writer.write("    <visibility>1</visibility>\n");

      writer.write("    <Style id=\"placemark-green\">\n");
      writer.write("      <IconStyle><Icon><href>https://comaps.at/placemarks/placemark-green.png</href></Icon></IconStyle>\n");
      writer.write("    </Style>\n");
      writer.write("    <Style id=\"placemark-orange\">\n");
      writer.write("      <IconStyle><Icon><href>https://comaps.at/placemarks/placemark-orange.png</href></Icon></IconStyle>\n");
      writer.write("    </Style>\n");
      writer.write("    <Style id=\"placemark-red\">\n");
      writer.write("      <IconStyle><Icon><href>https://comaps.at/placemarks/placemark-red.png</href></Icon></IconStyle>\n");
      writer.write("    </Style>\n");

      int stepNum = 1;
      // Start location
      if (startLat != 0.0 || startLon != 0.0) {
        writer.write("    <Placemark>\n");
        writer.write("      <name>" + escapeXml("[" + stepNum + "] Start: " + startLabel) + "</name>\n");
        writer.write("      <styleUrl>#placemark-green</styleUrl>\n");
        writer.write("      <visibility>1</visibility>\n");
        writer.write("      <ExtendedData xmlns:mwm=\"https://comaps.app\">\n");
        writer.write("        <mwm:visibility>1</mwm:visibility>\n");
        writer.write("      </ExtendedData>\n");
        writer.write("      <Point>\n");
        writer.write(String.format(java.util.Locale.US, "        <coordinates>%.6f,%.6f,0</coordinates>\n", startLon, startLat));
        writer.write("      </Point>\n");
        writer.write("    </Placemark>\n");
        stepNum++;
      }

      // Intermediate stops
      for (int i = 0; i < stops.size(); i++) {
        TelecomSite s = stops.get(i);
        if (!s.hasValidCoords()) continue;
        writer.write("    <Placemark>\n");
        writer.write("      <name>" + escapeXml("[" + stepNum + "] " + s.getSiteId() + (!s.getSiteName().isEmpty() ? " - " + s.getSiteName() : "")) + "</name>\n");
        writer.write("      <styleUrl>#placemark-orange</styleUrl>\n");
        writer.write("      <visibility>1</visibility>\n");
        writer.write("      <ExtendedData xmlns:mwm=\"https://comaps.app\">\n");
        writer.write("        <mwm:visibility>1</mwm:visibility>\n");
        writer.write("      </ExtendedData>\n");
        writer.write("      <description>" + escapeXml("Stop #" + (i + 1) + "\nTech: " + s.getTechnicianName() + " (" + s.getTechnicianPhone() + ")") + "</description>\n");
        writer.write("      <Point>\n");
        writer.write(String.format(java.util.Locale.US, "        <coordinates>%.6f,%.6f,0</coordinates>\n", s.getLongitude(), s.getLatitude()));
        writer.write("      </Point>\n");
        writer.write("    </Placemark>\n");
        stepNum++;
      }

      // Return leg
      if (hasReturn && (returnLat != 0.0 || returnLon != 0.0)) {
        writer.write("    <Placemark>\n");
        writer.write("      <name>" + escapeXml("[" + stepNum + "] Return: " + returnLabel) + "</name>\n");
        writer.write("      <styleUrl>#placemark-red</styleUrl>\n");
        writer.write("      <visibility>1</visibility>\n");
        writer.write("      <ExtendedData xmlns:mwm=\"https://comaps.app\">\n");
        writer.write("        <mwm:visibility>1</mwm:visibility>\n");
        writer.write("      </ExtendedData>\n");
        writer.write("      <Point>\n");
        writer.write(String.format(java.util.Locale.US, "        <coordinates>%.6f,%.6f,0</coordinates>\n", returnLon, returnLat));
        writer.write("      </Point>\n");
        writer.write("    </Placemark>\n");
      }

      // Route path line connecting start, stops, and return
      List<String> linePoints = new ArrayList<>();
      if (startLat != 0.0 || startLon != 0.0) {
        linePoints.add(String.format(java.util.Locale.US, "%.6f,%.6f,0", startLon, startLat));
      }
      for (TelecomSite s : stops) {
        if (s.hasValidCoords()) {
          linePoints.add(String.format(java.util.Locale.US, "%.6f,%.6f,0", s.getLongitude(), s.getLatitude()));
        }
      }
      if (hasReturn && (returnLat != 0.0 || returnLon != 0.0)) {
        linePoints.add(String.format(java.util.Locale.US, "%.6f,%.6f,0", returnLon, returnLat));
      }

      if (linePoints.size() >= 2) {
        writer.write("    <Placemark>\n");
        writer.write("      <name>Planned Route Path</name>\n");
        writer.write("      <visibility>1</visibility>\n");
        writer.write("      <ExtendedData xmlns:mwm=\"https://comaps.app\">\n");
        writer.write("        <mwm:visibility>1</mwm:visibility>\n");
        writer.write("      </ExtendedData>\n");
        writer.write("      <LineString>\n");
        writer.write("        <coordinates>\n");
        for (String pt : linePoints) {
          writer.write("          " + pt + "\n");
        }
        writer.write("        </coordinates>\n");
        writer.write("      </LineString>\n");
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
