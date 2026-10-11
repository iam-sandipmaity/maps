package app.organicmaps.telecom.planner;

import android.content.Context;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import app.organicmaps.telecom.db.TelecomDatabaseHelper;
import app.organicmaps.telecom.model.TelecomSite;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Manages the multi-site visit planning session, including stop selection,
 * sequence optimization, manual reordering, distance estimation, and persistence.
 */
public class VisitPlannerManager {

  private static final String PREFS_NAME = "telecom_visit_planner_prefs";
  private static final String KEY_RETURN_TO_START = "return_to_start";
  private static final String KEY_START_LAT = "start_lat";
  private static final String KEY_START_LON = "start_lon";
  private static final String KEY_START_LABEL = "start_label";
  private static final String KEY_HAS_CUSTOM_START = "has_custom_start";
  private static final String KEY_IS_GPS_START = "is_gps_start";

  private static VisitPlannerManager sInstance;

  private final List<TelecomSite> mPlannedSites = new ArrayList<>();
  private double mStartLat = 0.0;
  private double mStartLon = 0.0;
  private boolean mHasCustomStart = false;
  private boolean mIsGpsStart = false;
  private String mStartLabel = "First Selected Site";
  private boolean mReturnToStart = false;

  public static synchronized VisitPlannerManager getInstance(@NonNull Context context) {
    if (sInstance == null) {
      sInstance = new VisitPlannerManager();
      sInstance.loadPlanFromDb(context);
      sInstance.loadPrefs(context);
    }
    return sInstance;
  }

  private void loadPrefs(@NonNull Context context) {
    android.content.SharedPreferences sp = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    mReturnToStart = sp.getBoolean(KEY_RETURN_TO_START, false);
    mHasCustomStart = sp.getBoolean(KEY_HAS_CUSTOM_START, false);
    mIsGpsStart = sp.getBoolean(KEY_IS_GPS_START, false);
    mStartLat = Double.longBitsToDouble(sp.getLong(KEY_START_LAT, Double.doubleToLongBits(0.0)));
    mStartLon = Double.longBitsToDouble(sp.getLong(KEY_START_LON, Double.doubleToLongBits(0.0)));
    mStartLabel = sp.getString(KEY_START_LABEL, "First Selected Site");
  }

  public synchronized void savePrefs(@NonNull Context context) {
    context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        .edit()
        .putBoolean(KEY_RETURN_TO_START, mReturnToStart)
        .putBoolean(KEY_HAS_CUSTOM_START, mHasCustomStart)
        .putBoolean(KEY_IS_GPS_START, mIsGpsStart)
        .putLong(KEY_START_LAT, Double.doubleToLongBits(mStartLat))
        .putLong(KEY_START_LON, Double.doubleToLongBits(mStartLon))
        .putString(KEY_START_LABEL, mStartLabel)
        .apply();
  }

  public synchronized void loadPlanFromDb(@NonNull Context context) {
    mPlannedSites.clear();
    List<TelecomSite> saved = TelecomDatabaseHelper.getInstance(context).getVisitPlan();
    mPlannedSites.addAll(saved);
  }

  public synchronized void savePlanToDb(@NonNull Context context) {
    TelecomDatabaseHelper.getInstance(context).saveVisitPlan(mPlannedSites);
  }

  public synchronized List<TelecomSite> getPlannedSites() {
    return new ArrayList<>(mPlannedSites);
  }

  public synchronized boolean addSite(@NonNull Context context, @NonNull TelecomSite site) {
    for (TelecomSite existing : mPlannedSites) {
      if (existing.getSiteId().equalsIgnoreCase(site.getSiteId())) {
        return false; // Already in plan
      }
    }
    mPlannedSites.add(site);
    TelecomDatabaseHelper.getInstance(context).addToVisitPlan(site.getSiteId());
    return true;
  }

  public synchronized void removeSite(@NonNull Context context, @NonNull String siteId) {
    for (int i = 0; i < mPlannedSites.size(); i++) {
      if (mPlannedSites.get(i).getSiteId().equalsIgnoreCase(siteId)) {
        mPlannedSites.remove(i);
        TelecomDatabaseHelper.getInstance(context).removeFromVisitPlan(siteId);
        break;
      }
    }
  }

  public synchronized void clearPlan(@NonNull Context context) {
    mPlannedSites.clear();
    TelecomDatabaseHelper.getInstance(context).clearVisitPlan();
  }

  public synchronized void moveUp(int index, @NonNull Context context) {
    if (index > 0 && index < mPlannedSites.size()) {
      Collections.swap(mPlannedSites, index, index - 1);
      savePlanToDb(context);
    }
  }

  public synchronized void moveDown(int index, @NonNull Context context) {
    if (index >= 0 && index < mPlannedSites.size() - 1) {
      Collections.swap(mPlannedSites, index, index + 1);
      savePlanToDb(context);
    }
  }

  public synchronized void setCustomStartLocation(double lat, double lon, @NonNull String label) {
    mStartLat = lat;
    mStartLon = lon;
    mHasCustomStart = true;
    mIsGpsStart = false;
    mStartLabel = label;
  }

  public synchronized void setStartFromGps(double lat, double lon, @NonNull Context context) {
    mStartLat = lat;
    mStartLon = lon;
    mHasCustomStart = true;
    mIsGpsStart = true;
    mStartLabel = String.format(java.util.Locale.US, "My Location (%.4f, %.4f)", lat, lon);
    savePrefs(context);
  }

  public synchronized void resetStartLocation(@NonNull Context context) {
    mHasCustomStart = false;
    mIsGpsStart = false;
    mStartLat = 0.0;
    mStartLon = 0.0;
    mStartLabel = "First Selected Site";
    savePrefs(context);
  }

  public synchronized boolean isReturnToStart() {
    return mReturnToStart;
  }

  public synchronized void setReturnToStart(boolean returnToStart, @NonNull Context context) {
    mReturnToStart = returnToStart;
    savePrefs(context);
  }

  public String getStartLabel() {
    return mStartLabel;
  }

  public boolean hasCustomStart() {
    return mHasCustomStart;
  }

  public boolean isStartFromGps() {
    return mIsGpsStart;
  }

  public double getStartLat() {
    return mStartLat;
  }

  public double getStartLon() {
    return mStartLon;
  }

  /**
   * Calculates distance for a specific leg (from previous stop or start point to current stop).
   */
  public synchronized double calculateLegDistanceKm(int index) {
    if (index < 0 || index >= mPlannedSites.size()) return 0.0;
    TelecomSite current = mPlannedSites.get(index);
    if (!current.hasValidCoords()) return 0.0;

    double prevLat;
    double prevLon;
    if (index == 0) {
      if (mHasCustomStart && (mStartLat != 0.0 || mStartLon != 0.0)) {
        prevLat = mStartLat;
        prevLon = mStartLon;
      } else {
        return 0.0;
      }
    } else {
      TelecomSite prev = mPlannedSites.get(index - 1);
      if (!prev.hasValidCoords()) return 0.0;
      prevLat = prev.getLatitude();
      prevLon = prev.getLongitude();
    }
    return calculateHaversineDistanceKm(prevLat, prevLon, current.getLatitude(), current.getLongitude());
  }

  /**
   * Calculates distance of the return leg from the last site back to the start point.
   */
  public synchronized double calculateReturnLegDistanceKm() {
    if (!mReturnToStart || mPlannedSites.isEmpty()) return 0.0;
    TelecomSite last = mPlannedSites.get(mPlannedSites.size() - 1);
    if (!last.hasValidCoords()) return 0.0;

    double startLat = mHasCustomStart ? mStartLat : (mPlannedSites.get(0).hasValidCoords() ? mPlannedSites.get(0).getLatitude() : 0.0);
    double startLon = mHasCustomStart ? mStartLon : (mPlannedSites.get(0).hasValidCoords() ? mPlannedSites.get(0).getLongitude() : 0.0);

    if (startLat == 0.0 && startLon == 0.0) return 0.0;
    return calculateHaversineDistanceKm(last.getLatitude(), last.getLongitude(), startLat, startLon);
  }

  /**
   * Calculates total straight-line distance across the current planned sequence in kilometers,
   * including start leg and optional return-to-base leg.
   */
  public synchronized double calculateTotalDistanceKm() {
    if (mPlannedSites.isEmpty()) return 0.0;
    double total = 0.0;

    double currentLat = mHasCustomStart ? mStartLat : (mPlannedSites.get(0).hasValidCoords() ? mPlannedSites.get(0).getLatitude() : 0.0);
    double currentLon = mHasCustomStart ? mStartLon : (mPlannedSites.get(0).hasValidCoords() ? mPlannedSites.get(0).getLongitude() : 0.0);

    int startIndex = mHasCustomStart ? 0 : 1;

    for (int i = startIndex; i < mPlannedSites.size(); i++) {
      TelecomSite next = mPlannedSites.get(i);
      if (next.hasValidCoords()) {
        if (currentLat != 0.0 || currentLon != 0.0) {
          total += calculateHaversineDistanceKm(currentLat, currentLon, next.getLatitude(), next.getLongitude());
        }
        currentLat = next.getLatitude();
        currentLon = next.getLongitude();
      }
    }

    // Add return leg if enabled
    if (mReturnToStart && (mStartLat != 0.0 || mStartLon != 0.0 || (mPlannedSites.get(0).hasValidCoords()))) {
      double originLat = mHasCustomStart ? mStartLat : mPlannedSites.get(0).getLatitude();
      double originLon = mHasCustomStart ? mStartLon : mPlannedSites.get(0).getLongitude();
      if ((currentLat != 0.0 || currentLon != 0.0) && (originLat != 0.0 || originLon != 0.0)) {
        total += calculateHaversineDistanceKm(currentLat, currentLon, originLat, originLon);
      }
    }

    return total;
  }

  /**
   * Optimizes the visiting sequence using Nearest-Neighbor + 2-Opt heuristic,
   * factoring in starting location and optional return-to-base journey.
   */
  public synchronized void optimizeSequence(@NonNull Context context) {
    if (mPlannedSites.size() <= 1) return;

    List<TelecomSite> unvisited = new ArrayList<>(mPlannedSites);
    List<TelecomSite> optimized = new ArrayList<>();

    double originLat = mHasCustomStart ? mStartLat : 0.0;
    double originLon = mHasCustomStart ? mStartLon : 0.0;
    double currentLat = originLat;
    double currentLon = originLon;

    // If no custom start, anchor to the first site
    if (!mHasCustomStart) {
      TelecomSite first = unvisited.remove(0);
      optimized.add(first);
      if (first.hasValidCoords()) {
        currentLat = first.getLatitude();
        currentLon = first.getLongitude();
        originLat = currentLat;
        originLon = currentLon;
      }
    }

    // Nearest Neighbor greedy traversal
    while (!unvisited.isEmpty()) {
      int bestIdx = -1;
      double minDistance = Double.MAX_VALUE;

      for (int i = 0; i < unvisited.size(); i++) {
        TelecomSite candidate = unvisited.get(i);
        if (candidate.hasValidCoords()) {
          double dist = calculateHaversineDistanceKm(currentLat, currentLon, candidate.getLatitude(), candidate.getLongitude());
          if (dist < minDistance) {
            minDistance = dist;
            bestIdx = i;
          }
        } else {
          if (bestIdx == -1) bestIdx = i;
        }
      }

      if (bestIdx == -1) bestIdx = 0;
      TelecomSite next = unvisited.remove(bestIdx);
      optimized.add(next);
      if (next.hasValidCoords()) {
        currentLat = next.getLatitude();
        currentLon = next.getLongitude();
      }
    }

    // Apply 2-Opt improvements
    apply2Opt(optimized, originLat, originLon, mReturnToStart);

    mPlannedSites.clear();
    mPlannedSites.addAll(optimized);
    savePlanToDb(context);
    savePrefs(context);
  }

  private void apply2Opt(List<TelecomSite> list, double originLat, double originLon, boolean closedLoop) {
    int n = list.size();
    if (n < 3) return;

    boolean improved = true;
    int iterations = 0;

    while (improved && iterations < 50) {
      improved = false;
      iterations++;
      for (int i = 0; i < n - 1; i++) {
        for (int k = i + 1; k < n; k++) {
          double dBefore = calculateSubtourCost(list, i, k, originLat, originLon, closedLoop, false);
          double dAfter = calculateSubtourCost(list, i, k, originLat, originLon, closedLoop, true);

          if (dAfter < dBefore - 0.001) {
            Collections.reverse(list.subList(i, k + 1));
            improved = true;
          }
        }
      }
    }
  }

  private double calculateSubtourCost(List<TelecomSite> list, int i, int k,
                                      double originLat, double originLon,
                                      boolean closedLoop, boolean reversed) {
    int n = list.size();
    TelecomSite a = (i == 0) ? null : list.get(i - 1);
    TelecomSite b = reversed ? list.get(k) : list.get(i);
    TelecomSite c = reversed ? list.get(i) : list.get(k);
    TelecomSite d = (k == n - 1) ? null : list.get(k + 1);

    double cost = 0.0;
    // Link from predecessor to b
    if (a != null && a.hasValidCoords() && b.hasValidCoords()) {
      cost += calculateHaversineDistanceKm(a.getLatitude(), a.getLongitude(), b.getLatitude(), b.getLongitude());
    } else if (a == null && b.hasValidCoords() && (originLat != 0.0 || originLon != 0.0)) {
      cost += calculateHaversineDistanceKm(originLat, originLon, b.getLatitude(), b.getLongitude());
    }

    // Link from c to successor
    if (d != null && c.hasValidCoords() && d.hasValidCoords()) {
      cost += calculateHaversineDistanceKm(c.getLatitude(), c.getLongitude(), d.getLatitude(), d.getLongitude());
    } else if (d == null && c.hasValidCoords() && closedLoop && (originLat != 0.0 || originLon != 0.0)) {
      cost += calculateHaversineDistanceKm(c.getLatitude(), c.getLongitude(), originLat, originLon);
    }

    return cost;
  }

  /**
   * Calculates great-circle distance between two decimal coordinates in kilometers.
   */
  public static double calculateHaversineDistanceKm(double lat1, double lon1, double lat2, double lon2) {
    final double R = 6371.0; // Earth radius in km
    double dLat = Math.toRadians(lat2 - lat1);
    double dLon = Math.toRadians(lon2 - lon1);
    double rLat1 = Math.toRadians(lat1);
    double rLat2 = Math.toRadians(lat2);

    double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
        + Math.cos(rLat1) * Math.cos(rLat2) * Math.sin(dLon / 2) * Math.sin(dLon / 2);
    double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
    return R * c;
  }
}
