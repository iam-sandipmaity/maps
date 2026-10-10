package app.organicmaps.telecom.importer;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import app.organicmaps.telecom.model.TelecomSite;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Handles configurable column mapping from CSV headers to TelecomSite fields.
 * Includes intelligent alias matching for common telecom spreadsheets.
 */
public class ColumnMapper {

  public static final String FIELD_SITE_ID = "site_id";
  public static final String FIELD_SITE_NAME = "site_name";
  public static final String FIELD_SITE_TYPE = "site_type";
  public static final String FIELD_LOCATION = "location";
  public static final String FIELD_ADDRESS = "address";
  public static final String FIELD_LATITUDE = "latitude";
  public static final String FIELD_LONGITUDE = "longitude";
  public static final String FIELD_TECH_NAME = "technician_name";
  public static final String FIELD_TECH_PHONE = "technician_phone";
  public static final String FIELD_TECH_PPRJ = "technician_pprj";
  public static final String FIELD_SUPERVISOR_NAME = "supervisor_name";
  public static final String FIELD_SUPERVISOR_PHONE = "supervisor_phone";
  public static final String FIELD_POWER_STATUS = "power_status";
  public static final String FIELD_EB_AVAILABILITY = "eb_availability";
  public static final String FIELD_SITE_LOAD_AMP = "site_load_amp";
  public static final String FIELD_CONSUMER_ID = "consumer_id";
  public static final String FIELD_METER_NO = "meter_no";
  public static final String FIELD_INSTALLATION_NO = "installation_no";
  public static final String FIELD_EB_DIVISION = "eb_division";
  public static final String FIELD_EB_CCC = "eb_ccc";
  public static final String FIELD_DOCKET_NO = "docket_no";
  public static final String FIELD_SMPS_MAKE = "smps_make";
  public static final String FIELD_SMPS_RATING = "smps_rating";
  public static final String FIELD_SMPS_FAULT = "smps_fault";
  public static final String FIELD_SMPS_REMARKS = "smps_remarks";
  public static final String FIELD_BM_MAKE = "bm_make";
  public static final String FIELD_BM_RATING = "bm_rating";
  public static final String FIELD_BB_BACKUP = "bb_backup";
  public static final String FIELD_BM_PHYSICAL_COUNT = "bm_physical_count";
  public static final String FIELD_BM_FAULTY_COUNT = "bm_faulty_count";
  public static final String FIELD_BM_REMARKS = "bm_remarks";
  public static final String FIELD_BATTERY_SERIALS = "battery_serials";
  public static final String FIELD_RM_MAKE = "rm_make";
  public static final String FIELD_RM_RATING = "rm_rating";
  public static final String FIELD_RM_PHYSICAL_COUNT = "rm_physical_count";
  public static final String FIELD_RM_BYPASS_COUNT = "rm_bypass_count";
  public static final String FIELD_RM_FAULTY_COUNT = "rm_faulty_count";
  public static final String FIELD_RM_REMARKS = "rm_remarks";
  public static final String FIELD_DG_MAKE = "dg_make";
  public static final String FIELD_DG_RATING = "dg_rating";
  public static final String FIELD_GCU_MAKE = "gcu_make";
  public static final String FIELD_GCU_MODEL = "gcu_model";
  public static final String FIELD_GCU_STATUS = "gcu_status";
  public static final String FIELD_FUEL_SENSOR_STATUS = "fuel_sensor_status";
  public static final String FIELD_DG_AUTOMATION_STATUS = "dg_automation_status";
  public static final String FIELD_DG_RUN_HOURS = "dg_run_hours";
  public static final String FIELD_FUEL_LEVEL = "fuel_level";
  public static final String FIELD_IS_SOLAR = "is_solar";
  public static final String FIELD_MPPT_MAKE = "mppt_make";
  public static final String FIELD_MPPT_RATING = "mppt_rating";
  public static final String FIELD_MPPT_COUNT = "mppt_count";
  public static final String FIELD_MPPT_FAULTY_COUNT = "mppt_faulty_count";
  public static final String FIELD_VEGETATION_STATUS = "vegetation_status";
  public static final String FIELD_NOTES = "notes";

  // Map of column index to normalized field name
  private final Map<Integer, String> indexToFieldMap = new HashMap<>();
  // Header raw names for unmapped columns
  private final Map<Integer, String> indexToHeaderRawName = new HashMap<>();

  public static ColumnMapper autoDetectMapping(@NonNull List<String> headers) {
    ColumnMapper mapper = new ColumnMapper();
    for (int i = 0; i < headers.size(); i++) {
      String header = headers.get(i);
      mapper.indexToHeaderRawName.put(i, header);
      String normalized = normalizeHeader(header);
      String field = matchField(normalized);
      if (field != null) {
        mapper.indexToFieldMap.put(i, field);
      }
    }
    return mapper;
  }

  public void setMapping(int columnIndex, @Nullable String fieldName) {
    if (fieldName == null || fieldName.isEmpty()) {
      indexToFieldMap.remove(columnIndex);
    } else {
      indexToFieldMap.put(columnIndex, fieldName);
    }
  }

  public Map<Integer, String> getMappings() {
    return new HashMap<>(indexToFieldMap);
  }

  public static String normalizeHeader(String header) {
    if (header == null) return "";
    return header.trim().toLowerCase(Locale.ROOT)
        .replace("_", " ")
        .replace("-", " ")
        .replaceAll("[^a-z0-9 ]", "")
        .replaceAll("\\s+", " ")
        .trim();
  }

  @Nullable
  public static String matchField(String normalized) {
    if (normalized.equals("sap id") || normalized.equals("site id") || normalized.equals("new sap id") || normalized.equals("siteid") || normalized.equals("id")) {
      return FIELD_SITE_ID;
    }
    if (normalized.equals("site name") || normalized.equals("sitename") || normalized.equals("name")) {
      return FIELD_SITE_NAME;
    }
    if (normalized.equals("site type") || normalized.equals("type") || normalized.equals("p1 rp1") || normalized.equals("facility type") || normalized.equals("p1 rp1 colo")) {
      return FIELD_SITE_TYPE;
    }
    if (normalized.equals("lat") || normalized.equals("latitude") || normalized.equals("y")) {
      return FIELD_LATITUDE;
    }
    if (normalized.equals("long") || normalized.equals("longitude") || normalized.equals("lon") || normalized.equals("lng") || normalized.equals("x")) {
      return FIELD_LONGITUDE;
    }
    if (normalized.equals("jc name") || normalized.equals("jc") || normalized.equals("location") || normalized.equals("cmp") || normalized.equals("cluster") || normalized.equals("circle") || normalized.equals("zone")) {
      return FIELD_LOCATION;
    }
    if (normalized.equals("address") || normalized.equals("site address") || normalized.equals("address 2")) {
      return FIELD_ADDRESS;
    }
    if (normalized.contains("technician phone") || normalized.contains("technician mobile") || normalized.contains("technician number") || normalized.contains("techinician number") || normalized.equals("mobile no") || normalized.equals("tech no") || normalized.equals("contact no")) {
      return FIELD_TECH_PHONE;
    }
    if (normalized.contains("technician name") || normalized.equals("technician") || normalized.equals("new technician") || normalized.equals("old tech name")) {
      return FIELD_TECH_NAME;
    }
    if (normalized.contains("pprj")) {
      return FIELD_TECH_PPRJ;
    }
    if (normalized.contains("supervisor phone") || normalized.contains("sp engineer mobile") || normalized.contains("supervisor mobile") || normalized.contains("supervisor contact") || normalized.contains("sp engineer number")) {
      return FIELD_SUPERVISOR_PHONE;
    }

    if (normalized.contains("sp engineer") || normalized.contains("supervisor name") || normalized.equals("supervisor") || normalized.contains("sp lead name")) {
      return FIELD_SUPERVISOR_NAME;
    }
    if (normalized.equals("power status") || normalized.equals("energy status")) {
      return FIELD_POWER_STATUS;
    }
    if (normalized.contains("eb availability") || normalized.contains("eb availibilty") || normalized.equals("eb")) {
      return FIELD_EB_AVAILABILITY;
    }
    if (normalized.contains("site load") || normalized.contains("load")) {
      return FIELD_SITE_LOAD_AMP;
    }
    if (normalized.equals("consumer id")) {
      return FIELD_CONSUMER_ID;
    }
    if (normalized.equals("meter no")) {
      return FIELD_METER_NO;
    }
    if (normalized.equals("installation no")) {
      return FIELD_INSTALLATION_NO;
    }
    if (normalized.equals("division") || normalized.equals("division name") || normalized.equals("eb division")) {
      return FIELD_EB_DIVISION;
    }
    if (normalized.equals("ccc") || normalized.equals("supply") || normalized.equals("eb ccc")) {
      return FIELD_EB_CCC;
    }
    if (normalized.contains("docket")) {
      return FIELD_DOCKET_NO;
    }
    if (normalized.contains("smps make")) {
      return FIELD_SMPS_MAKE;
    }
    if (normalized.contains("smps rating")) {
      return FIELD_SMPS_RATING;
    }
    if (normalized.contains("smps fault") || normalized.contains("fault details")) {
      return FIELD_SMPS_FAULT;
    }
    if (normalized.contains("smps remark")) {
      return FIELD_SMPS_REMARKS;
    }
    if (normalized.contains("bm make")) {
      return FIELD_BM_MAKE;
    }
    if (normalized.contains("bm rating")) {
      return FIELD_BM_RATING;
    }
    if (normalized.contains("bb backup") || normalized.contains("battery backup")) {
      return FIELD_BB_BACKUP;
    }
    if (normalized.contains("bm physical count")) {
      return FIELD_BM_PHYSICAL_COUNT;
    }
    if (normalized.contains("bm faulty count")) {
      return FIELD_BM_FAULTY_COUNT;
    }
    if (normalized.contains("bm remark")) {
      return FIELD_BM_REMARKS;
    }
    if (normalized.contains("bat 1") || normalized.contains("battery serial") || normalized.contains("bm details")) {
      return FIELD_BATTERY_SERIALS;
    }
    if (normalized.contains("rm make")) {
      return FIELD_RM_MAKE;
    }
    if (normalized.contains("rm rating")) {
      return FIELD_RM_RATING;
    }
    if (normalized.contains("rm physical count")) {
      return FIELD_RM_PHYSICAL_COUNT;
    }
    if (normalized.contains("rm bypass count")) {
      return FIELD_RM_BYPASS_COUNT;
    }
    if (normalized.contains("rm faulty count")) {
      return FIELD_RM_FAULTY_COUNT;
    }
    if (normalized.contains("rm remark")) {
      return FIELD_RM_REMARKS;
    }
    if (normalized.contains("dg make")) {
      return FIELD_DG_MAKE;
    }
    if (normalized.contains("dg rating")) {
      return FIELD_DG_RATING;
    }
    if (normalized.contains("gcu make")) {
      return FIELD_GCU_MAKE;
    }
    if (normalized.contains("gcu model")) {
      return FIELD_GCU_MODEL;
    }
    if (normalized.contains("gcu faulty") || normalized.contains("gcu status")) {
      return FIELD_GCU_STATUS;
    }
    if (normalized.contains("fuel sensor")) {
      return FIELD_FUEL_SENSOR_STATUS;
    }
    if (normalized.contains("dg automation") || normalized.contains("automation status")) {
      return FIELD_DG_AUTOMATION_STATUS;
    }
    if (normalized.contains("dg runhr") || normalized.contains("runhr")) {
      return FIELD_DG_RUN_HOURS;
    }
    if (normalized.contains("fuel level") || normalized.contains("actual stock")) {
      return FIELD_FUEL_LEVEL;
    }
    if (normalized.contains("solar site") || normalized.equals("solar")) {
      return FIELD_IS_SOLAR;
    }
    if (normalized.contains("mppt make")) {
      return FIELD_MPPT_MAKE;
    }
    if (normalized.contains("mppt rating")) {
      return FIELD_MPPT_RATING;
    }
    if (normalized.contains("mppt count")) {
      return FIELD_MPPT_COUNT;
    }
    if (normalized.contains("mppt faulty")) {
      return FIELD_MPPT_FAULTY_COUNT;
    }
    if (normalized.contains("vegetation")) {
      return FIELD_VEGETATION_STATUS;
    }
    if (normalized.contains("remark") || normalized.contains("note") || normalized.contains("visibility lost")) {
      return FIELD_NOTES;
    }
    return null;
  }

  /**
   * Applies the mapping to a single row to construct a TelecomSite.
   */
  @Nullable
  public TelecomSite mapRow(@NonNull List<String> row, int rowNumber, @NonNull ImportSummary summary) {
    TelecomSite site = null;
    String siteId = null;
    double lat = 0.0;
    double lon = 0.0;
    boolean hasLat = false;
    boolean hasLon = false;

    // First find site_id
    for (int i = 0; i < row.size(); i++) {
      String field = indexToFieldMap.get(i);
      if (FIELD_SITE_ID.equals(field)) {
        String val = row.get(i).trim();
        if (!val.isEmpty()) {
          siteId = val;
          break;
        }
      }
    }

    if (siteId == null || siteId.isEmpty()) {
      summary.incrementInvalid();
      summary.addValidationError("Row " + rowNumber + ": Missing Site ID / SAP ID.");
      return null;
    }

    site = new TelecomSite(siteId);

    for (int i = 0; i < row.size(); i++) {
      String val = row.get(i).trim();
      if (val.isEmpty()) continue;

      String field = indexToFieldMap.get(i);
      if (field == null) {
        String rawHeader = indexToHeaderRawName.get(i);
        if (rawHeader != null && !rawHeader.trim().isEmpty()) {
          site.addExtraAttribute(rawHeader, val);
        }
        continue;
      }

      switch (field) {
        case FIELD_SITE_NAME -> site.setSiteName(val);
        case FIELD_SITE_TYPE -> site.setSiteType(val);
        case FIELD_LOCATION -> site.setLocation(val);
        case FIELD_ADDRESS -> site.setAddress(val);
        case FIELD_LATITUDE -> {
          try {
            lat = Double.parseDouble(val);
            hasLat = true;
          } catch (NumberFormatException e) {
            summary.addValidationError("Row " + rowNumber + " (" + siteId + "): Invalid latitude value '" + val + "'.");
          }
        }
        case FIELD_LONGITUDE -> {
          try {
            lon = Double.parseDouble(val);
            hasLon = true;
          } catch (NumberFormatException e) {
            summary.addValidationError("Row " + rowNumber + " (" + siteId + "): Invalid longitude value '" + val + "'.");
          }
        }
        case FIELD_TECH_NAME -> site.setTechnicianName(val);
        case FIELD_TECH_PHONE -> site.setTechnicianPhone(val);
        case FIELD_TECH_PPRJ -> site.setTechnicianPprj(val);
        case FIELD_SUPERVISOR_NAME -> site.setSupervisorName(val);
        case FIELD_SUPERVISOR_PHONE -> site.setSupervisorPhone(val);
        case FIELD_POWER_STATUS -> site.setPowerStatus(val);
        case FIELD_EB_AVAILABILITY -> site.setEbAvailability(val);
        case FIELD_SITE_LOAD_AMP -> site.setSiteLoadAmp(val);
        case FIELD_CONSUMER_ID -> site.setConsumerId(val);
        case FIELD_METER_NO -> site.setMeterNo(val);
        case FIELD_INSTALLATION_NO -> site.setInstallationNo(val);
        case FIELD_EB_DIVISION -> site.setEbDivision(val);
        case FIELD_EB_CCC -> site.setEbCcc(val);
        case FIELD_DOCKET_NO -> site.setDocketNo(val);
        case FIELD_SMPS_MAKE -> site.setSmpsMake(val);
        case FIELD_SMPS_RATING -> site.setSmpsRating(val);
        case FIELD_SMPS_FAULT -> site.setSmpsFault(val);
        case FIELD_SMPS_REMARKS -> site.setSmpsRemarks(val);
        case FIELD_BM_MAKE -> site.setBmMake(val);
        case FIELD_BM_RATING -> site.setBmRating(val);
        case FIELD_BB_BACKUP -> site.setBbBackup(val);
        case FIELD_BM_PHYSICAL_COUNT -> site.setBmPhysicalCount(val);
        case FIELD_BM_FAULTY_COUNT -> site.setBmFaultyCount(val);
        case FIELD_BM_REMARKS -> site.setBmRemarks(val);
        case FIELD_BATTERY_SERIALS -> site.setBatterySerials(val);
        case FIELD_RM_MAKE -> site.setRmMake(val);
        case FIELD_RM_RATING -> site.setRmRating(val);
        case FIELD_RM_PHYSICAL_COUNT -> site.setRmPhysicalCount(val);
        case FIELD_RM_BYPASS_COUNT -> site.setRmBypassCount(val);
        case FIELD_RM_FAULTY_COUNT -> site.setRmFaultyCount(val);
        case FIELD_RM_REMARKS -> site.setRmRemarks(val);
        case FIELD_DG_MAKE -> site.setDgMake(val);
        case FIELD_DG_RATING -> site.setDgRating(val);
        case FIELD_GCU_MAKE -> site.setGcuMake(val);
        case FIELD_GCU_MODEL -> site.setGcuModel(val);
        case FIELD_GCU_STATUS -> site.setGcuStatus(val);
        case FIELD_FUEL_SENSOR_STATUS -> site.setFuelSensorStatus(val);
        case FIELD_DG_AUTOMATION_STATUS -> site.setDgAutomationStatus(val);
        case FIELD_DG_RUN_HOURS -> site.setDgRunHours(val);
        case FIELD_FUEL_LEVEL -> site.setFuelLevel(val);
        case FIELD_IS_SOLAR -> site.setIsSolar(val);
        case FIELD_MPPT_MAKE -> site.setMpptMake(val);
        case FIELD_MPPT_RATING -> site.setMpptRating(val);
        case FIELD_MPPT_COUNT -> site.setMpptCount(val);
        case FIELD_MPPT_FAULTY_COUNT -> site.setMpptFaultyCount(val);
        case FIELD_VEGETATION_STATUS -> site.setVegetationStatus(val);
        case FIELD_NOTES -> site.setNotes(val);
        default -> {
          String rawH = indexToHeaderRawName.get(i);
          if (rawH != null) {
            site.addExtraAttribute(rawH, val);
          }
        }
      }
    }

    if (hasLat && hasLon) {
      if (TelecomSite.validateCoordinates(lat, lon)) {
        site.setCoordinates(lat, lon);
      } else {
        summary.addValidationError("Row " + rowNumber + " (" + siteId + "): Coordinates out of range: " + lat + ", " + lon);
      }
    }

    return site;
  }
}
