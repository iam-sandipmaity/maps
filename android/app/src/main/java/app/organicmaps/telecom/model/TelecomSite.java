package app.organicmaps.telecom.model;

import android.os.Parcel;
import android.os.Parcelable;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import org.json.JSONException;
import org.json.JSONObject;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * Represents a complete Telecom Site record, including identifiers, coordinates,
 * personnel contacts, electricity board (EB) details, DC power equipment (SMPS, BM, RM),
 * diesel generator (DG) equipment, solar details, vegetation, and extensible attributes.
 */
public class TelecomSite implements Serializable, Parcelable {

  private static final long serialVersionUID = 1L;

  @NonNull
  private String siteId;
  @NonNull
  private String siteName;
  private String siteType;
  private String location; // e.g., JC Name / Area / District
  private String address;
  private double latitude;
  private double longitude;
  private boolean hasValidCoords;

  // Personnel & Contacts
  private String technicianName;
  private String technicianPhone;
  private String technicianPprj;
  private String supervisorName;
  private String supervisorPhone;

  // Power & Electricity Board (EB)
  private String powerStatus;
  private String ebAvailability;
  private String siteLoadAmp;
  private String consumerId;
  private String meterNo;
  private String installationNo;
  private String ebDivision;
  private String ebCcc;
  private String docketNo;

  // Equipment: SMPS (DC Plant)
  private String smpsMake;
  private String smpsRating;
  private String smpsFault;
  private String smpsRemarks;

  // Equipment: Battery Bank (BM)
  private String bmMake;
  private String bmRating;
  private String bbBackup;
  private String bmPhysicalCount;
  private String bmFaultyCount;
  private String bmRemarks;
  private String batterySerials; // e.g., JSON string or comma-separated

  // Equipment: Rectifier Module (RM)
  private String rmMake;
  private String rmRating;
  private String rmPhysicalCount;
  private String rmBypassCount;
  private String rmFaultyCount;
  private String rmRemarks;

  // Equipment: Diesel Generator (DG) & GCU
  private String dgMake;
  private String dgRating;
  private String gcuMake;
  private String gcuModel;
  private String gcuStatus;
  private String fuelSensorStatus;
  private String dgAutomationStatus;
  private String dgRunHours;
  private String fuelLevel;

  // Solar & Environmental
  private String isSolar;
  private String mpptMake;
  private String mpptRating;
  private String mpptCount;
  private String mpptFaultyCount;
  private String vegetationStatus;

  // Notes & Extensible Attributes
  private String notes;
  @NonNull
  private Map<String, String> extraAttributes;
  private long updatedAt;

  public TelecomSite(@NonNull String siteId) {
    this.siteId = siteId.trim();
    this.siteName = "";
    this.siteType = "";
    this.location = "";
    this.address = "";
    this.latitude = 0.0;
    this.longitude = 0.0;
    this.hasValidCoords = false;
    this.notes = "";
    this.extraAttributes = new HashMap<>();
    this.updatedAt = System.currentTimeMillis();
  }

  protected TelecomSite(Parcel in) {
    siteId = in.readString();
    siteName = in.readString();
    siteType = in.readString();
    location = in.readString();
    address = in.readString();
    latitude = in.readDouble();
    longitude = in.readDouble();
    hasValidCoords = in.readByte() != 0;
    technicianName = in.readString();
    technicianPhone = in.readString();
    technicianPprj = in.readString();
    supervisorName = in.readString();
    supervisorPhone = in.readString();
    powerStatus = in.readString();
    ebAvailability = in.readString();
    siteLoadAmp = in.readString();
    consumerId = in.readString();
    meterNo = in.readString();
    installationNo = in.readString();
    ebDivision = in.readString();
    ebCcc = in.readString();
    docketNo = in.readString();
    smpsMake = in.readString();
    smpsRating = in.readString();
    smpsFault = in.readString();
    smpsRemarks = in.readString();
    bmMake = in.readString();
    bmRating = in.readString();
    bbBackup = in.readString();
    bmPhysicalCount = in.readString();
    bmFaultyCount = in.readString();
    bmRemarks = in.readString();
    batterySerials = in.readString();
    rmMake = in.readString();
    rmRating = in.readString();
    rmPhysicalCount = in.readString();
    rmBypassCount = in.readString();
    rmFaultyCount = in.readString();
    rmRemarks = in.readString();
    dgMake = in.readString();
    dgRating = in.readString();
    gcuMake = in.readString();
    gcuModel = in.readString();
    gcuStatus = in.readString();
    fuelSensorStatus = in.readString();
    dgAutomationStatus = in.readString();
    dgRunHours = in.readString();
    fuelLevel = in.readString();
    isSolar = in.readString();
    mpptMake = in.readString();
    mpptRating = in.readString();
    mpptCount = in.readString();
    mpptFaultyCount = in.readString();
    vegetationStatus = in.readString();
    notes = in.readString();
    updatedAt = in.readLong();
    extraAttributes = new HashMap<>();
    String jsonStr = in.readString();
    if (jsonStr != null && !jsonStr.isEmpty()) {
      try {
        JSONObject obj = new JSONObject(jsonStr);
        Iterator<String> keys = obj.keys();
        while (keys.hasNext()) {
          String key = keys.next();
          extraAttributes.put(key, obj.optString(key));
        }
      } catch (JSONException ignored) {}
    }
  }

  @Override
  public void writeToParcel(Parcel dest, int flags) {
    dest.writeString(siteId);
    dest.writeString(siteName);
    dest.writeString(siteType);
    dest.writeString(location);
    dest.writeString(address);
    dest.writeDouble(latitude);
    dest.writeDouble(longitude);
    dest.writeByte((byte) (hasValidCoords ? 1 : 0));
    dest.writeString(technicianName);
    dest.writeString(technicianPhone);
    dest.writeString(technicianPprj);
    dest.writeString(supervisorName);
    dest.writeString(supervisorPhone);
    dest.writeString(powerStatus);
    dest.writeString(ebAvailability);
    dest.writeString(siteLoadAmp);
    dest.writeString(consumerId);
    dest.writeString(meterNo);
    dest.writeString(installationNo);
    dest.writeString(ebDivision);
    dest.writeString(ebCcc);
    dest.writeString(docketNo);
    dest.writeString(smpsMake);
    dest.writeString(smpsRating);
    dest.writeString(smpsFault);
    dest.writeString(smpsRemarks);
    dest.writeString(bmMake);
    dest.writeString(bmRating);
    dest.writeString(bbBackup);
    dest.writeString(bmPhysicalCount);
    dest.writeString(bmFaultyCount);
    dest.writeString(bmRemarks);
    dest.writeString(batterySerials);
    dest.writeString(rmMake);
    dest.writeString(rmRating);
    dest.writeString(rmPhysicalCount);
    dest.writeString(rmBypassCount);
    dest.writeString(rmFaultyCount);
    dest.writeString(rmRemarks);
    dest.writeString(dgMake);
    dest.writeString(dgRating);
    dest.writeString(gcuMake);
    dest.writeString(gcuModel);
    dest.writeString(gcuStatus);
    dest.writeString(fuelSensorStatus);
    dest.writeString(dgAutomationStatus);
    dest.writeString(dgRunHours);
    dest.writeString(fuelLevel);
    dest.writeString(isSolar);
    dest.writeString(mpptMake);
    dest.writeString(mpptRating);
    dest.writeString(mpptCount);
    dest.writeString(mpptFaultyCount);
    dest.writeString(vegetationStatus);
    dest.writeString(notes);
    dest.writeLong(updatedAt);
    dest.writeString(getExtraAttributesAsJson());
  }

  @Override
  public int describeContents() {
    return 0;
  }

  public static final Creator<TelecomSite> CREATOR = new Creator<TelecomSite>() {
    @Override
    public TelecomSite createFromParcel(Parcel in) {
      return new TelecomSite(in);
    }

    @Override
    public TelecomSite[] newArray(int size) {
      return new TelecomSite[size];
    }
  };

  /**
   * Validates latitude and longitude ranges.
   * Returns true if -90 <= lat <= 90 and -180 <= lon <= 180, excluding exactly 0,0 null coordinates.
   */
  public static boolean validateCoordinates(double lat, double lon) {
    if (Double.isNaN(lat) || Double.isNaN(lon) || Double.isInfinite(lat) || Double.isInfinite(lon)) {
      return false;
    }
    if (lat < -90.0 || lat > 90.0 || lon < -180.0 || lon > 180.0) {
      return false;
    }
    // Check for 0.0, 0.0 (Gulf of Guinea placeholder)
    return Math.abs(lat) > 0.000001 || Math.abs(lon) > 0.000001;
  }

  public void setCoordinates(double lat, double lon) {
    this.latitude = lat;
    this.longitude = lon;
    this.hasValidCoords = validateCoordinates(lat, lon);
  }

  // Getters & Setters
  @NonNull
  public String getSiteId() {
    return siteId;
  }

  public void setSiteId(@NonNull String siteId) {
    this.siteId = siteId.trim();
  }

  @NonNull
  public String getSiteName() {
    return siteName != null ? siteName : "";
  }

  public void setSiteName(String siteName) {
    this.siteName = siteName != null ? siteName.trim() : "";
  }

  public String getSiteType() {
    return siteType != null ? siteType : "";
  }

  public void setSiteType(String siteType) {
    this.siteType = siteType != null ? siteType.trim() : "";
  }

  public String getLocation() {
    return location != null ? location : "";
  }

  public void setLocation(String location) {
    this.location = location != null ? location.trim() : "";
  }

  public String getAddress() {
    return address != null ? address : "";
  }

  public void setAddress(String address) {
    this.address = address != null ? address.trim() : "";
  }

  public double getLatitude() {
    return latitude;
  }

  public double getLongitude() {
    return longitude;
  }

  public boolean hasValidCoords() {
    return hasValidCoords;
  }

  public boolean hasValidCoordinates() {
    return hasValidCoords;
  }

  public void setHasValidCoords(boolean hasValidCoords) {
    this.hasValidCoords = hasValidCoords;
  }

  public String getTechnicianName() {
    return technicianName != null ? technicianName : "";
  }

  public void setTechnicianName(String technicianName) {
    this.technicianName = technicianName != null ? technicianName.trim() : "";
  }

  public String getTechnicianPhone() {
    return technicianPhone != null ? technicianPhone : "";
  }

  public void setTechnicianPhone(String technicianPhone) {
    this.technicianPhone = technicianPhone != null ? technicianPhone.trim() : "";
  }

  public String getTechnicianPprj() {
    return technicianPprj != null ? technicianPprj : "";
  }

  public void setTechnicianPprj(String technicianPprj) {
    this.technicianPprj = technicianPprj != null ? technicianPprj.trim() : "";
  }

  public String getSupervisorName() {
    return supervisorName != null ? supervisorName : "";
  }

  public void setSupervisorName(String supervisorName) {
    this.supervisorName = supervisorName != null ? supervisorName.trim() : "";
  }

  public String getSupervisorPhone() {
    return supervisorPhone != null ? supervisorPhone : "";
  }

  public void setSupervisorPhone(String supervisorPhone) {
    this.supervisorPhone = supervisorPhone != null ? supervisorPhone.trim() : "";
  }

  public String getPowerStatus() {
    return powerStatus != null ? powerStatus : "";
  }

  public void setPowerStatus(String powerStatus) {
    this.powerStatus = powerStatus != null ? powerStatus.trim() : "";
  }

  public String getEbAvailability() {
    return ebAvailability != null ? ebAvailability : "";
  }

  public void setEbAvailability(String ebAvailability) {
    this.ebAvailability = ebAvailability != null ? ebAvailability.trim() : "";
  }

  public String getSiteLoadAmp() {
    return siteLoadAmp != null ? siteLoadAmp : "";
  }

  public void setSiteLoadAmp(String siteLoadAmp) {
    this.siteLoadAmp = siteLoadAmp != null ? siteLoadAmp.trim() : "";
  }

  public String getConsumerId() {
    return consumerId != null ? consumerId : "";
  }

  public void setConsumerId(String consumerId) {
    this.consumerId = consumerId != null ? consumerId.trim() : "";
  }

  public String getMeterNo() {
    return meterNo != null ? meterNo : "";
  }

  public void setMeterNo(String meterNo) {
    this.meterNo = meterNo != null ? meterNo.trim() : "";
  }

  public String getInstallationNo() {
    return installationNo != null ? installationNo : "";
  }

  public void setInstallationNo(String installationNo) {
    this.installationNo = installationNo != null ? installationNo.trim() : "";
  }

  public String getEbDivision() {
    return ebDivision != null ? ebDivision : "";
  }

  public void setEbDivision(String ebDivision) {
    this.ebDivision = ebDivision != null ? ebDivision.trim() : "";
  }

  public String getEbCcc() {
    return ebCcc != null ? ebCcc : "";
  }

  public void setEbCcc(String ebCcc) {
    this.ebCcc = ebCcc != null ? ebCcc.trim() : "";
  }

  public String getDocketNo() {
    return docketNo != null ? docketNo : "";
  }

  public void setDocketNo(String docketNo) {
    this.docketNo = docketNo != null ? docketNo.trim() : "";
  }

  public String getSmpsMake() {
    return smpsMake != null ? smpsMake : "";
  }

  public void setSmpsMake(String smpsMake) {
    this.smpsMake = smpsMake != null ? smpsMake.trim() : "";
  }

  public String getSmpsRating() {
    return smpsRating != null ? smpsRating : "";
  }

  public void setSmpsRating(String smpsRating) {
    this.smpsRating = smpsRating != null ? smpsRating.trim() : "";
  }

  public String getSmpsFault() {
    return smpsFault != null ? smpsFault : "";
  }

  public void setSmpsFault(String smpsFault) {
    this.smpsFault = smpsFault != null ? smpsFault.trim() : "";
  }

  public String getSmpsRemarks() {
    return smpsRemarks != null ? smpsRemarks : "";
  }

  public void setSmpsRemarks(String smpsRemarks) {
    this.smpsRemarks = smpsRemarks != null ? smpsRemarks.trim() : "";
  }

  public String getBmMake() {
    return bmMake != null ? bmMake : "";
  }

  public void setBmMake(String bmMake) {
    this.bmMake = bmMake != null ? bmMake.trim() : "";
  }

  public String getBmRating() {
    return bmRating != null ? bmRating : "";
  }

  public void setBmRating(String bmRating) {
    this.bmRating = bmRating != null ? bmRating.trim() : "";
  }

  public String getBbBackup() {
    return bbBackup != null ? bbBackup : "";
  }

  public void setBbBackup(String bbBackup) {
    this.bbBackup = bbBackup != null ? bbBackup.trim() : "";
  }

  public String getBmPhysicalCount() {
    return bmPhysicalCount != null ? bmPhysicalCount : "";
  }

  public void setBmPhysicalCount(String bmPhysicalCount) {
    this.bmPhysicalCount = bmPhysicalCount != null ? bmPhysicalCount.trim() : "";
  }

  public String getBmFaultyCount() {
    return bmFaultyCount != null ? bmFaultyCount : "";
  }

  public void setBmFaultyCount(String bmFaultyCount) {
    this.bmFaultyCount = bmFaultyCount != null ? bmFaultyCount.trim() : "";
  }

  public String getBmRemarks() {
    return bmRemarks != null ? bmRemarks : "";
  }

  public void setBmRemarks(String bmRemarks) {
    this.bmRemarks = bmRemarks != null ? bmRemarks.trim() : "";
  }

  public String getBatterySerials() {
    return batterySerials != null ? batterySerials : "";
  }

  public void setBatterySerials(String batterySerials) {
    this.batterySerials = batterySerials != null ? batterySerials.trim() : "";
  }

  public String getRmMake() {
    return rmMake != null ? rmMake : "";
  }

  public void setRmMake(String rmMake) {
    this.rmMake = rmMake != null ? rmMake.trim() : "";
  }

  public String getRmRating() {
    return rmRating != null ? rmRating : "";
  }

  public void setRmRating(String rmRating) {
    this.rmRating = rmRating != null ? rmRating.trim() : "";
  }

  public String getRmPhysicalCount() {
    return rmPhysicalCount != null ? rmPhysicalCount : "";
  }

  public void setRmPhysicalCount(String rmPhysicalCount) {
    this.rmPhysicalCount = rmPhysicalCount != null ? rmPhysicalCount.trim() : "";
  }

  public String getRmBypassCount() {
    return rmBypassCount != null ? rmBypassCount : "";
  }

  public void setRmBypassCount(String rmBypassCount) {
    this.rmBypassCount = rmBypassCount != null ? rmBypassCount.trim() : "";
  }

  public String getRmFaultyCount() {
    return rmFaultyCount != null ? rmFaultyCount : "";
  }

  public void setRmFaultyCount(String rmFaultyCount) {
    this.rmFaultyCount = rmFaultyCount != null ? rmFaultyCount.trim() : "";
  }

  public String getRmRemarks() {
    return rmRemarks != null ? rmRemarks : "";
  }

  public void setRmRemarks(String rmRemarks) {
    this.rmRemarks = rmRemarks != null ? rmRemarks.trim() : "";
  }

  public String getDgMake() {
    return dgMake != null ? dgMake : "";
  }

  public void setDgMake(String dgMake) {
    this.dgMake = dgMake != null ? dgMake.trim() : "";
  }

  public String getDgRating() {
    return dgRating != null ? dgRating : "";
  }

  public void setDgRating(String dgRating) {
    this.dgRating = dgRating != null ? dgRating.trim() : "";
  }

  public String getGcuMake() {
    return gcuMake != null ? gcuMake : "";
  }

  public void setGcuMake(String gcuMake) {
    this.gcuMake = gcuMake != null ? gcuMake.trim() : "";
  }

  public String getGcuModel() {
    return gcuModel != null ? gcuModel : "";
  }

  public void setGcuModel(String gcuModel) {
    this.gcuModel = gcuModel != null ? gcuModel.trim() : "";
  }

  public String getGcuStatus() {
    return gcuStatus != null ? gcuStatus : "";
  }

  public void setGcuStatus(String gcuStatus) {
    this.gcuStatus = gcuStatus != null ? gcuStatus.trim() : "";
  }

  public String getFuelSensorStatus() {
    return fuelSensorStatus != null ? fuelSensorStatus : "";
  }

  public void setFuelSensorStatus(String fuelSensorStatus) {
    this.fuelSensorStatus = fuelSensorStatus != null ? fuelSensorStatus.trim() : "";
  }

  public String getDgAutomationStatus() {
    return dgAutomationStatus != null ? dgAutomationStatus : "";
  }

  public void setDgAutomationStatus(String dgAutomationStatus) {
    this.dgAutomationStatus = dgAutomationStatus != null ? dgAutomationStatus.trim() : "";
  }

  public String getDgRunHours() {
    return dgRunHours != null ? dgRunHours : "";
  }

  public void setDgRunHours(String dgRunHours) {
    this.dgRunHours = dgRunHours != null ? dgRunHours.trim() : "";
  }

  public String getFuelLevel() {
    return fuelLevel != null ? fuelLevel : "";
  }

  public void setFuelLevel(String fuelLevel) {
    this.fuelLevel = fuelLevel != null ? fuelLevel.trim() : "";
  }

  public String getIsSolar() {
    return isSolar != null ? isSolar : "";
  }

  public void setIsSolar(String isSolar) {
    this.isSolar = isSolar != null ? isSolar.trim() : "";
  }

  public String getMpptMake() {
    return mpptMake != null ? mpptMake : "";
  }

  public void setMpptMake(String mpptMake) {
    this.mpptMake = mpptMake != null ? mpptMake.trim() : "";
  }

  public String getMpptRating() {
    return mpptRating != null ? mpptRating : "";
  }

  public void setMpptRating(String mpptRating) {
    this.mpptRating = mpptRating != null ? mpptRating.trim() : "";
  }

  public String getMpptCount() {
    return mpptCount != null ? mpptCount : "";
  }

  public void setMpptCount(String mpptCount) {
    this.mpptCount = mpptCount != null ? mpptCount.trim() : "";
  }

  public String getMpptFaultyCount() {
    return mpptFaultyCount != null ? mpptFaultyCount : "";
  }

  public void setMpptFaultyCount(String mpptFaultyCount) {
    this.mpptFaultyCount = mpptFaultyCount != null ? mpptFaultyCount.trim() : "";
  }

  public String getVegetationStatus() {
    return vegetationStatus != null ? vegetationStatus : "";
  }

  public void setVegetationStatus(String vegetationStatus) {
    this.vegetationStatus = vegetationStatus != null ? vegetationStatus.trim() : "";
  }

  public String getNotes() {
    return notes != null ? notes : "";
  }

  public void setNotes(String notes) {
    this.notes = notes != null ? notes.trim() : "";
  }

  @NonNull
  public Map<String, String> getExtraAttributes() {
    return extraAttributes;
  }

  public void setExtraAttributes(@Nullable Map<String, String> attributes) {
    this.extraAttributes = attributes != null ? attributes : new HashMap<>();
  }

  public void addExtraAttribute(@NonNull String key, @Nullable String value) {
    if (value != null && !value.trim().isEmpty()) {
      extraAttributes.put(key.trim(), value.trim());
    }
  }

  public String getExtraAttributesAsJson() {
    if (extraAttributes.isEmpty()) {
      return "";
    }
    StringBuilder sb = new StringBuilder("{");
    boolean first = true;
    for (Map.Entry<String, String> entry : extraAttributes.entrySet()) {
      if (!first) sb.append(",");
      sb.append("\"").append(escapeJson(entry.getKey())).append("\":\"")
        .append(escapeJson(entry.getValue())).append("\"");
      first = false;
    }
    sb.append("}");
    return sb.toString();
  }

  private static String escapeJson(String s) {
    if (s == null) return "";
    return s.replace("\\", "\\\\")
            .replace("\"", "\\\"")
            .replace("\n", "\\n")
            .replace("\r", "\\r")
            .replace("\t", "\\t");
  }

  public void setExtraAttributesFromJson(@Nullable String json) {
    extraAttributes.clear();
    if (json == null || json.trim().isEmpty() || !json.startsWith("{")) {
      return;
    }
    try {
      JSONObject obj = new JSONObject(json);
      Iterator<String> keys = obj.keys();
      while (keys.hasNext()) {
        String key = keys.next();
        extraAttributes.put(key, obj.optString(key));
      }
    } catch (Throwable t) {
      parseSimpleJsonMap(json);
    }
  }

  private void parseSimpleJsonMap(String json) {
    String content = json.trim();
    if (content.startsWith("{") && content.endsWith("}")) {
      content = content.substring(1, content.length() - 1).trim();
      if (content.isEmpty()) return;
      String[] pairs = content.split(",(?=(?:[^\"]*\"[^\"]*\")*[^\"]*$)");
      for (String pair : pairs) {
        String[] kv = pair.split(":(?=(?:[^\"]*\"[^\"]*\")*[^\"]*$)");
        if (kv.length == 2) {
          String k = unescapeJson(kv[0].trim());
          String v = unescapeJson(kv[1].trim());
          if (!k.isEmpty()) {
            extraAttributes.put(k, v);
          }
        }
      }
    }
  }

  private static String unescapeJson(String s) {
    if (s.startsWith("\"") && s.endsWith("\"") && s.length() >= 2) {
      s = s.substring(1, s.length() - 1);
    }
    return s.replace("\\\"", "\"")
            .replace("\\\\", "\\")
            .replace("\\n", "\n")
            .replace("\\r", "\r")
            .replace("\\t", "\t");
  }


  public long getUpdatedAt() {
    return updatedAt;
  }

  public void setUpdatedAt(long updatedAt) {
    this.updatedAt = updatedAt;
  }

  /**
   * Returns a concise description of the site for display on markers and list items.
   */
  @NonNull
  public String getSummary() {
    StringBuilder sb = new StringBuilder();
    if (siteName != null && !siteName.isEmpty()) {
      sb.append(siteName);
    }
    if (location != null && !location.isEmpty()) {
      if (sb.length() > 0) sb.append(" | ");
      sb.append(location);
    }
    if (siteType != null && !siteType.isEmpty()) {
      if (sb.length() > 0) sb.append(" | ");
      sb.append(siteType);
    }
    return sb.toString();
  }

  @NonNull
  public String getFormattedCoordinates() {
    if (!hasValidCoords) {
      return "No valid coordinates";
    }
    return String.format(java.util.Locale.US, "%.6f, %.6f", latitude, longitude);
  }

  private double distanceMeters = -1.0;

  public double getDistanceMeters() {
    return distanceMeters;
  }

  public void setDistanceMeters(double distanceMeters) {
    this.distanceMeters = distanceMeters;
  }

  public boolean hasDistance() {
    return distanceMeters >= 0;
  }

  @NonNull
  public String getFormattedDistance() {
    if (distanceMeters < 0) {
      return "";
    }
    if (distanceMeters < 1000) {
      return Math.round(distanceMeters) + " m";
    }
    return String.format(java.util.Locale.US, "%.1f km", distanceMeters / 1000.0);
  }

  @Override
  public String toString() {
    return siteId + (siteName.isEmpty() ? "" : " - " + siteName);
  }
}
