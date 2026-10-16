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
   * Prepares and starts turn-by-turn road navigation directly to a single site in CoMaps from user's location.
   * Useful to test if the site is reachable via OpenStreetMap road snapping or if it is off-road.
   */
  public static void navigateInCoMaps(@NonNull Context context, @NonNull TelecomSite site) {
    if (!site.hasValidCoords()) {
      showToast(context, "Site has no valid coordinates to navigate");
      return;
    }
    try {
      MapObject gpsPos = MwmApplication.from(context).getLocationHelper().getMyPosition();
      Intent intent = new Intent(context, MwmActivity.class);
      intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
      if (gpsPos != null) {
        String uriStr = String.format(Locale.US,
            "om://route?sll=%.6f,%.6f&saddr=%s&dll=%.6f,%.6f&daddr=%s&type=vehicle",
            gpsPos.getLat(), gpsPos.getLon(), Uri.encode("My Location"),
            site.getLatitude(), site.getLongitude(), Uri.encode(site.getSiteId()));
        intent.setData(Uri.parse(uriStr));
      } else {
        String uriStr = String.format(Locale.US, "geo:%.6f,%.6f?z=17", site.getLatitude(), site.getLongitude());
        intent.setData(Uri.parse(uriStr));
      }
      context.startActivity(intent);
    } catch (Exception e) {
      openInCoMaps(context, site);
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

    // 1. Try turn-by-turn navigation in Google Maps app
    if (turnByTurn) {
      try {
        Uri navUri = Uri.parse(String.format(Locale.US, "google.navigation:q=%.6f,%.6f&mode=d", lat, lon));
        Intent navIntent = new Intent(Intent.ACTION_VIEW, navUri);
        navIntent.setPackage("com.google.android.apps.maps");
        navIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startActivity(navIntent);
        return;
      } catch (Exception ignored) {}
    }

    // 2. Try Google Maps search intent with explicit package
    try {
      Uri gmmIntentUri = Uri.parse(String.format(Locale.US, "geo:%.6f,%.6f?q=%.6f,%.6f(%s)",
          lat, lon, lat, lon, Uri.encode(site.getSiteId() + " " + site.getSiteName())));
      Intent mapIntent = new Intent(Intent.ACTION_VIEW, gmmIntentUri);
      mapIntent.setPackage("com.google.android.apps.maps");
      mapIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
      context.startActivity(mapIntent);
      return;
    } catch (Exception ignored) {}

    // 3. Try generic geo: intent without package restriction
    try {
      Uri geoUri = Uri.parse(String.format(Locale.US, "geo:%.6f,%.6f?q=%.6f,%.6f(%s)",
          lat, lon, lat, lon, Uri.encode(site.getSiteId() + " " + site.getSiteName())));
      Intent geoIntent = new Intent(Intent.ACTION_VIEW, geoUri);
      geoIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
      context.startActivity(geoIntent);
      return;
    } catch (Exception ignored) {}

    // 4. Web Fallback if Google Maps app is not installed
    try {
      Uri webUri = Uri.parse(String.format(Locale.US, "https://www.google.com/maps/dir/?api=1&destination=%.6f,%.6f", lat, lon));
      Intent webIntent = new Intent(Intent.ACTION_VIEW, webUri);
      webIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
      context.startActivity(webIntent);
      return;
    } catch (Exception ignored) {}

    // 5. Final fallback: copy coordinates
    copyCoordinates(context, site);
    showToast(context, "Google Maps is not installed. Coordinates copied to clipboard.");
  }

  /**
   * Opens site location in Mappls (MapmyIndia).
   */
  public static void openInMappls(@NonNull Context context, @NonNull TelecomSite site) {
    if (!site.hasValidCoords()) {
      showToast(context, "Site has no valid coordinates");
      return;
    }
    String label = site.getSiteId() + (site.getSiteName().isEmpty() ? "" : " " + site.getSiteName());
    openInMappls(context, site.getLatitude(), site.getLongitude(), label);
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
                                       @NonNull String returnLabel,
                                       @Nullable Runnable onComplete) {
    if (stops.isEmpty()) {
      showToast(context, "No stops in visit plan");
      if (onComplete != null) onComplete.run();
      return;
    }

    // Sync numbered placemarks and route track into CoMaps map layer
    TelecomMapLayerManager.getInstance().syncRouteToMap(context, startLat, startLon, startLabel, stops,
        hasReturn, returnLat, returnLon, returnLabel, (success, count) -> {
          if (onComplete != null) {
            onComplete.run();
          }

          try {
            long catId = TelecomMapLayerManager.getInstance().getRouteCategoryId();
            Intent intent = new Intent(context, MwmActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
            if (catId != -1) {
              intent.putExtra(MwmActivity.EXTRA_CATEGORY_ID, catId);
            } else {
              double centerLat = (startLat != 0.0) ? startLat : stops.get(0).getLatitude();
              double centerLon = (startLon != 0.0) ? startLon : stops.get(0).getLongitude();
              intent.setData(Uri.parse(String.format(Locale.US, "geo:%.6f,%.6f?z=13", centerLat, centerLon)));
            }
            context.startActivity(intent);
          } catch (Exception e) {
            showToast(context, "Error displaying route: " + e.getMessage());
          }
        });
  }

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
    showRouteInCoMaps(context, startLat, startLon, startLabel, isStartFromGps, stops, hasReturn, returnLat, returnLon, returnLabel, null);
  }

  public static void showRouteInCoMaps(@NonNull Context context,
                                       double startLat,
                                       double startLon,
                                       @NonNull String startLabel,
                                       @NonNull List<TelecomSite> stops,
                                       boolean returnToStart) {
    showRouteInCoMaps(context, startLat, startLon, startLabel, false, stops, returnToStart, startLat, startLon, startLabel, null);
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
    try {
      context.startActivity(intent);
    } catch (Exception e) {
      try {
        intent.setPackage(null);
        context.startActivity(intent);
      } catch (Exception e2) {
        showToast(context, "Could not open Google Maps: " + e2.getMessage());
      }
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
   * Checks generic mappls:// scheme, alternate URI, and known packages.
   */
  public static void openInMappls(@NonNull Context context, double lat, double lon, @NonNull String destinationName) {
    if (lat == 0.0 && lon == 0.0) {
      showToast(context, "Invalid coordinates for Mappls");
      return;
    }

    String encodedName = Uri.encode(destinationName);
    String mapplsUri = String.format(Locale.US, "mappls://navigation?places=%.6f,%.6f,%s&isNav=true&mode=driving", lat, lon, encodedName);
    Uri parsedMapplsUri = Uri.parse(mapplsUri);

    // 1. Try generic mappls:// deep link directly (system matches installed Mappls without requiring package visibility)
    try {
      Intent intent = new Intent(Intent.ACTION_VIEW, parsedMapplsUri);
      intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
      context.startActivity(intent);
      return;
    } catch (Exception ignored) {}

    // 2. Try alternate mappls scheme: mappls://nav?dest=lat,lon
    try {
      Uri altMapplsUri = Uri.parse(String.format(Locale.US, "mappls://nav?dest=%.6f,%.6f", lat, lon));
      Intent altIntent = new Intent(Intent.ACTION_VIEW, altMapplsUri);
      altIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
      context.startActivity(altIntent);
      return;
    } catch (Exception ignored) {}

    // 3. Try known Mappls packages via deep link or geo URI
    String[] mapplsPackages = new String[] { "com.mapmyindia.b2c", "com.mmi.maps", "com.mapmyindia.move" };
    for (String pkg : mapplsPackages) {
      try {
        Intent intent = new Intent(Intent.ACTION_VIEW, parsedMapplsUri);
        intent.setPackage(pkg);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startActivity(intent);
        return;
      } catch (Exception ignored) {
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

    // 4. Fallback: Open in web browser
    try {
      String webUri = String.format(Locale.US, "https://mappls.com/navigation?places=%.6f,%.6f,%s&isNav=true&mode=driving", lat, lon, encodedName);
      Intent webIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(webUri));
      webIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
      context.startActivity(webIntent);
      return;
    } catch (Exception ignored) {}

    // 5. Final fallback: Copy coordinates to clipboard instead of opening market:// details
    copyToClipboard(context, "Mappls Coordinates", String.format(Locale.US, "%.6f, %.6f", lat, lon),
        "Mappls is not installed. Coordinates copied to clipboard.");
  }

  /**
   * Opens complete multi-stop visit plan route in Mappls with start location, intermediate stops, and optional return.
   */
  public static void openMultiStopInMappls(@NonNull Context context,
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

    List<String> places = new ArrayList<>();

    // 1. Origin / Start Point
    if (startLat != 0.0 || startLon != 0.0) {
      places.add(String.format(Locale.US, "%.6f,%.6f,Start", startLat, startLon));
    }

    // 2. All intermediate stops in visit order
    for (TelecomSite s : stops) {
      if (s.hasValidCoords()) {
        String label = s.getSiteId().isEmpty() ? "Site" : Uri.encode(s.getSiteId());
        places.add(String.format(Locale.US, "%.6f,%.6f,%s", s.getLatitude(), s.getLongitude(), label));
      }
    }

    // 3. Return Destination
    if (hasReturn && (returnLat != 0.0 || returnLon != 0.0)) {
      places.add(String.format(Locale.US, "%.6f,%.6f,Return", returnLat, returnLon));
    }

    if (places.isEmpty()) {
      showToast(context, "No stops with valid coordinates");
      return;
    }

    // If only one location, launch single stop navigation
    if (places.size() == 1) {
      TelecomSite first = stops.get(0);
      if (first.hasValidCoords()) {
        openInMappls(context, first.getLatitude(), first.getLongitude(), first.getSiteId());
      }
      return;
    }

    // Semicolon-delimited waypoints: place1;place2;place3;...
    StringBuilder placesParam = new StringBuilder();
    for (int i = 0; i < places.size(); i++) {
      if (i > 0) {
        placesParam.append(";");
      }
      placesParam.append(places.get(i));
    }

    String placesStr = placesParam.toString();
    Uri dirUri = Uri.parse("mappls://direction?places=" + placesStr);
    Uri navUri = Uri.parse("mappls://navigation?places=" + placesStr + "&isNav=true&mode=driving");

    // 1. Try generic mappls://direction deep link
    try {
      Intent intent = new Intent(Intent.ACTION_VIEW, dirUri);
      intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
      context.startActivity(intent);
      return;
    } catch (Exception ignored) {}

    // 2. Try generic mappls://navigation deep link with waypoints
    try {
      Intent intent = new Intent(Intent.ACTION_VIEW, navUri);
      intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
      context.startActivity(intent);
      return;
    } catch (Exception ignored) {}

    // 3. Try with known Mappls packages directly
    String[] mapplsPackages = new String[] { "com.mapmyindia.b2c", "com.mmi.maps", "com.mapmyindia.move" };
    for (String pkg : mapplsPackages) {
      try {
        Intent intent = new Intent(Intent.ACTION_VIEW, dirUri);
        intent.setPackage(pkg);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startActivity(intent);
        return;
      } catch (Exception ignored) {}
    }

    // 4. Fallback: Open web direction widget
    try {
      StringBuilder webCoords = new StringBuilder();
      for (int i = 0; i < places.size(); i++) {
        if (i > 0) webCoords.append(";");
        String[] parts = places.get(i).split(",");
        if (parts.length >= 2) {
          webCoords.append(parts[0]).append(",").append(parts[1]);
        }
      }
      String webUri = "https://mappls.com/direction?places=" + webCoords.toString();
      Intent webIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(webUri));
      webIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
      context.startActivity(webIntent);
      return;
    } catch (Exception ignored) {}

    // 5. Final fallback: open first stop
    TelecomSite first = stops.get(0);
    if (first.hasValidCoords()) {
      openInMappls(context, first.getLatitude(), first.getLongitude(), first.getSiteId());
    }
  }

  public static void openMultiStopInMappls(@NonNull Context context, @NonNull List<TelecomSite> stops) {
    openMultiStopInMappls(context, 0.0, 0.0, stops, false, 0.0, 0.0);
  }

  private static void showToast(@NonNull Context context, String message) {
    Toast.makeText(context, message, Toast.LENGTH_SHORT).show();
  }
}
