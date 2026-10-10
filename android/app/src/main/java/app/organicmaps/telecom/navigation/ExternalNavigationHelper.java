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
import app.organicmaps.R;
import app.organicmaps.sdk.Framework;
import app.organicmaps.telecom.model.TelecomSite;
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

  private static void showToast(@NonNull Context context, String message) {
    Toast.makeText(context, message, Toast.LENGTH_SHORT).show();
  }
}
