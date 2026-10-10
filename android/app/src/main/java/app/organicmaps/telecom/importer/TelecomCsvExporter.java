package app.organicmaps.telecom.importer;

import androidx.annotation.NonNull;
import app.organicmaps.telecom.model.TelecomSite;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * Exports TelecomSite dataset to a standard RFC 4180 UTF-8 CSV file.
 */
public class TelecomCsvExporter {

  public static void exportToCsv(@NonNull List<TelecomSite> sites, @NonNull OutputStream outputStream) throws IOException {
    Writer writer = new BufferedWriter(new OutputStreamWriter(outputStream, StandardCharsets.UTF_8));
    // Write UTF-8 BOM for Excel compatibility
    writer.write('\ufeff');

    // Header row
    String[] headers = new String[]{
        "site_id", "site_name", "site_type", "location", "address",
        "latitude", "longitude", "technician_name", "technician_phone", "technician_pprj",
        "supervisor_name", "supervisor_phone", "power_status", "eb_availability", "site_load_amp",
        "consumer_id", "meter_no", "installation_no", "eb_division", "eb_ccc", "docket_no",
        "smps_make", "smps_rating", "smps_fault", "smps_remarks",
        "bm_make", "bm_rating", "bb_backup", "bm_physical_count", "bm_faulty_count", "bm_remarks", "battery_serials",
        "rm_make", "rm_rating", "rm_physical_count", "rm_bypass_count", "rm_faulty_count", "rm_remarks",
        "dg_make", "dg_rating", "gcu_make", "gcu_model", "gcu_status", "fuel_sensor_status", "dg_automation_status", "dg_run_hours", "fuel_level",
        "is_solar", "mppt_make", "mppt_rating", "mppt_count", "mppt_faulty_count", "vegetation_status",
        "notes", "extra_attributes"
    };

    writeCsvRow(writer, headers);

    for (TelecomSite site : sites) {
      String[] row = new String[]{
          site.getSiteId(),
          site.getSiteName(),
          site.getSiteType(),
          site.getLocation(),
          site.getAddress(),
          site.hasValidCoords() ? String.format(java.util.Locale.US, "%.6f", site.getLatitude()) : "",
          site.hasValidCoords() ? String.format(java.util.Locale.US, "%.6f", site.getLongitude()) : "",
          site.getTechnicianName(),
          site.getTechnicianPhone(),
          site.getTechnicianPprj(),
          site.getSupervisorName(),
          site.getSupervisorPhone(),
          site.getPowerStatus(),
          site.getEbAvailability(),
          site.getSiteLoadAmp(),
          site.getConsumerId(),
          site.getMeterNo(),
          site.getInstallationNo(),
          site.getEbDivision(),
          site.getEbCcc(),
          site.getDocketNo(),
          site.getSmpsMake(),
          site.getSmpsRating(),
          site.getSmpsFault(),
          site.getSmpsRemarks(),
          site.getBmMake(),
          site.getBmRating(),
          site.getBbBackup(),
          site.getBmPhysicalCount(),
          site.getBmFaultyCount(),
          site.getBmRemarks(),
          site.getBatterySerials(),
          site.getRmMake(),
          site.getRmRating(),
          site.getRmPhysicalCount(),
          site.getRmBypassCount(),
          site.getRmFaultyCount(),
          site.getRmRemarks(),
          site.getDgMake(),
          site.getDgRating(),
          site.getGcuMake(),
          site.getGcuModel(),
          site.getGcuStatus(),
          site.getFuelSensorStatus(),
          site.getDgAutomationStatus(),
          site.getDgRunHours(),
          site.getFuelLevel(),
          site.getIsSolar(),
          site.getMpptMake(),
          site.getMpptRating(),
          site.getMpptCount(),
          site.getMpptFaultyCount(),
          site.getVegetationStatus(),
          site.getNotes(),
          site.getExtraAttributesAsJson()
      };
      writeCsvRow(writer, row);
    }

    writer.flush();
  }

  public static void exportToFile(@NonNull List<TelecomSite> sites, @NonNull File file) throws IOException {
    try (FileOutputStream fos = new FileOutputStream(file)) {
      exportToCsv(sites, fos);
    }
  }

  private static void writeCsvRow(Writer writer, String[] values) throws IOException {
    for (int i = 0; i < values.length; i++) {
      if (i > 0) {
        writer.write(',');
      }
      String val = values[i] != null ? values[i] : "";
      if (val.contains(",") || val.contains("\"") || val.contains("\n") || val.contains("\r")) {
        writer.write('"');
        writer.write(val.replace("\"", "\"\""));
        writer.write('"');
      } else {
        writer.write(val);
      }
    }
    writer.write("\r\n");
  }
}
