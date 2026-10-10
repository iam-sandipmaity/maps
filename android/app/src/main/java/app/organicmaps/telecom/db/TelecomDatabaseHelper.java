package app.organicmaps.telecom.db;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import app.organicmaps.telecom.model.TelecomSite;
import java.util.ArrayList;
import java.util.List;

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
   * Fast offline search by Site ID, partial Site ID, site name, JC location, technician, or remarks.
   */
  @NonNull
  public List<TelecomSite> searchSites(@NonNull String query, int limit) {
    List<TelecomSite> results = new ArrayList<>();
    String q = query.trim();
    if (q.isEmpty()) {
      return getAllSites(limit > 0 ? limit : 50);
    }

    SQLiteDatabase db = getReadableDatabase();
    String likeQuery = "%" + q + "%";
    String exactQuery = q;

    // Search query prioritizing exact or prefix site_id match first, then partial match across fields
    String sql = "SELECT * FROM " + TABLE_SITES + " WHERE "
        + COL_SITE_ID + " LIKE ? OR "
        + COL_SITE_NAME + " LIKE ? OR "
        + COL_LOCATION + " LIKE ? OR "
        + COL_TECH_NAME + " LIKE ? OR "
        + COL_SUPERVISOR_NAME + " LIKE ? OR "
        + COL_NOTES + " LIKE ? "
        + " ORDER BY "
        + "   CASE "
        + "     WHEN " + COL_SITE_ID + " = ? THEN 1 "
        + "     WHEN " + COL_SITE_ID + " LIKE ? THEN 2 "
        + "     WHEN " + COL_SITE_NAME + " LIKE ? THEN 3 "
        + "     ELSE 4 "
        + "   END, "
        + "   " + COL_SITE_ID + " ASC "
        + (limit > 0 ? " LIMIT " + limit : "");

    String prefixQuery = q + "%";
    String[] args = new String[]{likeQuery, likeQuery, likeQuery, likeQuery, likeQuery, likeQuery,
        exactQuery, prefixQuery, prefixQuery};

    Cursor c = db.rawQuery(sql, args);
    while (c.moveToNext()) {
      results.add(cursorToSite(c));
    }
    c.close();
    return results;
  }

  @NonNull
  public List<TelecomSite> getAllSites(int limit) {
    List<TelecomSite> list = new ArrayList<>();
    SQLiteDatabase db = getReadableDatabase();
    String query = "SELECT * FROM " + TABLE_SITES + " ORDER BY " + COL_SITE_ID + " ASC"
        + (limit > 0 ? " LIMIT " + limit : "");
    Cursor c = db.rawQuery(query, null);
    while (c.moveToNext()) {
      list.add(cursorToSite(c));
    }
    c.close();
    return list;
  }

  @NonNull
  public List<TelecomSite> getAllSites() {
    return getAllSites(-1);
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
}
