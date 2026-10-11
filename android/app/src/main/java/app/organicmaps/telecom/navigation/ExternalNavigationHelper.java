package app.organicmaps.telecom.navigation;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import app.organicmaps.MwmActivity;
import app.organicmaps.MwmApplication;
import app.organicmaps.R;
import app.organicmaps.sdk.Framework;
import app.organicmaps.sdk.bookmarks.data.FeatureId;
import app.organicmaps.sdk.bookmarks.data.MapObject;
import app.organicmaps.sdk.routing.RouteMarkData;
import app.organicmaps.sdk.routing.RouteMarkType;
import app.organicmaps.sdk.routing.RoutingController;
import app.organicmaps.telecom.map.TelecomMapLayerManager;
import app.organicmaps.telecom.model.TelecomSite;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class ExternalNavigationHelper {

  // Supported Navigation Targets
  public enum NavApp {
    COMAPS,
    GOOGLE_MAPS,
    MAPPLS,
    SYSTEM_CHOOSER
  }

  // 1. Copy Site ID
  public static void copySiteId(@NonNull Context context, @NonNull TelecomSite site) {
    copyToClipboard(context, "Site ID", site.getSiteId(), "Site ID copied: " + site.getSiteId());
  }

  // 2. Copy Latitude
  public static void copyLatitude(@NonNull Context context, @NonNull TelecomSite site) {
    if (!site.hasValidCoords()) {
      showToast(context, "Site has no valid coordinates");
      return;
    }
    String latStr = String.format(Locale.US, "%.6f", site.getLatitude());
    copyToClipboard(context, "Latitude", latStr, "Latitude copied: " + latStr);
  }

  // 3. Copy Longitude
  public static void copyLongitude(@NonNull Context context, @NonNull TelecomSite site) {
    if (!site.hasValidCoords()) {
      showToast(context, "Site has no valid coordinates");
      return;
    }
    String lonStr = String.format(Locale.US, "%.6f", site.getLongitude());
    copyToClipboard(context, "Longitude", lonStr, "Longitude copied: " + lonStr);
  }

  // 4. Copy Lat, Lon Pair
  public static void copyCoordinates(@NonNull Context context, @NonNull TelecomSite site) {
    if (!site.hasValidCoords()) {
      showToast(context, "Site has no valid coordinates");
      return;
    }
    String coords = site.getFormattedCoordinates();
    copyToClipboard(context, "Coordinates", coords, "Coordinates copied: " + coords);
  }

  // 5. Copy Google Maps compatible string / link
  public static void copyGoogleMapsUrl(@NonNull Context context, @NonNull TelecomSite site) {
    if (!site.hasValidCoords()) {
      showToast(context, "Site has no valid coordinates");
      return;
    }
    String url = String.format(Locale.US, "https://www.google.com/maps/search/?api=1&query=%.6f,%.6f",
        site.getLatitude(), site.getLongitude());
    copyToClipboard(context, "Google Maps Link", url, "Google Maps link copied");
  }

  // Copy phone number
  public static void copyPhoneNumber(@NonNull Context context, @NonNull String label, @NonNull String phone) {
    if (phone.isEmpty()) {
      showToast(context, "No phone number available");
      return;
    }
    copyToClipboard(context, label, phone, label + " copied: " + phone);
  }

  // Dial phone number
  public static void dialPhoneNumber(@NonNull Context context, @Nullable String phone) {
    if (phone == null || phone.trim().isEmpty()) {
      showToast(context, "Phone number is not available");
      return;
    }
    try {
      Intent intent = new Intent(Intent.ACTION_DIAL);
      intent.setData(Uri.parse("tel:" + phone.trim()));
      intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
      context.startActivity(intent);
    } catch (Exception e) {
      showToast(context, "Unable to initiate call: " + e.getMessage());
    }
  }

  /**
   * Opens site location in CoMaps internal map view.
   */
  public static void openInCoMaps(@NonNull Context context, @NonNull TelecomSite site) {
    if (!site.hasValidCoords()) {
      showToast(context, "Site has no valid coordinates to display");
      return;
    }
    try {
      // Focus map directly using native framework if possible, or start MwmActivity
      Intent intent = new Intent(context, MwmActivity.class);
      intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
      // geo:lat,lon?z=17
      String uriStr = String.format(Locale.US, "geo:%.6f,%.6f?z=17", site.getLatitude(), site.getLongitude());
      intent.setData(Uri.parse(uriStr));
      context.startActivity(intent);
    } catch (Exception e) {
      showToast(context, "Error opening CoMaps: " + e.getMessage());
    }
  }

  /**
   * Launches navigation or map view in Google Maps.
   * Uses turn-by-turn navigation intent first, falls back to geo: or browser.
   */
  public static void openInGoogleMaps(@NonNull Context context, @NonNull TelecomSite site, boolean turnByTurn) {
    if (!site.hasValidCoords()) {
      showToast(context, "Site has no valid coordinates");
      return;
    }
    double lat = site.getLatitude();
    double lon = site.getLongitude();

    if (turnByTurn) {
      Uri navUri = Uri.parse(String.format(Locale.US, "google.navigation:q=%.6f,%.6f&mode=d", lat, lon));
      Intent navIntent = new Intent(Intent.ACTION_VIEW, navUri);
      navIntent.setPackage("com.google.android.apps.maps");
      navIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
      if (isIntentResolvable(context, navIntent)) {
        context.startActivity(navIntent);
        return;
      }
    }

    // Fallback: Google Maps search intent
    Uri gmmIntentUri = Uri.parse(String.format(Locale.US, "geo:%.6f,%.6f?q=%.6f,%.6f(%s)",
        lat, lon, lat, lon, Uri.encode(site.getSiteId() + " " + site.getSiteName())));
    Intent mapIntent = new Intent(Intent.ACTION_VIEW, gmmIntentUri);
    mapIntent.setPackage("com.google.android.apps.maps");
    mapIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
    if (isIntentResolvable(context, mapIntent)) {
      context.startActivity(mapIntent);
      return;
    }

    // Web Fallback if Google Maps app is not installed
    Uri webUri = Uri.parse(String.format(Locale.US, "https://www.google.com/maps/dir/?api=1&destination=%.6f,%.6f", lat, lon));
    Intent webIntent = new Intent(Intent.ACTION_VIEW, webUri);
    webIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
    if (isIntentResolvable(context, webIntent)) {
      context.startActivity(webIntent);
    } else {
      copyCoordinates(context, site);
      showToast(context, "Google Maps not installed. Coordinates copied to clipboard.");
    }
  }

  /**
   * Opens site location in Mappls (MapmyIndia) if installed.
   */
  public static void openInMappls(@NonNull Context context, @NonNull TelecomSite site) {
    if (!site.hasValidCoords()) {
      showToast(context, "Site has no valid coordinates");
      return;
    }
    double lat = site.getLatitude();
    double lon = site.getLongitude();

    // Mappls navigation URI scheme
    Uri mapplsNavUri = Uri.parse(String.format(Locale.US, "mappls://nav?dest=%.6f,%.6f", lat, lon));
    Intent mapplsIntent = new Intent(Intent.ACTION_VIEW, mapplsNavUri);
    mapplsIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
    if (isIntentResolvable(context, mapplsIntent)) {
      context.startActivity(mapplsIntent);
      return;
    }

    // Mappls package direct intent
    Uri mapplsGeoUri = Uri.parse(String.format(Locale.US, "geo:%.6f,%.6f?q=%.6f,%.6f(%s)",
        lat, lon, lat, lon, Uri.encode(site.getSiteId())));
    Intent pkgIntent = new Intent(Intent.ACTION_VIEW, mapplsGeoUri);
    pkgIntent.setPackage("com.mmi.maps");
    pkgIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
    if (isIntentResolvable(context, pkgIntent)) {
      context.startActivity(pkgIntent);
      return;
    }

    // Fallback: Show toast and copy coordinates
    copyCoordinates(context, site);
    showToast(context, "Mappls is not installed. Coordinates copied to clipboard.");
  }

  /**
   * Opens standard Android Application Chooser for geo coordinates.
   */
  public static void openInChooser(@NonNull Context context, @NonNull TelecomSite site) {
    if (!site.hasValidCoords()) {
      showToast(context, "Site has no valid coordinates");
      return;
    }
    double lat = site.getLatitude();
    double lon = site.getLongitude();
    Uri geoUri = Uri.parse(String.format(Locale.US, "geo:%.6f,%.6f?q=%.6f,%.6f(%s)",
        lat, lon, lat, lon, Uri.encode(site.getSiteId() + " - " + site.getSiteName())));
    Intent intent = new Intent(Intent.ACTION_VIEW, geoUri);
    Intent chooser = Intent.createChooser(intent, "Navigate to " + site.getSiteId());
    chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
    try {
      context.startActivity(chooser);
    } catch (Exception e) {
      copyCoordinates(context, site);
      showToast(context, "No map application available. Coordinates copied to clipboard.");
    }
  }

  private static boolean isIntentResolvable(@NonNull Context context, @NonNull Intent intent) {
    return intent.resolveActivity(context.getPackageManager()) != null;
  }

  private static void copyToClipboard(@NonNull Context context, String label, String text, String toastMessage) {
    ClipboardManager clipboard = (ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
    if (clipboard != null) {
      ClipData clip = ClipData.newPlainText(label, text);
      clipboard.setPrimaryClip(clip);
      showToast(context, toastMessage);
    }
  }

  /**
   * Visualizes planned visit route in CoMaps with numbered waypoints and prepares turn-by-turn routing safely.
   */
  public static void showRouteInCoMaps(@NonNull Context context,
                                       double startLat,
                                       double startLon,
                                       @NonNull String startLabel,
                                       boolean isStartFromGps,
                                       @NonNull List<TelecomSite> stops,
                                       boolean hasReturn,
                                       double returnLat,
                                       double returnLon,
                                       @NonNull String returnLabel) {
    if (stops.isEmpty()) {
      showToast(context, "No stops in visit plan");
      return;
    }

    // 1. Sync numbered placemarks into CoMaps map layer (all stops [1]..[N] + return are always visible)
    TelecomMapLayerManager.getInstance().syncRouteToMap(context, startLat, startLon, startLabel, stops,
        hasReturn, returnLat, returnLon, returnLabel);

    // 2. Prepare native turn-by-turn routing controller using batch method
    try {
      double originLat = (startLat != 0.0 || startLon != 0.0) ? startLat : stops.get(0).getLatitude();
      double originLon = (startLat != 0.0 || startLon != 0.0) ? startLon : stops.get(0).getLongitude();

      // If starting from GPS or user location, set MY_POSITION so MwmActivity doesn't ask "Navigation is only available from your current location"
      boolean isStartMyPos = isStartFromGps || startLabel.startsWith("My Location");
      MapObject startPoint;
      if (isStartMyPos) {
        MapObject gpsPos = MwmApplication.from(context).getLocationHelper().getMyPosition();
        if (gpsPos != null) {
          startPoint = gpsPos;
        } else {
          startPoint = MapObject.createMapObject(FeatureId.EMPTY, MapObject.MY_POSITION, "My Position", "", originLat, originLon);
        }
      } else {
        startPoint = MapObject.createMapObject(FeatureId.EMPTY, MapObject.API_POINT, "Start: " + startLabel, "", originLat, originLon);
      }

      double destLat;
      double destLon;
      String destLabel;

      if (hasReturn && (returnLat != 0.0 || returnLon != 0.0)) {
        destLat = returnLat;
        destLon = returnLon;
        destLabel = "Return: " + returnLabel;
      } else {
        TelecomSite lastSite = stops.get(stops.size() - 1);
        destLat = lastSite.getLatitude();
        destLon = lastSite.getLongitude();
        destLabel = lastSite.getSiteId();
      }

      MapObject endPoint = MapObject.createMapObject(FeatureId.EMPTY, MapObject.API_POINT, destLabel, "", destLat, destLon);

      // Clean existing route marks to avoid race condition
      Framework.nativeRemoveRoutePoints();

      // Batch pattern: 1st add destination, 2nd add start, 3rd add intermediates without reordering
      Framework.addRoutePoint(new RouteMarkData(endPoint.getTitle(), endPoint.getSubtitle(),
          RouteMarkType.Finish, 0, true, false, false, destLat, destLon));

      Framework.addRoutePoint(new RouteMarkData(startPoint.getTitle(), startPoint.getSubtitle(),
          RouteMarkType.Start, 0, !isStartMyPos, isStartMyPos, false, originLat, originLon));

      // Intermediate stops:
      // For offline road networks, adding more than 4 intermediate points at once can cause offline road snapping failure.
      // We safely add up to 4 intermediate points for active navigation, while all stops are displayed on the map layer.
      int endIndex = (hasReturn && (returnLat != 0.0 || returnLon != 0.0)) ? stops.size() : stops.size() - 1;
      int startIndex = (startLat != 0.0 || startLon != 0.0) ? 0 : 1;
      int intermediateIndex = 0;
      for (int i = startIndex; i < endIndex; i++) {
        TelecomSite s = stops.get(i);
        if (s.hasValidCoords()) {
          Framework.addRoutePoint(new RouteMarkData(s.getSiteId(), s.getSiteName(),
              RouteMarkType.Intermediate, intermediateIndex, true, false, false, s.getLatitude(), s.getLongitude()), false);
          intermediateIndex++;
        }
      }

      // Single planning build call
      RoutingController.get().launchPlanning();
    } catch (Exception ignored) {
      // Placemark layer remains visible even if native routing data is partially available
    }

    // 3. Launch MwmActivity centered on starting point or first stop
    double centerLat = (startLat != 0.0) ? startLat : stops.get(0).getLatitude();
    double centerLon = (startLon != 0.0) ? startLon : stops.get(0).getLongitude();
    Intent intent = new Intent(context, MwmActivity.class);
    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
    intent.setData(Uri.parse(String.format(Locale.US, "geo:%.6f,%.6f?z=13", centerLat, centerLon)));
    context.startActivity(intent);
  }

  public static void showRouteInCoMaps(@NonNull Context context,
                                       double startLat,
                                       double startLon,
                                       @NonNull String startLabel,
                                       @NonNull List<TelecomSite> stops,
                                       boolean returnToStart) {
    showRouteInCoMaps(context, startLat, startLon, startLabel, false, stops, returnToStart, startLat, startLon, startLabel);
  }

  /**
   * Opens Google Maps with origin, intermediate waypoints, and destination in one complete itinerary.
   */
  public static void openMultiStopInGoogleMaps(@NonNull Context context,
                                               double startLat,
                                               double startLon,
                                               @NonNull List<TelecomSite> stops,
                                               boolean hasReturn,
                                               double returnLat,
                                               double returnLon) {
    if (stops.isEmpty()) {
      showToast(context, "No stops in visit plan");
      return;
    }

    double originLat = (startLat != 0.0 || startLon != 0.0) ? startLat : stops.get(0).getLatitude();
    double originLon = (startLat != 0.0 || startLon != 0.0) ? startLon : stops.get(0).getLongitude();

    double destLat;
    double destLon;
    if (hasReturn && (returnLat != 0.0 || returnLon != 0.0)) {
      destLat = returnLat;
      destLon = returnLon;
    } else {
      TelecomSite last = stops.get(stops.size() - 1);
      destLat = last.getLatitude();
      destLon = last.getLongitude();
    }

    StringBuilder url = new StringBuilder();
    url.append("https://www.google.com/maps/dir/?api=1")
       .append(String.format(Locale.US, "&origin=%.6f,%.6f", originLat, originLon))
       .append(String.format(Locale.US, "&destination=%.6f,%.6f", destLat, destLon));

    int endIndex = (hasReturn && (returnLat != 0.0 || returnLon != 0.0)) ? stops.size() : stops.size() - 1;
    int startIndex = (startLat != 0.0 || startLon != 0.0) ? 0 : 1;
    List<String> waypoints = new ArrayList<>();
    for (int i = startIndex; i < endIndex; i++) {
      TelecomSite s = stops.get(i);
      if (s.hasValidCoords()) {
        waypoints.add(String.format(Locale.US, "%.6f,%.6f", s.getLatitude(), s.getLongitude()));
      }
    }

    if (!waypoints.isEmpty()) {
      url.append("&waypoints=");
      for (int i = 0; i < waypoints.size(); i++) {
        if (i > 0) url.append("%7C"); // URL-encoded pipe |
        url.append(waypoints.get(i));
      }
    }

    Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url.toString()));
    intent.setPackage("com.google.android.apps.maps");
    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
    if (isIntentResolvable(context, intent)) {
      context.startActivity(intent);
    } else {
      intent.setPackage(null);
      context.startActivity(intent);
    }
  }

  public static void openMultiStopInGoogleMaps(@NonNull Context context,
                                               double startLat,
                                               double startLon,
                                               @NonNull List<TelecomSite> stops,
                                               boolean returnToStart) {
    openMultiStopInGoogleMaps(context, startLat, startLon, stops, returnToStart, startLat, startLon);
  }

  /**
   * Opens turn-by-turn navigation directly in Mappls (MapmyIndia) app or web fallback.
   * Checks both official Google Play package (com.mapmyindia.b2c) and OEM package (com.mmi.maps).
   */
  public static void openInMappls(@NonNull Context context, double lat, double lon, @NonNull String destinationName) {
    if (lat == 0.0 && lon == 0.0) {
      showToast(context, "Invalid coordinates for Mappls");
      return;
    }

    String encodedName = Uri.encode(destinationName);
    String mapplsUri = String.format(Locale.US, "mappls://navigation?places=%.6f,%.6f,%s&isNav=true&mode=driving", lat, lon, encodedName);
    Uri parsedMapplsUri = Uri.parse(mapplsUri);

    // List of known Mappls Android package names (Play Store is com.mapmyindia.b2c)
    String[] mapplsPackages = new String[] { "com.mapmyindia.b2c", "com.mmi.maps" };

    // 1. Try launching directly with Mappls packages via deep link or geo URI
    for (String pkg : mapplsPackages) {
      try {
        Intent intent = new Intent(Intent.ACTION_VIEW, parsedMapplsUri);
        intent.setPackage(pkg);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startActivity(intent);
        return;
      } catch (Exception ignored) {
        // Fallback to geo intent directed at this package
        try {
          Uri geoUri = Uri.parse(String.format(Locale.US, "geo:%.6f,%.6f?q=%.6f,%.6f(%s)", lat, lon, lat, lon, encodedName));
          Intent geoIntent = new Intent(Intent.ACTION_VIEW, geoUri);
          geoIntent.setPackage(pkg);
          geoIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
          context.startActivity(geoIntent);
          return;
        } catch (Exception ignored2) {}
      }
    }

    // 2. Try generic mappls:// URI without package restriction
    try {
      Intent intent = new Intent(Intent.ACTION_VIEW, parsedMapplsUri);
      intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
      context.startActivity(intent);
      return;
    } catch (Exception ignored) {}

    // 3. Fallback: Open in web browser
    try {
      String webUri = String.format(Locale.US, "https://mappls.com/navigation?places=%.6f,%.6f,%s&isNav=true&mode=driving", lat, lon, encodedName);
      Intent webIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(webUri));
      webIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
      context.startActivity(webIntent);
      return;
    } catch (Exception ignored) {}

    // 4. Fallback: Google Play Store with official package com.mapmyindia.b2c
    try {
      Intent marketIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=com.mapmyindia.b2c"));
      marketIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
      context.startActivity(marketIntent);
    } catch (Exception e) {
      showToast(context, "Mappls (MapmyIndia) is not installed");
    }
  }

  /**
   * Navigates the first/next stop in the visit plan using Mappls.
   */
  public static void openMultiStopInMappls(@NonNull Context context, @NonNull List<TelecomSite> stops) {
    if (stops.isEmpty()) {
      showToast(context, "No stops in visit plan");
      return;
    }
    TelecomSite nextStop = null;
    for (TelecomSite s : stops) {
      if (s.hasValidCoords()) {
        nextStop = s;
        break;
      }
    }
    if (nextStop != null) {
      showToast(context, "Launching navigation to " + nextStop.getSiteId() + " in Mappls");
      openInMappls(context, nextStop.getLatitude(), nextStop.getLongitude(), nextStop.getSiteId());
    } else {
      showToast(context, "No stops with valid coordinates");
    }
  }

  private static void showToast(@NonNull Context context, String message) {
    Toast.makeText(context, message, Toast.LENGTH_SHORT).show();
  }
}
