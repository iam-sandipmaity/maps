package app.organicmaps.telecom.db;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import app.organicmaps.telecom.importer.ColumnMapper;
import app.organicmaps.telecom.importer.CsvParser;
import app.organicmaps.telecom.model.TelecomSite;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TelecomDatabaseHelper extends SQLiteOpenHelper {

  private static final String DATABASE_NAME = "telecom_sites.db";
  private static final int DATABASE_VERSION = 1;

  public static final String TABLE_SITES = "telecom_sites";
  public static final String TABLE_VISIT_PLAN = "visit_plan";

  // Column definitions for TABLE_SITES
  public static final String COL_SITE_ID = "site_id";
  public static final String COL_SITE_NAME = "site_name";
  public static final String COL_SITE_TYPE = "site_type";
  public static final String COL_LOCATION = "location";
  public static final String COL_ADDRESS = "address";
  public static final String COL_LATITUDE = "latitude";
  public static final String COL_LONGITUDE = "longitude";
  public static final String COL_HAS_VALID_COORDS = "has_valid_coords";
  public static final String COL_TECH_NAME = "technician_name";
  public static final String COL_TECH_PHONE = "technician_phone";
  public static final String COL_TECH_PPRJ = "technician_pprj";
  public static final String COL_SUPERVISOR_NAME = "supervisor_name";
  public static final String COL_SUPERVISOR_PHONE = "supervisor_phone";
  public static final String COL_POWER_STATUS = "power_status";
  public static final String COL_EB_AVAILABILITY = "eb_availability";
  public static final String COL_SITE_LOAD_AMP = "site_load_amp";
  public static final String COL_CONSUMER_ID = "consumer_id";
  public static final String COL_METER_NO = "meter_no";
  public static final String COL_INSTALLATION_NO = "installation_no";
  public static final String COL_EB_DIVISION = "eb_division";
  public static final String COL_EB_CCC = "eb_ccc";
  public static final String COL_DOCKET_NO = "docket_no";
  public static final String COL_SMPS_MAKE = "smps_make";
  public static final String COL_SMPS_RATING = "smps_rating";
  public static final String COL_SMPS_FAULT = "smps_fault";
  public static final String COL_SMPS_REMARKS = "smps_remarks";
  public static final String COL_BM_MAKE = "bm_make";
  public static final String COL_BM_RATING = "bm_rating";
  public static final String COL_BB_BACKUP = "bb_backup";
  public static final String COL_BM_PHYSICAL_COUNT = "bm_physical_count";
  public static final String COL_BM_FAULTY_COUNT = "bm_faulty_count";
  public static final String COL_BM_REMARKS = "bm_remarks";
  public static final String COL_BATTERY_SERIALS = "battery_serials";
  public static final String COL_RM_MAKE = "rm_make";
  public static final String COL_RM_RATING = "rm_rating";
  public static final String COL_RM_PHYSICAL_COUNT = "rm_physical_count";
  public static final String COL_RM_BYPASS_COUNT = "rm_bypass_count";
  public static final String COL_RM_FAULTY_COUNT = "rm_faulty_count";
  public static final String COL_RM_REMARKS = "rm_remarks";
  public static final String COL_DG_MAKE = "dg_make";
  public static final String COL_DG_RATING = "dg_rating";
  public static final String COL_GCU_MAKE = "gcu_make";
  public static final String COL_GCU_MODEL = "gcu_model";
  public static final String COL_GCU_STATUS = "gcu_status";
  public static final String COL_FUEL_SENSOR_STATUS = "fuel_sensor_status";
  public static final String COL_DG_AUTOMATION_STATUS = "dg_automation_status";
  public static final String COL_DG_RUN_HOURS = "dg_run_hours";
  public static final String COL_FUEL_LEVEL = "fuel_level";
  public static final String COL_IS_SOLAR = "is_solar";
  public static final String COL_MPPT_MAKE = "mppt_make";
  public static final String COL_MPPT_RATING = "mppt_rating";
  public static final String COL_MPPT_COUNT = "mppt_count";
  public static final String COL_MPPT_FAULTY_COUNT = "mppt_faulty_count";
  public static final String COL_VEGETATION_STATUS = "vegetation_status";
  public static final String COL_NOTES = "notes";
  public static final String COL_EXTRA_ATTRIBUTES = "extra_attributes";
  public static final String COL_UPDATED_AT = "updated_at";

  // Visit Plan Columns
  public static final String COL_PLAN_ID = "id";
  public static final String COL_PLAN_SITE_ID = "site_id";
  public static final String COL_PLAN_ORDER = "visit_order";
  public static final String COL_PLAN_NOTES = "notes";

  public enum ImportMode {
    ADD_NEW, // Only add non-existing records
    UPDATE_EXISTING, // Insert or update existing records
    REPLACE_ALL // Wipe existing records and replace with new dataset
  }

  private static TelecomDatabaseHelper sInstance;

  public static synchronized TelecomDatabaseHelper getInstance(@NonNull Context context) {
    if (sInstance == null) {
      sInstance = new TelecomDatabaseHelper(context.getApplicationContext());
    }
    return sInstance;
  }

  public TelecomDatabaseHelper(@NonNull Context context) {
    super(context, DATABASE_NAME, null, DATABASE_VERSION);
  }

  @Override
  public void onCreate(SQLiteDatabase db) {
    String createSitesTable = "CREATE TABLE " + TABLE_SITES + " ("
        + COL_SITE_ID + " TEXT PRIMARY KEY, "
        + COL_SITE_NAME + " TEXT, "
        + COL_SITE_TYPE + " TEXT, "
        + COL_LOCATION + " TEXT, "
        + COL_ADDRESS + " TEXT, "
        + COL_LATITUDE + " REAL, "
        + COL_LONGITUDE + " REAL, "
        + COL_HAS_VALID_COORDS + " INTEGER DEFAULT 0, "
        + COL_TECH_NAME + " TEXT, "
        + COL_TECH_PHONE + " TEXT, "
        + COL_TECH_PPRJ + " TEXT, "
        + COL_SUPERVISOR_NAME + " TEXT, "
        + COL_SUPERVISOR_PHONE + " TEXT, "
        + COL_POWER_STATUS + " TEXT, "
        + COL_EB_AVAILABILITY + " TEXT, "
        + COL_SITE_LOAD_AMP + " TEXT, "
        + COL_CONSUMER_ID + " TEXT, "
        + COL_METER_NO + " TEXT, "
        + COL_INSTALLATION_NO + " TEXT, "
        + COL_EB_DIVISION + " TEXT, "
        + COL_EB_CCC + " TEXT, "
        + COL_DOCKET_NO + " TEXT, "
        + COL_SMPS_MAKE + " TEXT, "
        + COL_SMPS_RATING + " TEXT, "
        + COL_SMPS_FAULT + " TEXT, "
        + COL_SMPS_REMARKS + " TEXT, "
        + COL_BM_MAKE + " TEXT, "
        + COL_BM_RATING + " TEXT, "
        + COL_BB_BACKUP + " TEXT, "
        + COL_BM_PHYSICAL_COUNT + " TEXT, "
        + COL_BM_FAULTY_COUNT + " TEXT, "
        + COL_BM_REMARKS + " TEXT, "
        + COL_BATTERY_SERIALS + " TEXT, "
        + COL_RM_MAKE + " TEXT, "
        + COL_RM_RATING + " TEXT, "
        + COL_RM_PHYSICAL_COUNT + " TEXT, "
        + COL_RM_BYPASS_COUNT + " TEXT, "
        + COL_RM_FAULTY_COUNT + " TEXT, "
        + COL_RM_REMARKS + " TEXT, "
        + COL_DG_MAKE + " TEXT, "
        + COL_DG_RATING + " TEXT, "
        + COL_GCU_MAKE + " TEXT, "
        + COL_GCU_MODEL + " TEXT, "
        + COL_GCU_STATUS + " TEXT, "
        + COL_FUEL_SENSOR_STATUS + " TEXT, "
        + COL_DG_AUTOMATION_STATUS + " TEXT, "
        + COL_DG_RUN_HOURS + " TEXT, "
        + COL_FUEL_LEVEL + " TEXT, "
        + COL_IS_SOLAR + " TEXT, "
        + COL_MPPT_MAKE + " TEXT, "
        + COL_MPPT_RATING + " TEXT, "
        + COL_MPPT_COUNT + " TEXT, "
        + COL_MPPT_FAULTY_COUNT + " TEXT, "
        + COL_VEGETATION_STATUS + " TEXT, "
        + COL_NOTES + " TEXT, "
        + COL_EXTRA_ATTRIBUTES + " TEXT, "
        + COL_UPDATED_AT + " INTEGER"
        + ");";

    db.execSQL(createSitesTable);
    db.execSQL("CREATE INDEX idx_telecom_site_id ON " + TABLE_SITES + " (" + COL_SITE_ID + ");");
    db.execSQL("CREATE INDEX idx_telecom_site_name ON " + TABLE_SITES + " (" + COL_SITE_NAME + ");");
    db.execSQL("CREATE INDEX idx_telecom_location ON " + TABLE_SITES + " (" + COL_LOCATION + ");");
    db.execSQL("CREATE INDEX idx_telecom_tech ON " + TABLE_SITES + " (" + COL_TECH_NAME + ");");
    db.execSQL("CREATE INDEX idx_telecom_coords ON " + TABLE_SITES + " (" + COL_LATITUDE + ", " + COL_LONGITUDE + ");");

    String createVisitPlanTable = "CREATE TABLE " + TABLE_VISIT_PLAN + " ("
        + COL_PLAN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
        + COL_PLAN_SITE_ID + " TEXT UNIQUE, "
        + COL_PLAN_ORDER + " INTEGER, "
        + COL_PLAN_NOTES + " TEXT, "
        + "FOREIGN KEY(" + COL_PLAN_SITE_ID + ") REFERENCES " + TABLE_SITES + "(" + COL_SITE_ID + ") ON DELETE CASCADE"
        + ");";
    db.execSQL(createVisitPlanTable);
  }

  @Override
  public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
    db.execSQL("DROP TABLE IF EXISTS " + TABLE_VISIT_PLAN);
    db.execSQL("DROP TABLE IF EXISTS " + TABLE_SITES);
    onCreate(db);
  }

  private ContentValues createContentValues(TelecomSite site) {
    ContentValues cv = new ContentValues();
    cv.put(COL_SITE_ID, site.getSiteId());
    cv.put(COL_SITE_NAME, site.getSiteName());
    cv.put(COL_SITE_TYPE, site.getSiteType());
    cv.put(COL_LOCATION, site.getLocation());
    cv.put(COL_ADDRESS, site.getAddress());
    cv.put(COL_LATITUDE, site.getLatitude());
    cv.put(COL_LONGITUDE, site.getLongitude());
    cv.put(COL_HAS_VALID_COORDS, site.hasValidCoords() ? 1 : 0);
    cv.put(COL_TECH_NAME, site.getTechnicianName());
    cv.put(COL_TECH_PHONE, site.getTechnicianPhone());
    cv.put(COL_TECH_PPRJ, site.getTechnicianPprj());
    cv.put(COL_SUPERVISOR_NAME, site.getSupervisorName());
    cv.put(COL_SUPERVISOR_PHONE, site.getSupervisorPhone());
    cv.put(COL_POWER_STATUS, site.getPowerStatus());
    cv.put(COL_EB_AVAILABILITY, site.getEbAvailability());
    cv.put(COL_SITE_LOAD_AMP, site.getSiteLoadAmp());
    cv.put(COL_CONSUMER_ID, site.getConsumerId());
    cv.put(COL_METER_NO, site.getMeterNo());
    cv.put(COL_INSTALLATION_NO, site.getInstallationNo());
    cv.put(COL_EB_DIVISION, site.getEbDivision());
    cv.put(COL_EB_CCC, site.getEbCcc());
    cv.put(COL_DOCKET_NO, site.getDocketNo());
    cv.put(COL_SMPS_MAKE, site.getSmpsMake());
    cv.put(COL_SMPS_RATING, site.getSmpsRating());
    cv.put(COL_SMPS_FAULT, site.getSmpsFault());
    cv.put(COL_SMPS_REMARKS, site.getSmpsRemarks());
    cv.put(COL_BM_MAKE, site.getBmMake());
    cv.put(COL_BM_RATING, site.getBmRating());
    cv.put(COL_BB_BACKUP, site.getBbBackup());
    cv.put(COL_BM_PHYSICAL_COUNT, site.getBmPhysicalCount());
    cv.put(COL_BM_FAULTY_COUNT, site.getBmFaultyCount());
    cv.put(COL_BM_REMARKS, site.getBmRemarks());
    cv.put(COL_BATTERY_SERIALS, site.getBatterySerials());
    cv.put(COL_RM_MAKE, site.getRmMake());
    cv.put(COL_RM_RATING, site.getRmRating());
    cv.put(COL_RM_PHYSICAL_COUNT, site.getRmPhysicalCount());
    cv.put(COL_RM_BYPASS_COUNT, site.getRmBypassCount());
    cv.put(COL_RM_FAULTY_COUNT, site.getRmFaultyCount());
    cv.put(COL_RM_REMARKS, site.getRmRemarks());
    cv.put(COL_DG_MAKE, site.getDgMake());
    cv.put(COL_DG_RATING, site.getDgRating());
    cv.put(COL_GCU_MAKE, site.getGcuMake());
    cv.put(COL_GCU_MODEL, site.getGcuModel());
    cv.put(COL_GCU_STATUS, site.getGcuStatus());
    cv.put(COL_FUEL_SENSOR_STATUS, site.getFuelSensorStatus());
    cv.put(COL_DG_AUTOMATION_STATUS, site.getDgAutomationStatus());
    cv.put(COL_DG_RUN_HOURS, site.getDgRunHours());
    cv.put(COL_FUEL_LEVEL, site.getFuelLevel());
    cv.put(COL_IS_SOLAR, site.getIsSolar());
    cv.put(COL_MPPT_MAKE, site.getMpptMake());
    cv.put(COL_MPPT_RATING, site.getMpptRating());
    cv.put(COL_MPPT_COUNT, site.getMpptCount());
    cv.put(COL_MPPT_FAULTY_COUNT, site.getMpptFaultyCount());
    cv.put(COL_VEGETATION_STATUS, site.getVegetationStatus());
    cv.put(COL_NOTES, site.getNotes());
    cv.put(COL_EXTRA_ATTRIBUTES, site.getExtraAttributesAsJson());
    cv.put(COL_UPDATED_AT, site.getUpdatedAt() > 0 ? site.getUpdatedAt() : System.currentTimeMillis());
    return cv;
  }

  private TelecomSite cursorToSite(Cursor c) {
    String siteId = c.getString(c.getColumnIndexOrThrow(COL_SITE_ID));
    TelecomSite site = new TelecomSite(siteId);
    site.setSiteName(c.getString(c.getColumnIndexOrThrow(COL_SITE_NAME)));
    site.setSiteType(c.getString(c.getColumnIndexOrThrow(COL_SITE_TYPE)));
    site.setLocation(c.getString(c.getColumnIndexOrThrow(COL_LOCATION)));
    site.setAddress(c.getString(c.getColumnIndexOrThrow(COL_ADDRESS)));
    double lat = c.getDouble(c.getColumnIndexOrThrow(COL_LATITUDE));
    double lon = c.getDouble(c.getColumnIndexOrThrow(COL_LONGITUDE));
    site.setCoordinates(lat, lon);
    site.setHasValidCoords(c.getInt(c.getColumnIndexOrThrow(COL_HAS_VALID_COORDS)) == 1);
    site.setTechnicianName(c.getString(c.getColumnIndexOrThrow(COL_TECH_NAME)));
    site.setTechnicianPhone(c.getString(c.getColumnIndexOrThrow(COL_TECH_PHONE)));
    site.setTechnicianPprj(c.getString(c.getColumnIndexOrThrow(COL_TECH_PPRJ)));
    site.setSupervisorName(c.getString(c.getColumnIndexOrThrow(COL_SUPERVISOR_NAME)));
    site.setSupervisorPhone(c.getString(c.getColumnIndexOrThrow(COL_SUPERVISOR_PHONE)));
    site.setPowerStatus(c.getString(c.getColumnIndexOrThrow(COL_POWER_STATUS)));
    site.setEbAvailability(c.getString(c.getColumnIndexOrThrow(COL_EB_AVAILABILITY)));
    site.setSiteLoadAmp(c.getString(c.getColumnIndexOrThrow(COL_SITE_LOAD_AMP)));
    site.setConsumerId(c.getString(c.getColumnIndexOrThrow(COL_CONSUMER_ID)));
    site.setMeterNo(c.getString(c.getColumnIndexOrThrow(COL_METER_NO)));
    site.setInstallationNo(c.getString(c.getColumnIndexOrThrow(COL_INSTALLATION_NO)));
    site.setEbDivision(c.getString(c.getColumnIndexOrThrow(COL_EB_DIVISION)));
    site.setEbCcc(c.getString(c.getColumnIndexOrThrow(COL_EB_CCC)));
    site.setDocketNo(c.getString(c.getColumnIndexOrThrow(COL_DOCKET_NO)));
    site.setSmpsMake(c.getString(c.getColumnIndexOrThrow(COL_SMPS_MAKE)));
    site.setSmpsRating(c.getString(c.getColumnIndexOrThrow(COL_SMPS_RATING)));
    site.setSmpsFault(c.getString(c.getColumnIndexOrThrow(COL_SMPS_FAULT)));
    site.setSmpsRemarks(c.getString(c.getColumnIndexOrThrow(COL_SMPS_REMARKS)));
    site.setBmMake(c.getString(c.getColumnIndexOrThrow(COL_BM_MAKE)));
    site.setBmRating(c.getString(c.getColumnIndexOrThrow(COL_BM_RATING)));
    site.setBbBackup(c.getString(c.getColumnIndexOrThrow(COL_BB_BACKUP)));
    site.setBmPhysicalCount(c.getString(c.getColumnIndexOrThrow(COL_BM_PHYSICAL_COUNT)));
    site.setBmFaultyCount(c.getString(c.getColumnIndexOrThrow(COL_BM_FAULTY_COUNT)));
    site.setBmRemarks(c.getString(c.getColumnIndexOrThrow(COL_BM_REMARKS)));
    site.setBatterySerials(c.getString(c.getColumnIndexOrThrow(COL_BATTERY_SERIALS)));
    site.setRmMake(c.getString(c.getColumnIndexOrThrow(COL_RM_MAKE)));
    site.setRmRating(c.getString(c.getColumnIndexOrThrow(COL_RM_RATING)));
    site.setRmPhysicalCount(c.getString(c.getColumnIndexOrThrow(COL_RM_PHYSICAL_COUNT)));
    site.setRmBypassCount(c.getString(c.getColumnIndexOrThrow(COL_RM_BYPASS_COUNT)));
    site.setRmFaultyCount(c.getString(c.getColumnIndexOrThrow(COL_RM_FAULTY_COUNT)));
    site.setRmRemarks(c.getString(c.getColumnIndexOrThrow(COL_RM_REMARKS)));
    site.setDgMake(c.getString(c.getColumnIndexOrThrow(COL_DG_MAKE)));
    site.setDgRating(c.getString(c.getColumnIndexOrThrow(COL_DG_RATING)));
    site.setGcuMake(c.getString(c.getColumnIndexOrThrow(COL_GCU_MAKE)));
    site.setGcuModel(c.getString(c.getColumnIndexOrThrow(COL_GCU_MODEL)));
    site.setGcuStatus(c.getString(c.getColumnIndexOrThrow(COL_GCU_STATUS)));
    site.setFuelSensorStatus(c.getString(c.getColumnIndexOrThrow(COL_FUEL_SENSOR_STATUS)));
    site.setDgAutomationStatus(c.getString(c.getColumnIndexOrThrow(COL_DG_AUTOMATION_STATUS)));
    site.setDgRunHours(c.getString(c.getColumnIndexOrThrow(COL_DG_RUN_HOURS)));
    site.setFuelLevel(c.getString(c.getColumnIndexOrThrow(COL_FUEL_LEVEL)));
    site.setIsSolar(c.getString(c.getColumnIndexOrThrow(COL_IS_SOLAR)));
    site.setMpptMake(c.getString(c.getColumnIndexOrThrow(COL_MPPT_MAKE)));
    site.setMpptRating(c.getString(c.getColumnIndexOrThrow(COL_MPPT_RATING)));
    site.setMpptCount(c.getString(c.getColumnIndexOrThrow(COL_MPPT_COUNT)));
    site.setMpptFaultyCount(c.getString(c.getColumnIndexOrThrow(COL_MPPT_FAULTY_COUNT)));
    site.setVegetationStatus(c.getString(c.getColumnIndexOrThrow(COL_VEGETATION_STATUS)));
    site.setNotes(c.getString(c.getColumnIndexOrThrow(COL_NOTES)));
    site.setExtraAttributesFromJson(c.getString(c.getColumnIndexOrThrow(COL_EXTRA_ATTRIBUTES)));
    site.setUpdatedAt(c.getLong(c.getColumnIndexOrThrow(COL_UPDATED_AT)));
    return site;
  }

  /**
   * Batch insert or update sites in an atomic transaction.
   * Returns an array with [newly_inserted_count, updated_count, skipped_count].
   */
  public int[] insertBatch(@NonNull List<TelecomSite> sites, @NonNull ImportMode mode) {
    int inserted = 0;
    int updated = 0;
    int skipped = 0;
    SQLiteDatabase db = getWritableDatabase();
    db.beginTransaction();
    try {
      if (mode == ImportMode.REPLACE_ALL) {
        db.delete(TABLE_SITES, null, null);
      }
      for (TelecomSite site : sites) {
        if (site.getSiteId().isEmpty()) {
          skipped++;
          continue;
        }
        ContentValues cv = createContentValues(site);
        if (mode == ImportMode.REPLACE_ALL) {
          long res = db.insert(TABLE_SITES, null, cv);
          if (res != -1) inserted++; else skipped++;
        } else if (mode == ImportMode.ADD_NEW) {
          long res = db.insertWithOnConflict(TABLE_SITES, null, cv, SQLiteDatabase.CONFLICT_IGNORE);
          if (res != -1) inserted++; else skipped++;
        } else { // UPDATE_EXISTING
          boolean exists = siteExists(db, site.getSiteId());
          long res = db.insertWithOnConflict(TABLE_SITES, null, cv, SQLiteDatabase.CONFLICT_REPLACE);
          if (res != -1) {
            if (exists) updated++; else inserted++;
          } else {
            skipped++;
          }
        }
      }
      db.setTransactionSuccessful();
    } finally {
      db.endTransaction();
    }
    return new int[]{inserted, updated, skipped};
  }

  public boolean siteExists(SQLiteDatabase db, String siteId) {
    Cursor c = db.rawQuery("SELECT 1 FROM " + TABLE_SITES + " WHERE " + COL_SITE_ID + " = ? LIMIT 1", new String[]{siteId});
    boolean exists = c.moveToFirst();
    c.close();
    return exists;
  }

  public boolean siteExists(String siteId) {
    return siteExists(getReadableDatabase(), siteId);
  }

  @Nullable
  public TelecomSite getSiteById(@NonNull String siteId) {
    SQLiteDatabase db = getReadableDatabase();
    Cursor c = db.query(TABLE_SITES, null, COL_SITE_ID + " = ?", new String[]{siteId.trim()}, null, null, null);
    TelecomSite site = null;
    if (c.moveToFirst()) {
      site = cursorToSite(c);
    }
    c.close();
    return site;
  }

  /**
   * Extracts search tokens from user query.
   * Splits on punctuation, spaces, and transitions between letters and digits (e.g. "kgpo9030" -> "kgpo", "9030", "rt01" -> "rt", "01").
   */
  @NonNull
  public static List<String> extractSearchTokens(@NonNull String query) {
    List<String> tokens = new ArrayList<>();
    String trimmed = query.trim();
    if (trimmed.isEmpty()) {
      return tokens;
    }

    String[] parts = trimmed.split("[\\s\\-_/.,+#:;]+");
    for (String part : parts) {
      if (part.isEmpty()) {
        continue;
      }
      Matcher matcher = Pattern.compile("([a-zA-Z]+|[0-9]+)").matcher(part);
      while (matcher.find()) {
        String token = matcher.group();
        if (!token.isEmpty()) {
          tokens.add(token);
        }
      }
    }
    return tokens;
  }

  /**
   * Fast offline search with smart SAP ID matching, sequence wildcard matching, and multi-token ranking.
   * Handles:
   * - "kgpo 9030", "slbn 9036", "egro 9006" skipping technology/state codes ("-ENB-", "-ESC-", "-ILA-")
   * - "kgpo9030", "slbn9036" (letters concatenated with digits)
   * - RailTel sites ("rt01", "rt 01", "rt10")
   * - COW sites ("cow 01", "kgpo cow")
   * - General search across site name, JC location, technician, remarks with strict precision.
   */
  @NonNull
  public List<TelecomSite> searchSites(@NonNull String query, int limit, int offset) {
    List<TelecomSite> results = new ArrayList<>();
    String q = query.trim();
    if (q.isEmpty()) {
      return getAllSites(limit > 0 ? limit : 50, offset);
    }

    SQLiteDatabase db = getReadableDatabase();
    List<String> tokens = extractSearchTokens(q);
    if (tokens.isEmpty()) {
      return getAllSites(limit > 0 ? limit : 50, offset);
    }

    StringBuilder sql = new StringBuilder();
    List<String> argsList = new ArrayList<>();
    String rawLikeQuery = "%" + q + "%";

    if (tokens.size() == 1) {
      String token = tokens.get(0);
      String likeToken = "%" + token + "%";
      String prefixToken = token + "%";

      sql.append("SELECT * FROM ").append(TABLE_SITES).append(" WHERE ")
         .append(COL_SITE_ID).append(" LIKE ? OR ")
         .append(COL_SITE_NAME).append(" LIKE ? OR ")
         .append(COL_LOCATION).append(" LIKE ? OR ")
         .append(COL_TECH_NAME).append(" LIKE ? OR ")
         .append(COL_SUPERVISOR_NAME).append(" LIKE ? OR ")
         .append(COL_NOTES).append(" LIKE ? ")
         .append(" ORDER BY ")
         .append("   CASE ")
         .append("     WHEN ").append(COL_SITE_ID).append(" = ? THEN 1 ")
         .append("     WHEN ").append(COL_SITE_ID).append(" LIKE ? THEN 2 ")
         .append("     WHEN ").append(COL_SITE_ID).append(" LIKE ? THEN 3 ")
         .append("     WHEN ").append(COL_SITE_NAME).append(" LIKE ? THEN 4 ")
         .append("     ELSE 5 ")
         .append("   END, ")
         .append("   ").append(COL_SITE_ID).append(" ASC ");

      argsList.add(likeToken);
      argsList.add(likeToken);
      argsList.add(likeToken);
      argsList.add(likeToken);
      argsList.add(likeToken);
      argsList.add(likeToken);

      // Order by args
      argsList.add(q);
      argsList.add(prefixToken);
      argsList.add(likeToken);
      argsList.add(prefixToken);
    } else {
      // Multi-token: build sequential pattern "%t1%t2%...%tn%" to skip intermediate hyphens and tech tags (ENB, ESC, ILA)
      StringBuilder seqSb = new StringBuilder("%");
      for (String t : tokens) {
        seqSb.append(t).append("%");
      }
      String seqPattern = seqSb.toString();

      sql.append("SELECT * FROM ").append(TABLE_SITES).append(" WHERE (");

      // 1. Site ID sequential match (e.g. "%kgpo%9030%" matches "I-WB-KGPO-ENB-9030" or "%rt%01%" matches "RT01")
      sql.append(COL_SITE_ID).append(" LIKE ? ");
      argsList.add(seqPattern);

      // 2. Site ID contains all tokens in any order (e.g. user typed number first "9030 kgpo")
      sql.append(" OR (");
      for (int i = 0; i < tokens.size(); i++) {
        if (i > 0) sql.append(" AND ");
        sql.append(COL_SITE_ID).append(" LIKE ?");
        argsList.add("%" + tokens.get(i) + "%");
      }
      sql.append(")");

      // 3. Literal continuous substring match across columns
      sql.append(" OR (")
         .append(COL_SITE_ID).append(" LIKE ? OR ")
         .append(COL_SITE_NAME).append(" LIKE ? OR ")
         .append(COL_LOCATION).append(" LIKE ? OR ")
         .append(COL_TECH_NAME).append(" LIKE ? OR ")
         .append(COL_SUPERVISOR_NAME).append(" LIKE ? OR ")
         .append(COL_NOTES).append(" LIKE ?")
         .append(")");
      argsList.add(rawLikeQuery);
      argsList.add(rawLikeQuery);
      argsList.add(rawLikeQuery);
      argsList.add(rawLikeQuery);
      argsList.add(rawLikeQuery);
      argsList.add(rawLikeQuery);

      // 4. Record-wide token conjunction (every token must be matched in at least one field)
      sql.append(" OR (");
      for (int i = 0; i < tokens.size(); i++) {
        if (i > 0) sql.append(" AND ");
        String tLike = "%" + tokens.get(i) + "%";
        sql.append("(")
           .append(COL_SITE_ID).append(" LIKE ? OR ")
           .append(COL_SITE_NAME).append(" LIKE ? OR ")
           .append(COL_LOCATION).append(" LIKE ? OR ")
           .append(COL_TECH_NAME).append(" LIKE ? OR ")
           .append(COL_SUPERVISOR_NAME).append(" LIKE ? OR ")
           .append(COL_NOTES).append(" LIKE ?")
           .append(")");
        argsList.add(tLike);
        argsList.add(tLike);
        argsList.add(tLike);
        argsList.add(tLike);
        argsList.add(tLike);
        argsList.add(tLike);
      }
      sql.append(")");

      sql.append(")"); // close WHERE

      // Relevance ranking
      sql.append(" ORDER BY ")
         .append("   CASE ")
         .append("     WHEN ").append(COL_SITE_ID).append(" = ? THEN 1 ")
         .append("     WHEN ").append(COL_SITE_ID).append(" LIKE ? THEN 2 ")
         .append("     WHEN ").append(COL_SITE_ID).append(" LIKE ? THEN 3 ")
         .append("     WHEN (");
      for (int i = 0; i < tokens.size(); i++) {
        if (i > 0) sql.append(" AND ");
        sql.append(COL_SITE_ID).append(" LIKE ?");
      }
      sql.append("     ) THEN 4 ")
         .append("     WHEN ").append(COL_SITE_NAME).append(" LIKE ? THEN 5 ")
         .append("     WHEN ").append(COL_LOCATION).append(" LIKE ? THEN 6 ")
         .append("     ELSE 7 ")
         .append("   END, ")
         .append("   ").append(COL_SITE_ID).append(" ASC ");

      argsList.add(q);
      argsList.add(q + "%");
      argsList.add(seqPattern);
      for (String t : tokens) {
        argsList.add("%" + t + "%");
      }
      argsList.add(seqPattern);
      argsList.add(seqPattern);
    }

    if (limit > 0) {
      sql.append(" LIMIT ").append(limit);
      if (offset > 0) {
        sql.append(" OFFSET ").append(offset);
      }
    }

    String[] args = argsList.toArray(new String[0]);
    Cursor c = db.rawQuery(sql.toString(), args);
    while (c.moveToNext()) {
      results.add(cursorToSite(c));
    }
    c.close();
    return results;
  }

  @NonNull
  public List<TelecomSite> searchSites(@NonNull String query, int limit) {
    return searchSites(query, limit, 0);
  }

  /**
   * Returns total count of sites matching a search query with the same smart matching logic.
   */
  public int countSites(@NonNull String query) {
    String q = query.trim();
    if (q.isEmpty()) {
      return getSitesCount();
    }
    List<String> tokens = extractSearchTokens(q);
    if (tokens.isEmpty()) {
      return getSitesCount();
    }

    SQLiteDatabase db = getReadableDatabase();
    StringBuilder sql = new StringBuilder();
    List<String> argsList = new ArrayList<>();

    if (tokens.size() == 1) {
      String token = tokens.get(0);
      String likeToken = "%" + token + "%";
      sql.append("SELECT COUNT(*) FROM ").append(TABLE_SITES).append(" WHERE ")
         .append(COL_SITE_ID).append(" LIKE ? OR ")
         .append(COL_SITE_NAME).append(" LIKE ? OR ")
         .append(COL_LOCATION).append(" LIKE ? OR ")
         .append(COL_TECH_NAME).append(" LIKE ? OR ")
         .append(COL_SUPERVISOR_NAME).append(" LIKE ? OR ")
         .append(COL_NOTES).append(" LIKE ?");
      argsList.add(likeToken);
      argsList.add(likeToken);
      argsList.add(likeToken);
      argsList.add(likeToken);
      argsList.add(likeToken);
      argsList.add(likeToken);
    } else {
      StringBuilder seqSb = new StringBuilder("%");
      for (String t : tokens) {
        seqSb.append(t).append("%");
      }
      String seqPattern = seqSb.toString();
      String rawLikeQuery = "%" + q + "%";

      sql.append("SELECT COUNT(*) FROM ").append(TABLE_SITES).append(" WHERE (")
         .append(COL_SITE_ID).append(" LIKE ? ");
      argsList.add(seqPattern);

      sql.append(" OR (");
      for (int i = 0; i < tokens.size(); i++) {
        if (i > 0) sql.append(" AND ");
        sql.append(COL_SITE_ID).append(" LIKE ?");
        argsList.add("%" + tokens.get(i) + "%");
      }
      sql.append(")");

      sql.append(" OR (")
         .append(COL_SITE_ID).append(" LIKE ? OR ")
         .append(COL_SITE_NAME).append(" LIKE ? OR ")
         .append(COL_LOCATION).append(" LIKE ? OR ")
         .append(COL_TECH_NAME).append(" LIKE ? OR ")
         .append(COL_SUPERVISOR_NAME).append(" LIKE ? OR ")
         .append(COL_NOTES).append(" LIKE ?")
         .append(")");
      argsList.add(rawLikeQuery);
      argsList.add(rawLikeQuery);
      argsList.add(rawLikeQuery);
      argsList.add(rawLikeQuery);
      argsList.add(rawLikeQuery);
      argsList.add(rawLikeQuery);

      sql.append(" OR (");
      for (int i = 0; i < tokens.size(); i++) {
        if (i > 0) sql.append(" AND ");
        String tLike = "%" + tokens.get(i) + "%";
        sql.append("(")
           .append(COL_SITE_ID).append(" LIKE ? OR ")
           .append(COL_SITE_NAME).append(" LIKE ? OR ")
           .append(COL_LOCATION).append(" LIKE ? OR ")
           .append(COL_TECH_NAME).append(" LIKE ? OR ")
           .append(COL_SUPERVISOR_NAME).append(" LIKE ? OR ")
           .append(COL_NOTES).append(" LIKE ?")
           .append(")");
        argsList.add(tLike);
        argsList.add(tLike);
        argsList.add(tLike);
        argsList.add(tLike);
        argsList.add(tLike);
        argsList.add(tLike);
      }
      sql.append("))");
    }

    String[] args = argsList.toArray(new String[0]);
    Cursor c = db.rawQuery(sql.toString(), args);
    int count = 0;
    if (c.moveToFirst()) {
      count = c.getInt(0);
    }
    c.close();
    return count;
  }

  @NonNull
  public List<TelecomSite> getAllSites(int limit, int offset) {
    List<TelecomSite> list = new ArrayList<>();
    SQLiteDatabase db = getReadableDatabase();
    StringBuilder query = new StringBuilder("SELECT * FROM " + TABLE_SITES + " ORDER BY " + COL_SITE_ID + " ASC");
    if (limit > 0) {
      query.append(" LIMIT ").append(limit);
      if (offset > 0) {
        query.append(" OFFSET ").append(offset);
      }
    }
    Cursor c = db.rawQuery(query.toString(), null);
    while (c.moveToNext()) {
      list.add(cursorToSite(c));
    }
    c.close();
    return list;
  }

  @NonNull
  public List<TelecomSite> getAllSites(int limit) {
    return getAllSites(limit, 0);
  }

  @NonNull
  public List<TelecomSite> getAllSites() {
    return getAllSites(-1, 0);
  }

  @NonNull
  public List<TelecomSite> getAllSitesWithCoordinates() {
    List<TelecomSite> list = new ArrayList<>();
    SQLiteDatabase db = getReadableDatabase();
    Cursor c = db.query(TABLE_SITES, null, COL_HAS_VALID_COORDS + " = 1", null, null, null, COL_SITE_ID + " ASC");
    while (c.moveToNext()) {
      list.add(cursorToSite(c));
    }
    c.close();
    return list;
  }

  public int getSitesCount() {
    SQLiteDatabase db = getReadableDatabase();
    Cursor c = db.rawQuery("SELECT COUNT(*) FROM " + TABLE_SITES, null);
    int count = 0;
    if (c.moveToFirst()) {
      count = c.getInt(0);
    }
    c.close();
    return count;
  }

  public int getValidCoordsSitesCount() {
    SQLiteDatabase db = getReadableDatabase();
    Cursor c = db.rawQuery("SELECT COUNT(*) FROM " + TABLE_SITES + " WHERE " + COL_HAS_VALID_COORDS + " = 1", null);
    int count = 0;
    if (c.moveToFirst()) {
      count = c.getInt(0);
    }
    c.close();
    return count;
  }

  public void clearAllSites() {
    SQLiteDatabase db = getWritableDatabase();
    db.delete(TABLE_VISIT_PLAN, null, null);
    db.delete(TABLE_SITES, null, null);
  }

  /**
   * Calculates the great-circle distance between two geographic coordinates using the Haversine formula.
   *
   * @return Distance in meters.
   */
  public static double calculateDistanceMeters(double lat1, double lon1, double lat2, double lon2) {
    final double R = 6371000.0; // Earth radius in meters
    double dLat = Math.toRadians(lat2 - lat1);
    double dLon = Math.toRadians(lon2 - lon1);
    double a = Math.sin(dLat / 2.0) * Math.sin(dLat / 2.0)
        + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
        * Math.sin(dLon / 2.0) * Math.sin(dLon / 2.0);
    double c = 2.0 * Math.atan2(Math.sqrt(a), Math.sqrt(1.0 - a));
    return R * c;
  }

  /**
   * Queries sites within a given radius (in meters) from a center coordinate.
   * Utilizes indexed SQLite bounding box pre-filtering followed by exact Haversine distance computation.
   *
   * @param centerLat Center latitude.
   * @param centerLon Center longitude.
   * @param radiusMeters Maximum distance in meters.
   * @param limit Maximum results to return (<= 0 for unlimited).
   * @return List of TelecomSite objects sorted in ascending order of distance (nearest first).
   */
  @NonNull
  public List<TelecomSite> getSitesWithinRadius(double centerLat, double centerLon, double radiusMeters, int limit) {
    List<TelecomSite> results = new ArrayList<>();
    if (radiusMeters <= 0.0) {
      return results;
    }

    // Rough bounding box: 1 deg lat ~ 111.32 km
    double latDelta = radiusMeters / 111320.0;
    double latMin = centerLat - latDelta;
    double latMax = centerLat + latDelta;
    double cosLat = Math.cos(Math.toRadians(centerLat));
    double lonDelta = (cosLat > 0.0001) ? (radiusMeters / (111320.0 * cosLat)) : 180.0;
    double lonMin = centerLon - lonDelta;
    double lonMax = centerLon + lonDelta;

    SQLiteDatabase db = getReadableDatabase();
    String selection = COL_HAS_VALID_COORDS + " = 1 AND "
        + COL_LATITUDE + " >= ? AND " + COL_LATITUDE + " <= ? AND "
        + COL_LONGITUDE + " >= ? AND " + COL_LONGITUDE + " <= ?";
    String[] args = new String[] {
        String.valueOf(latMin),
        String.valueOf(latMax),
        String.valueOf(lonMin),
        String.valueOf(lonMax)
    };

    Cursor c = db.query(TABLE_SITES, null, selection, args, null, null, null);
    while (c.moveToNext()) {
      TelecomSite site = cursorToSite(c);
      double dist = calculateDistanceMeters(centerLat, centerLon, site.getLatitude(), site.getLongitude());
      if (dist <= radiusMeters) {
        site.setDistanceMeters(dist);
        results.add(site);
      }
    }
    c.close();

    // Sort ascending: nearest first
    Collections.sort(results, new Comparator<TelecomSite>() {
      @Override
      public int compare(TelecomSite o1, TelecomSite o2) {
        return Double.compare(o1.getDistanceMeters(), o2.getDistanceMeters());
      }
    });

    if (limit > 0 && results.size() > limit) {
      return new ArrayList<>(results.subList(0, limit));
    }
    return results;
  }

  @NonNull
  public List<TelecomSite> getSitesWithinRadius(double centerLat, double centerLon, double radiusMeters) {
    return getSitesWithinRadius(centerLat, centerLon, radiusMeters, -1);
  }


  // Visit Plan Operations
  public void saveVisitPlan(@NonNull List<TelecomSite> sites) {
    SQLiteDatabase db = getWritableDatabase();
    db.beginTransaction();
    try {
      db.delete(TABLE_VISIT_PLAN, null, null);
      for (int i = 0; i < sites.size(); i++) {
        ContentValues cv = new ContentValues();
        cv.put(COL_PLAN_SITE_ID, sites.get(i).getSiteId());
        cv.put(COL_PLAN_ORDER, i + 1);
        db.insert(TABLE_VISIT_PLAN, null, cv);
      }
      db.setTransactionSuccessful();
    } finally {
      db.endTransaction();
    }
  }

  @NonNull
  public List<TelecomSite> getVisitPlan() {
    List<TelecomSite> plan = new ArrayList<>();
    SQLiteDatabase db = getReadableDatabase();
    String query = "SELECT s.* FROM " + TABLE_SITES + " s "
        + " INNER JOIN " + TABLE_VISIT_PLAN + " p ON s." + COL_SITE_ID + " = p." + COL_PLAN_SITE_ID
        + " ORDER BY p." + COL_PLAN_ORDER + " ASC";
    Cursor c = db.rawQuery(query, null);
    while (c.moveToNext()) {
      plan.add(cursorToSite(c));
    }
    c.close();
    return plan;
  }

  public void clearVisitPlan() {
    SQLiteDatabase db = getWritableDatabase();
    db.delete(TABLE_VISIT_PLAN, null, null);
  }

  public void addToVisitPlan(@NonNull String siteId) {
    SQLiteDatabase db = getWritableDatabase();
    Cursor c = db.rawQuery("SELECT MAX(" + COL_PLAN_ORDER + ") FROM " + TABLE_VISIT_PLAN, null);
    int nextOrder = 1;
    if (c.moveToFirst() && !c.isNull(0)) {
      nextOrder = c.getInt(0) + 1;
    }
    c.close();

    ContentValues cv = new ContentValues();
    cv.put(COL_PLAN_SITE_ID, siteId);
    cv.put(COL_PLAN_ORDER, nextOrder);
    db.insertWithOnConflict(TABLE_VISIT_PLAN, null, cv, SQLiteDatabase.CONFLICT_IGNORE);
  }

  public void removeFromVisitPlan(@NonNull String siteId) {
    SQLiteDatabase db = getWritableDatabase();
    db.delete(TABLE_VISIT_PLAN, COL_PLAN_SITE_ID + " = ?", new String[]{siteId});
  }

  public boolean isInVisitPlan(@NonNull String siteId) {
    SQLiteDatabase db = getReadableDatabase();
    Cursor c = db.rawQuery("SELECT 1 FROM " + TABLE_VISIT_PLAN + " WHERE " + COL_PLAN_SITE_ID + " = ? LIMIT 1", new String[]{siteId});
    boolean inPlan = c.moveToFirst();
    c.close();
    return inPlan;
  }

  @NonNull
  public List<String> getAllDistinctJcNames() {
    List<String> list = new ArrayList<>();
    SQLiteDatabase db = getReadableDatabase();
    Cursor c = db.rawQuery("SELECT DISTINCT " + COL_LOCATION + " FROM " + TABLE_SITES + " WHERE " + COL_LOCATION + " IS NOT NULL AND TRIM(" + COL_LOCATION + ") != '' ORDER BY " + COL_LOCATION + " ASC", null);
    while (c.moveToNext()) {
      list.add(c.getString(0).trim());
    }
    c.close();
    return list;
  }

  @NonNull
  public List<String> getAllDistinctTechnicians() {
    List<String> list = new ArrayList<>();
    SQLiteDatabase db = getReadableDatabase();
    Cursor c = db.rawQuery("SELECT DISTINCT " + COL_TECH_NAME + " FROM " + TABLE_SITES + " WHERE " + COL_TECH_NAME + " IS NOT NULL AND TRIM(" + COL_TECH_NAME + ") != '' ORDER BY " + COL_TECH_NAME + " ASC", null);
    while (c.moveToNext()) {
      list.add(c.getString(0).trim());
    }
    c.close();
    return list;
  }
}
