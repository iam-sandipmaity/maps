package app.organicmaps.telecom.ui;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import app.organicmaps.R;
import app.organicmaps.telecom.db.TelecomDatabaseHelper;
import app.organicmaps.telecom.model.TelecomSite;
import app.organicmaps.telecom.navigation.ExternalNavigationHelper;
import app.organicmaps.telecom.planner.VisitPlannerManager;
import com.google.android.material.appbar.MaterialToolbar;
import java.util.Map;

public class TelecomSiteDetailsActivity extends AppCompatActivity {

  public static final String EXTRA_SITE_ID = "extra_site_id";

  private TelecomSite mSite;

  public static void start(@NonNull Context context, @NonNull String siteId) {
    Intent intent = new Intent(context, TelecomSiteDetailsActivity.class);
    intent.putExtra(EXTRA_SITE_ID, siteId);
    context.startActivity(intent);
  }

  @Override
  protected void onCreate(@Nullable Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_telecom_site_details);

    MaterialToolbar toolbar = findViewById(R.id.toolbar);
    setSupportActionBar(toolbar);
    if (getSupportActionBar() != null) {
      getSupportActionBar().setDisplayHomeAsUpEnabled(true);
    }
    toolbar.setNavigationOnClickListener(v -> finish());

    String siteId = getIntent().getStringExtra(EXTRA_SITE_ID);
    if (siteId == null || siteId.isEmpty()) {
      Toast.makeText(this, "Site ID not specified", Toast.LENGTH_SHORT).show();
      finish();
      return;
    }

    mSite = TelecomDatabaseHelper.getInstance(this).getSiteById(siteId);
    if (mSite == null) {
      Toast.makeText(this, "Site not found: " + siteId, Toast.LENGTH_SHORT).show();
      finish();
      return;
    }

    bindSiteData();
  }

  private void bindSiteData() {
    TextView tvSiteId = findViewById(R.id.tv_detail_site_id);
    TextView tvSiteName = findViewById(R.id.tv_detail_site_name);
    TextView tvTypeLoc = findViewById(R.id.tv_detail_type_loc);
    TextView tvAddress = findViewById(R.id.tv_detail_address);
    TextView tvCoords = findViewById(R.id.tv_detail_coords);

    tvSiteId.setText(mSite.getSiteId());
    tvSiteName.setText(mSite.getSiteName().isEmpty() ? "(No Site Name)" : mSite.getSiteName());

    String typeStr = mSite.getSiteType().isEmpty() ? "N/A" : mSite.getSiteType();
    String locStr = mSite.getLocation().isEmpty() ? "N/A" : mSite.getLocation();
    tvTypeLoc.setText("Type: " + typeStr + " | JC: " + locStr);

    if (!mSite.getAddress().isEmpty()) {
      tvAddress.setVisibility(View.VISIBLE);
      tvAddress.setText("Address: " + mSite.getAddress());
    } else {
      tvAddress.setVisibility(View.GONE);
    }

    tvCoords.setText(mSite.hasValidCoords() ? mSite.getFormattedCoordinates() : "No valid coordinates available");

    // Copy Buttons
    Button btnCopyCoords = findViewById(R.id.btn_copy_coords);
    Button btnCopySiteId = findViewById(R.id.btn_copy_site_id);
    Button btnCopyLat = findViewById(R.id.btn_copy_lat);
    Button btnCopyLon = findViewById(R.id.btn_copy_lon);
    Button btnCopyGmaps = findViewById(R.id.btn_copy_gmaps);

    btnCopyCoords.setOnClickListener(v -> ExternalNavigationHelper.copyCoordinates(this, mSite));
    btnCopySiteId.setOnClickListener(v -> ExternalNavigationHelper.copySiteId(this, mSite));
    btnCopyLat.setOnClickListener(v -> ExternalNavigationHelper.copyLatitude(this, mSite));
    btnCopyLon.setOnClickListener(v -> ExternalNavigationHelper.copyLongitude(this, mSite));
    btnCopyGmaps.setOnClickListener(v -> ExternalNavigationHelper.copyGoogleMapsUrl(this, mSite));

    // Navigation Buttons
    Button btnOpenGmaps = findViewById(R.id.btn_open_google_maps);
    Button btnOpenCoMaps = findViewById(R.id.btn_open_comaps);
    Button btnOpenMappls = findViewById(R.id.btn_open_mappls);

    btnOpenGmaps.setOnClickListener(v -> ExternalNavigationHelper.openInGoogleMaps(this, mSite, true));
    btnOpenCoMaps.setOnClickListener(v -> ExternalNavigationHelper.openInCoMaps(this, mSite));
    btnOpenMappls.setOnClickListener(v -> ExternalNavigationHelper.openInMappls(this, mSite));

    // Visit Plan Button
    Button btnToggleVisitPlan = findViewById(R.id.btn_toggle_visit_plan);
    updateVisitPlanButton(btnToggleVisitPlan);
    btnToggleVisitPlan.setOnClickListener(v -> {
      boolean inPlan = TelecomDatabaseHelper.getInstance(this).isInVisitPlan(mSite.getSiteId());
      if (inPlan) {
        VisitPlannerManager.getInstance(this).removeSite(this, mSite.getSiteId());
        Toast.makeText(this, "Removed from Visit Plan", Toast.LENGTH_SHORT).show();
      } else {
        VisitPlannerManager.getInstance(this).addSite(this, mSite);
        Toast.makeText(this, "Added to Visit Plan", Toast.LENGTH_SHORT).show();
      }
      updateVisitPlanButton(btnToggleVisitPlan);
    });

    // Personnel & Contacts
    TextView tvTechName = findViewById(R.id.tv_detail_tech_name);
    TextView tvTechPhone = findViewById(R.id.tv_detail_tech_phone);
    ImageView btnCallTech = findViewById(R.id.btn_call_tech);
    ImageView btnCopyTechPhone = findViewById(R.id.btn_copy_tech_phone);

    String techName = mSite.getTechnicianName();
    String techPhone = mSite.getTechnicianPhone();
    String techPprj = mSite.getTechnicianPprj();
    tvTechName.setText("Technician: " + (techName.isEmpty() ? "Not Assigned" : techName) + (techPprj.isEmpty() ? "" : " (" + techPprj + ")"));
    tvTechPhone.setText("Mobile: " + (techPhone.isEmpty() ? "N/A" : techPhone));

    btnCallTech.setOnClickListener(v -> ExternalNavigationHelper.dialPhoneNumber(this, techPhone));
    btnCopyTechPhone.setOnClickListener(v -> ExternalNavigationHelper.copyPhoneNumber(this, "Technician Phone", techPhone));

    TextView tvSpName = findViewById(R.id.tv_detail_sp_name);
    TextView tvSpPhone = findViewById(R.id.tv_detail_sp_phone);
    ImageView btnCallSp = findViewById(R.id.btn_call_sp);
    ImageView btnCopySpPhone = findViewById(R.id.btn_copy_sp_phone);

    String spName = mSite.getSupervisorName();
    String spPhone = mSite.getSupervisorPhone();
    tvSpName.setText("Supervisor: " + (spName.isEmpty() ? "Not Assigned" : spName));
    tvSpPhone.setText("Mobile: " + (spPhone.isEmpty() ? "N/A" : spPhone));

    btnCallSp.setOnClickListener(v -> ExternalNavigationHelper.dialPhoneNumber(this, spPhone));
    btnCopySpPhone.setOnClickListener(v -> ExternalNavigationHelper.copyPhoneNumber(this, "Supervisor Phone", spPhone));

    // Power & Electricity Board (EB)
    TextView tvEb = findViewById(R.id.tv_detail_eb);
    StringBuilder ebSb = new StringBuilder();
    appendField(ebSb, "Power Status", mSite.getPowerStatus());
    appendField(ebSb, "EB Availability", mSite.getEbAvailability());
    appendField(ebSb, "Site Load (Amp)", mSite.getSiteLoadAmp());
    appendField(ebSb, "Consumer ID", mSite.getConsumerId());
    appendField(ebSb, "Meter No", mSite.getMeterNo());
    appendField(ebSb, "Installation No", mSite.getInstallationNo());
    appendField(ebSb, "Division", mSite.getEbDivision());
    appendField(ebSb, "CCC Substation", mSite.getEbCcc());
    appendField(ebSb, "Docket Ticket No", mSite.getDocketNo());
    tvEb.setText(ebSb.length() > 0 ? ebSb.toString() : "No EB data recorded.");

    // Infrastructure Equipment (SMPS, BM, RM, DG, Solar)
    TextView tvEquip = findViewById(R.id.tv_detail_equipment);
    StringBuilder eqSb = new StringBuilder();

    // SMPS
    eqSb.append("【SMPS DC Power Plant】\n");
    appendField(eqSb, "Make", mSite.getSmpsMake());
    appendField(eqSb, "Rating", mSite.getSmpsRating());
    appendField(eqSb, "Fault Details", mSite.getSmpsFault());
    appendField(eqSb, "Remarks", mSite.getSmpsRemarks());

    // Battery Bank (BM)
    eqSb.append("\n【Battery Bank (BM)】\n");
    appendField(eqSb, "Make", mSite.getBmMake());
    appendField(eqSb, "Rating", mSite.getBmRating());
    appendField(eqSb, "Backup Hours", mSite.getBbBackup());
    appendField(eqSb, "Physical Count", mSite.getBmPhysicalCount());
    appendField(eqSb, "Faulty Count", mSite.getBmFaultyCount());
    appendField(eqSb, "Remarks", mSite.getBmRemarks());
    appendField(eqSb, "Battery Serial Numbers", mSite.getBatterySerials());

    // Rectifiers (RM)
    eqSb.append("\n【Rectifier Modules (RM)】\n");
    appendField(eqSb, "Make", mSite.getRmMake());
    appendField(eqSb, "Rating", mSite.getRmRating());
    appendField(eqSb, "Physical Count", mSite.getRmPhysicalCount());
    appendField(eqSb, "Bypass Count", mSite.getRmBypassCount());
    appendField(eqSb, "Faulty Count", mSite.getRmFaultyCount());
    appendField(eqSb, "Remarks", mSite.getRmRemarks());

    // DG & GCU
    eqSb.append("\n【Diesel Generator (DG) & GCU】\n");
    appendField(eqSb, "DG Make", mSite.getDgMake());
    appendField(eqSb, "DG Rating", mSite.getDgRating());
    appendField(eqSb, "GCU Make / Model", mSite.getGcuMake() + (mSite.getGcuModel().isEmpty() ? "" : " / " + mSite.getGcuModel()));
    appendField(eqSb, "GCU Status", mSite.getGcuStatus());
    appendField(eqSb, "Fuel Sensor Status", mSite.getFuelSensorStatus());
    appendField(eqSb, "Automation Status", mSite.getDgAutomationStatus());
    appendField(eqSb, "DG Run Hours", mSite.getDgRunHours());
    appendField(eqSb, "Fuel Level / Stock", mSite.getFuelLevel());

    // Solar & Environment
    eqSb.append("\n【Solar & Site Environment】\n");
    appendField(eqSb, "Solar Site", mSite.getIsSolar());
    appendField(eqSb, "MPPT Make / Rating", mSite.getMpptMake() + (mSite.getMpptRating().isEmpty() ? "" : " / " + mSite.getMpptRating()));
    appendField(eqSb, "MPPT Count / Faulty", mSite.getMpptCount() + (mSite.getMpptFaultyCount().isEmpty() ? "" : " / " + mSite.getMpptFaultyCount()));
    appendField(eqSb, "Vegetation Status", mSite.getVegetationStatus());

    tvEquip.setText(eqSb.toString());

    // Remarks & Notes
    TextView tvNotes = findViewById(R.id.tv_detail_notes);
    StringBuilder notesSb = new StringBuilder();
    if (!mSite.getNotes().isEmpty()) {
      notesSb.append(mSite.getNotes()).append("\n\n");
    }
    // Append any extra unmapped attributes from Excel
    Map<String, String> extras = mSite.getExtraAttributes();
    if (!extras.isEmpty()) {
      notesSb.append("【Additional Spreadsheet Attributes】\n");
      for (Map.Entry<String, String> entry : extras.entrySet()) {
        notesSb.append("• ").append(entry.getKey()).append(": ").append(entry.getValue()).append("\n");
      }
    }
    tvNotes.setText(notesSb.length() > 0 ? notesSb.toString() : "No remarks recorded.");
  }

  private void updateVisitPlanButton(Button btn) {
    boolean inPlan = TelecomDatabaseHelper.getInstance(this).isInVisitPlan(mSite.getSiteId());
    btn.setText(inPlan ? "Remove from Visit Plan" : "Add to Visit Plan");
  }

  private void appendField(StringBuilder sb, String label, String value) {
    if (value != null && !value.trim().isEmpty()) {
      sb.append("• ").append(label).append(": ").append(value.trim()).append("\n");
    }
  }
}
