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

  private static VisitPlannerManager sInstance;

  private final List<TelecomSite> mPlannedSites = new ArrayList<>();
  private double mStartLat = 0.0;
  private double mStartLon = 0.0;
  private boolean mHasCustomStart = false;
  private String mStartLabel = "Default (First Site)";

  public static synchronized VisitPlannerManager getInstance(@NonNull Context context) {
    if (sInstance == null) {
      sInstance = new VisitPlannerManager();
      sInstance.loadPlanFromDb(context);
    }
    return sInstance;
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
    mStartLabel = label;
  }

  public synchronized void resetStartLocation() {
    mHasCustomStart = false;
    mStartLabel = "Default (First Site)";
  }

  public String getStartLabel() {
    return mStartLabel;
  }

  public boolean hasCustomStart() {
    return mHasCustomStart;
  }

  /**
   * Calculates total straight-line distance across the current planned sequence in kilometers.
   */
  public synchronized double calculateTotalDistanceKm() {
    if (mPlannedSites.size() < 2) return 0.0;
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
    return total;
  }

  /**
   * Optimizes the visiting sequence using Nearest-Neighbor + 2-Opt heuristic.
   * NOTE: This calculates an approximate sequence based on great-circle geospatial distance.
   */
  public synchronized void optimizeSequence(@NonNull Context context) {
    if (mPlannedSites.size() <= 2) return;

    List<TelecomSite> unvisited = new ArrayList<>(mPlannedSites);
    List<TelecomSite> optimized = new ArrayList<>();

    double currentLat = mHasCustomStart ? mStartLat : 0.0;
    double currentLon = mHasCustomStart ? mStartLon : 0.0;

    // If no custom start, start from the first site in the original list
    if (!mHasCustomStart) {
      TelecomSite first = unvisited.remove(0);
      optimized.add(first);
      if (first.hasValidCoords()) {
        currentLat = first.getLatitude();
        currentLon = first.getLongitude();
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
          // If candidate has no coords, keep for the end
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

    // Apply 2-opt improvements on coordinate-valid sites
    apply2Opt(optimized);

    mPlannedSites.clear();
    mPlannedSites.addAll(optimized);
    savePlanToDb(context);
  }

  private void apply2Opt(List<TelecomSite> list) {
    int n = list.size();
    if (n < 4) return;

    boolean improved = true;
    int iterations = 0;

    while (improved && iterations < 50) {
      improved = false;
      iterations++;
      for (int i = 1; i < n - 2; i++) {
        for (int k = i + 1; k < n - 1; k++) {
          TelecomSite a = list.get(i - 1);
          TelecomSite b = list.get(i);
          TelecomSite c = list.get(k);
          TelecomSite d = list.get(k + 1);

          if (!a.hasValidCoords() || !b.hasValidCoords() || !c.hasValidCoords() || !d.hasValidCoords()) {
            continue;
          }

          double currentDist = calculateHaversineDistanceKm(a.getLatitude(), a.getLongitude(), b.getLatitude(), b.getLongitude())
                             + calculateHaversineDistanceKm(c.getLatitude(), c.getLongitude(), d.getLatitude(), d.getLongitude());
          double newDist = calculateHaversineDistanceKm(a.getLatitude(), a.getLongitude(), c.getLatitude(), c.getLongitude())
                         + calculateHaversineDistanceKm(b.getLatitude(), b.getLongitude(), d.getLatitude(), d.getLongitude());

          if (newDist < currentDist - 0.001) {
            // Reverse from i to k
            Collections.reverse(list.subList(i, k + 1));
            improved = true;
          }
        }
      }
    }
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
