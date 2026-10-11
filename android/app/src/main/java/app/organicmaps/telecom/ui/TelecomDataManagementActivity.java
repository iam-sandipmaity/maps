package app.organicmaps.telecom.ui;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;
import app.organicmaps.R;
import app.organicmaps.telecom.db.TelecomDatabaseHelper;
import app.organicmaps.telecom.importer.ColumnMapper;
import app.organicmaps.telecom.importer.CsvParser;
import app.organicmaps.telecom.importer.ImportSummary;
import app.organicmaps.telecom.importer.TelecomCsvExporter;
import app.organicmaps.telecom.importer.XlsxParser;
import app.organicmaps.telecom.map.TelecomMapLayerManager;
import app.organicmaps.telecom.model.TelecomSite;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.switchmaterial.SwitchMaterial;
import java.io.File;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

public class TelecomDataManagementActivity extends AppCompatActivity {

  private TextView mTvStatsTotal;
  private TextView mTvStatsCoords;
  private SwitchMaterial mSwitchMapLayer;
  private RadioGroup mRgImportMode;
  private MaterialCardView mCardImportSummary;
  private TextView mTvImportSummaryText;

  private ActivityResultLauncher<String[]> mFilePickerLauncher;

  public static void start(@NonNull Context context) {
    Intent intent = new Intent(context, TelecomDataManagementActivity.class);
    context.startActivity(intent);
  }

  @Override
  protected void onCreate(@Nullable Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_telecom_data_management);

    MaterialToolbar toolbar = findViewById(R.id.toolbar);
    setSupportActionBar(toolbar);
    if (getSupportActionBar() != null) {
      getSupportActionBar().setDisplayHomeAsUpEnabled(true);
    }
    toolbar.setNavigationOnClickListener(v -> finish());

    mTvStatsTotal = findViewById(R.id.tv_stats_total);
    mTvStatsCoords = findViewById(R.id.tv_stats_coords);
    mSwitchMapLayer = findViewById(R.id.switch_map_layer);
    mRgImportMode = findViewById(R.id.rg_import_mode);
    mCardImportSummary = findViewById(R.id.card_import_summary);
    mTvImportSummaryText = findViewById(R.id.tv_import_summary_text);

    Button btnSyncMap = findViewById(R.id.btn_sync_map);
    Button btnImportCsv = findViewById(R.id.btn_import_csv);
    Button btnLoadDemoData = findViewById(R.id.btn_load_demo_data);
    Button btnExportCsv = findViewById(R.id.btn_export_csv);

    mSwitchMapLayer.setChecked(TelecomMapLayerManager.getInstance().isLayerVisible(this));
    mSwitchMapLayer.setOnCheckedChangeListener((buttonView, isChecked) -> {
      TelecomMapLayerManager.getInstance().setLayerVisible(this, isChecked);
      Toast.makeText(this, isChecked ? "Telecom map layer enabled" : "Telecom map layer hidden", Toast.LENGTH_SHORT).show();
    });

    btnSyncMap.setOnClickListener(v -> syncMapMarkers());

    mFilePickerLauncher = registerForActivityResult(
        new ActivityResultContracts.OpenDocument(),
        uri -> {
          if (uri != null) {
            handleSelectedFile(uri);
          }
        }
    );

    btnImportCsv.setOnClickListener(v -> {
      int checkedId = mRgImportMode.getCheckedRadioButtonId();
      if (checkedId == R.id.rb_mode_replace) {
        new AlertDialog.Builder(this)
            .setTitle("Confirm Dataset Replacement")
            .setMessage("Are you sure you want to replace your entire telecom dataset? All current site records will be overwritten.")
            .setPositiveButton("Proceed", (dialog, which) -> launchFilePicker())
            .setNegativeButton("Cancel", null)
            .show();
      } else {
        launchFilePicker();
      }
    });

    btnLoadDemoData.setOnClickListener(v -> {
      int checkedId = mRgImportMode.getCheckedRadioButtonId();
      if (checkedId == R.id.rb_mode_replace) {
        new AlertDialog.Builder(this)
            .setTitle("Confirm Dataset Replacement")
            .setMessage("Are you sure you want to replace your entire telecom dataset with the Infra Tracker demo records?")
            .setPositiveButton("Proceed", (dialog, which) -> loadDemoDataset())
            .setNegativeButton("Cancel", null)
            .show();
      } else {
        loadDemoDataset();
      }
    });

    btnExportCsv.setOnClickListener(v -> exportDatasetToCsv());

    updateStatistics();
  }

  private void launchFilePicker() {
    mFilePickerLauncher.launch(new String[]{
        "text/comma-separated-values",
        "text/csv",
        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
        "application/vnd.ms-excel",
        "*/*"
    });
  }

  private void updateStatistics() {
    new Thread(() -> {
      TelecomDatabaseHelper db = TelecomDatabaseHelper.getInstance(this);
      int total = db.getSitesCount();
      int coords = db.getValidCoordsSitesCount();
      runOnUiThread(() -> {
        mTvStatsTotal.setText("Total Site Records: " + total);
        mTvStatsCoords.setText("Sites with Valid Coordinates: " + coords);
      });
    }).start();
  }

  private void loadDemoDataset() {
    ProgressDialog progress = new ProgressDialog(this);
    progress.setMessage("Loading demo Infra Tracker records from assets...");
    progress.setCancelable(false);
    progress.show();

    int checkedId = mRgImportMode.getCheckedRadioButtonId();
    TelecomDatabaseHelper.ImportMode mode;
    if (checkedId == R.id.rb_mode_add) {
      mode = TelecomDatabaseHelper.ImportMode.ADD_NEW;
    } else if (checkedId == R.id.rb_mode_replace) {
      mode = TelecomDatabaseHelper.ImportMode.REPLACE_ALL;
    } else {
      mode = TelecomDatabaseHelper.ImportMode.UPDATE_EXISTING;
    }

    new Thread(() -> {
      ImportSummary summary = new ImportSummary();
      try (InputStream is = getAssets().open("telecom/infra_tracker_demo.csv")) {
        List<List<String>> rows = CsvParser.parse(is);
        if (rows.isEmpty()) {
          throw new Exception("Demo dataset file is empty");
        }

        List<String> headers = rows.get(0);
        ColumnMapper mapper = ColumnMapper.autoDetectMapping(headers);

        List<TelecomSite> parsedSites = new ArrayList<>();
        for (int i = 1; i < rows.size(); i++) {
          summary.incrementTotal();
          TelecomSite site = mapper.mapRow(rows.get(i), i + 1, summary);
          if (site != null) {
            parsedSites.add(site);
          }
        }

        TelecomDatabaseHelper db = TelecomDatabaseHelper.getInstance(this);
        int[] counts = db.insertBatch(parsedSites, mode);
        for (int i = 0; i < counts[0]; i++) summary.incrementImported();
        for (int i = 0; i < counts[1]; i++) summary.incrementUpdated();
        for (int i = 0; i < counts[2]; i++) summary.incrementSkipped();

        // Sync with map layer
        TelecomMapLayerManager.getInstance().syncSitesToMap(this);

        runOnUiThread(() -> {
          progress.dismiss();
          showImportSummaryDialog(summary);
          updateStatistics();
        });

      } catch (Exception e) {
        runOnUiThread(() -> {
          progress.dismiss();
          new AlertDialog.Builder(this)
              .setTitle("Demo Load Failed")
              .setMessage("Error loading demo dataset: " + e.getMessage())
              .setPositiveButton("OK", null)
              .show();
        });
      }
    }).start();
  }

  private void handleSelectedFile(Uri uri) {
    ProgressDialog progress = new ProgressDialog(this);
    progress.setMessage("Importing telecom records...");
    progress.setCancelable(false);
    progress.show();

    int checkedId = mRgImportMode.getCheckedRadioButtonId();
    TelecomDatabaseHelper.ImportMode mode;
    if (checkedId == R.id.rb_mode_add) {
      mode = TelecomDatabaseHelper.ImportMode.ADD_NEW;
    } else if (checkedId == R.id.rb_mode_replace) {
      mode = TelecomDatabaseHelper.ImportMode.REPLACE_ALL;
    } else {
      mode = TelecomDatabaseHelper.ImportMode.UPDATE_EXISTING;
    }

    new Thread(() -> {
      ImportSummary summary = new ImportSummary();
      try (InputStream is = getContentResolver().openInputStream(uri)) {
        if (is == null) {
          throw new Exception("Unable to open selected file");
        }

        String uriStr = uri.toString().toLowerCase();
        boolean isXlsx = uriStr.endsWith(".xlsx") || "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet".equals(getContentResolver().getType(uri));

        List<List<String>> rows;
        if (isXlsx) {
          rows = XlsxParser.parse(is, getCacheDir());
        } else {
          rows = CsvParser.parse(is);
        }

        if (rows.isEmpty()) {
          throw new Exception("File is empty or contains no valid rows");
        }

        List<String> headers = rows.get(0);
        ColumnMapper mapper = ColumnMapper.autoDetectMapping(headers);

        List<TelecomSite> parsedSites = new ArrayList<>();
        for (int i = 1; i < rows.size(); i++) {
          summary.incrementTotal();
          TelecomSite site = mapper.mapRow(rows.get(i), i + 1, summary);
          if (site != null) {
            parsedSites.add(site);
          }
        }

        TelecomDatabaseHelper db = TelecomDatabaseHelper.getInstance(this);
        int[] counts = db.insertBatch(parsedSites, mode);
        for (int i = 0; i < counts[0]; i++) summary.incrementImported();
        for (int i = 0; i < counts[1]; i++) summary.incrementUpdated();
        for (int i = 0; i < counts[2]; i++) summary.incrementSkipped();

        // Sync with map layer
        TelecomMapLayerManager.getInstance().syncSitesToMap(this);

        runOnUiThread(() -> {
          progress.dismiss();
          showImportSummaryDialog(summary);
          updateStatistics();
        });

      } catch (Exception e) {
        runOnUiThread(() -> {
          progress.dismiss();
          new AlertDialog.Builder(this)
              .setTitle("Import Failed")
              .setMessage("Error during file import: " + e.getMessage())
              .setPositiveButton("OK", null)
              .show();
        });
      }
    }).start();
  }

  private void showImportSummaryDialog(ImportSummary summary) {
    mCardImportSummary.setVisibility(View.VISIBLE);
    mTvImportSummaryText.setText(summary.getFormattedSummary());

    new AlertDialog.Builder(this)
        .setTitle("Import Completed")
        .setMessage(summary.getFormattedSummary())
        .setPositiveButton("OK", null)
        .show();
  }

  private void syncMapMarkers() {
    ProgressDialog progress = new ProgressDialog(this);
    progress.setMessage("Syncing sites to map placemarks...");
    progress.setCancelable(false);
    progress.show();

    new Thread(() -> {
      boolean success = TelecomMapLayerManager.getInstance().syncSitesToMap(this);
      runOnUiThread(() -> {
        progress.dismiss();
        if (success) {
          Toast.makeText(this, "Telecom sites synced to map placemarks successfully", Toast.LENGTH_SHORT).show();
        } else {
          Toast.makeText(this, "No valid sites with coordinates found to sync", Toast.LENGTH_SHORT).show();
        }
      });
    }).start();
  }

  private void exportDatasetToCsv() {
    ProgressDialog progress = new ProgressDialog(this);
    progress.setMessage("Exporting dataset to CSV...");
    progress.setCancelable(false);
    progress.show();

    new Thread(() -> {
      try {
        TelecomDatabaseHelper db = TelecomDatabaseHelper.getInstance(this);
        List<TelecomSite> allSites = db.getAllSites();
        if (allSites.isEmpty()) {
          throw new Exception("Dataset is empty. Nothing to export.");
        }

        File exportDir = new File(getExternalFilesDir(null), "exports");
        if (!exportDir.exists()) exportDir.mkdirs();
        File exportFile = new File(exportDir, "telecom_sites_export_" + System.currentTimeMillis() + ".csv");
        TelecomCsvExporter.exportToFile(allSites, exportFile);

        runOnUiThread(() -> {
          progress.dismiss();
          shareExportedFile(exportFile);
        });
      } catch (Exception e) {
        runOnUiThread(() -> {
          progress.dismiss();
          Toast.makeText(this, "Export failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
        });
      }
    }).start();
  }

  private void shareExportedFile(File file) {
    try {
      Uri uri = FileProvider.getUriForFile(this, getPackageName() + ".provider", file);
      Intent shareIntent = new Intent(Intent.ACTION_SEND);
      shareIntent.setType("text/csv");
      shareIntent.putExtra(Intent.EXTRA_STREAM, uri);
      shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
      startActivity(Intent.createChooser(shareIntent, "Share Telecom CSV Backup"));
    } catch (Exception e) {
      Toast.makeText(this, "File exported to: " + file.getAbsolutePath(), Toast.LENGTH_LONG).show();
    }
  }
}
