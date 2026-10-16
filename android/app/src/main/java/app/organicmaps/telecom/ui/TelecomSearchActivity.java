package app.organicmaps.telecom.ui;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import app.organicmaps.MwmApplication;
import app.organicmaps.R;
import app.organicmaps.sdk.bookmarks.data.MapObject;
import app.organicmaps.telecom.db.TelecomDatabaseHelper;
import app.organicmaps.telecom.map.TelecomMapLayerManager;
import app.organicmaps.telecom.model.TelecomSite;
import app.organicmaps.telecom.navigation.ExternalNavigationHelper;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.textfield.TextInputEditText;
import java.util.ArrayList;
import java.util.List;

public class TelecomSearchActivity extends AppCompatActivity {

  public static final String EXTRA_INITIAL_QUERY = "extra_initial_query";

  private TextInputEditText mEtSearch;
  private ChipGroup mCgRadius;
  private RecyclerView mRvResults;
  private TextView mTvResultCount;
  private MaterialButton mBtnShowOnMap;
  private TextView mTvEmpty;
  private SearchAdapter mAdapter;

  private final Handler mHandler = new Handler(Looper.getMainLooper());
  private Runnable mSearchRunnable;

  private static final int PAGE_SIZE = 100;
  private int mCurrentOffset = 0;
  private int mTotalCount = 0;
  private String mCurrentQuery = "";
  private boolean mIsLoading = false;
  private boolean mHasMore = true;

  // Selected radius in meters: -1 means All Sites (no radius restriction)
  private double mSelectedRadiusMeters = -1.0;
  private String mCustomRadiusLabel = "Custom...";

  private ImageButton mBtnToggleFilters;
  private LinearLayout mLayoutAdvancedFilters;
  private Chip mChipFilterJc;
  private Chip mChipFilterTech;
  private MaterialButton mBtnResetFilters;

  private String mSelectedJc = null;
  private String mSelectedTech = null;

  public static void start(@NonNull Context context, @Nullable String initialQuery) {
    Intent intent = new Intent(context, TelecomSearchActivity.class);
    if (initialQuery != null) {
      intent.putExtra(EXTRA_INITIAL_QUERY, initialQuery);
    }
    context.startActivity(intent);
  }

  @Override
  protected void onCreate(@Nullable Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_telecom_search);

    MaterialToolbar toolbar = findViewById(R.id.toolbar);
    setSupportActionBar(toolbar);
    if (getSupportActionBar() != null) {
      getSupportActionBar().setDisplayHomeAsUpEnabled(true);
    }
    toolbar.setNavigationOnClickListener(v -> finish());

    androidx.core.view.ViewCompat.setOnApplyWindowInsetsListener(toolbar, (v, insets) -> {
      androidx.core.graphics.Insets statusBarInsets = insets.getInsets(androidx.core.view.WindowInsetsCompat.Type.statusBars());
      v.setPadding(v.getPaddingLeft(), statusBarInsets.top, v.getPaddingRight(), v.getPaddingBottom());
      return insets;
    });

    View root = findViewById(android.R.id.content);
    if (root != null) {
      androidx.core.view.ViewCompat.setOnApplyWindowInsetsListener(root, (v, insets) -> {
        androidx.core.graphics.Insets navBarInsets = insets.getInsets(androidx.core.view.WindowInsetsCompat.Type.navigationBars());
        v.setPadding(v.getPaddingLeft(), v.getPaddingTop(), v.getPaddingRight(), navBarInsets.bottom);
        return insets;
      });
    }

    mEtSearch = findViewById(R.id.et_search);
    mCgRadius = findViewById(R.id.cg_radius);
    mRvResults = findViewById(R.id.rv_results);
    mTvResultCount = findViewById(R.id.tv_result_count);
    mBtnShowOnMap = findViewById(R.id.btn_show_on_map);
    mTvEmpty = findViewById(R.id.tv_empty);

    mBtnToggleFilters = findViewById(R.id.btn_toggle_filters);
    mLayoutAdvancedFilters = findViewById(R.id.layout_advanced_filters);
    mChipFilterJc = findViewById(R.id.chip_filter_jc);
    mChipFilterTech = findViewById(R.id.chip_filter_tech);
    mBtnResetFilters = findViewById(R.id.btn_reset_filters);

    setupAdvancedFilters();

    mAdapter = new SearchAdapter();
    LinearLayoutManager layoutManager = new LinearLayoutManager(this);
    mRvResults.setLayoutManager(layoutManager);
    mRvResults.setAdapter(mAdapter);

    mRvResults.addOnScrollListener(new RecyclerView.OnScrollListener() {
      @Override
      public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
        if (dy <= 0) return;
        if (mHasMore && !mIsLoading && mSelectedRadiusMeters <= 0.0 && mSelectedJc == null && mSelectedTech == null) {
          int visibleItemCount = layoutManager.getChildCount();
          int totalItemCount = layoutManager.getItemCount();
          int firstVisibleItemPosition = layoutManager.findFirstVisibleItemPosition();
          if ((visibleItemCount + firstVisibleItemPosition) >= totalItemCount - 15) {
            loadNextPage();
          }
        }
      }
    });

    mEtSearch.addTextChangedListener(new TextWatcher() {
      @Override
      public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

      @Override
      public void onTextChanged(CharSequence s, int start, int before, int count) {
        if (mSearchRunnable != null) {
          mHandler.removeCallbacks(mSearchRunnable);
        }
        mSearchRunnable = () -> resetAndSearch(s != null ? s.toString() : "");
        mHandler.postDelayed(mSearchRunnable, 200); // 200ms debounce
      }

      @Override
      public void afterTextChanged(Editable s) {}
    });

    setupRadiusFilterChips();

    mBtnShowOnMap.setOnClickListener(v -> showCurrentSitesOnMap());

    String initialQuery = getIntent().getStringExtra(EXTRA_INITIAL_QUERY);
    if (initialQuery != null && !initialQuery.isEmpty()) {
      mEtSearch.setText(initialQuery);
    } else {
      resetAndSearch("");
    }
  }

  private void setupAdvancedFilters() {
    mBtnToggleFilters.setOnClickListener(v -> {
      if (mLayoutAdvancedFilters.getVisibility() == View.VISIBLE) {
        mLayoutAdvancedFilters.setVisibility(View.GONE);
      } else {
        mLayoutAdvancedFilters.setVisibility(View.VISIBLE);
      }
    });

    mChipFilterJc.setOnClickListener(v -> showJcFilterDialog());
    mChipFilterTech.setOnClickListener(v -> showTechFilterDialog());

    mBtnResetFilters.setOnClickListener(v -> {
      mSelectedJc = null;
      mSelectedTech = null;
      updateFilterChipTexts();
      resetAndSearch(mCurrentQuery);
    });
  }

  private void updateFilterChipTexts() {
    if (mSelectedJc != null) {
      mChipFilterJc.setText("JC: " + mSelectedJc);
      mChipFilterJc.setChipBackgroundColorResource(R.color.base_accent_transparent);
    } else {
      mChipFilterJc.setText("JC: All");
      mChipFilterJc.setChipBackgroundColor(null);
    }

    if (mSelectedTech != null) {
      mChipFilterTech.setText("Tech: " + mSelectedTech);
      mChipFilterTech.setChipBackgroundColorResource(R.color.base_accent_transparent);
    } else {
      mChipFilterTech.setText("Tech: All");
      mChipFilterTech.setChipBackgroundColor(null);
    }

    boolean hasActiveFilters = mSelectedJc != null || mSelectedTech != null;
    mBtnResetFilters.setVisibility(hasActiveFilters ? View.VISIBLE : View.GONE);
  }

  private void showJcFilterDialog() {
    new Thread(() -> {
      TelecomDatabaseHelper db = TelecomDatabaseHelper.getInstance(this);
      List<String> jcList = db.getAllDistinctJcNames();
      runOnUiThread(() -> {
        if (jcList.isEmpty()) {
          Toast.makeText(this, "No JC names found in database", Toast.LENGTH_SHORT).show();
          return;
        }

        List<String> options = new ArrayList<>();
        options.add("All JCs (Clear)");
        options.addAll(jcList);

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Filter by JC");
        builder.setItems(options.toArray(new String[0]), (dialog, which) -> {
          if (which == 0) {
            mSelectedJc = null;
          } else {
            mSelectedJc = options.get(which);
          }
          // Reset technician if it no longer belongs to the newly selected JC
          if (mSelectedTech != null && mSelectedJc != null) {
            new Thread(() -> {
              List<String> jcTechs = db.getAllDistinctTechnicians(mSelectedJc);
              if (!jcTechs.contains(mSelectedTech)) {
                mSelectedTech = null;
              }
              runOnUiThread(() -> {
                updateFilterChipTexts();
                resetAndSearch(mCurrentQuery);
              });
            }).start();
            return;
          }
          updateFilterChipTexts();
          resetAndSearch(mCurrentQuery);
        });
        builder.show();
      });
    }).start();
  }

  private void showTechFilterDialog() {
    new Thread(() -> {
      TelecomDatabaseHelper db = TelecomDatabaseHelper.getInstance(this);
      List<String> techList = db.getAllDistinctTechnicians(mSelectedJc);
      runOnUiThread(() -> {
        if (techList.isEmpty()) {
          String msg = mSelectedJc != null
              ? "No technicians found for JC: " + mSelectedJc
              : "No technicians found in database";
          Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
          return;
        }

        List<String> options = new ArrayList<>();
        options.add("All Technicians (Clear)");
        options.addAll(techList);

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(mSelectedJc != null ? "Technicians in " + mSelectedJc : "Filter by Technician");
        builder.setItems(options.toArray(new String[0]), (dialog, which) -> {
          if (which == 0) {
            mSelectedTech = null;
          } else {
            mSelectedTech = options.get(which);
          }
          updateFilterChipTexts();
          resetAndSearch(mCurrentQuery);
        });
        builder.show();
      });
    }).start();
  }

  private void setupRadiusFilterChips() {
    mCgRadius.setOnCheckedStateChangeListener((group, checkedIds) -> {
      if (checkedIds.isEmpty()) return;
      int checkedId = checkedIds.get(0);

      if (checkedId == R.id.chip_all) {
        mSelectedRadiusMeters = -1.0;
        resetAndSearch(mCurrentQuery);
      } else if (checkedId == R.id.chip_100m) {
        mSelectedRadiusMeters = 100.0;
        resetAndSearch(mCurrentQuery);
      } else if (checkedId == R.id.chip_500m) {
        mSelectedRadiusMeters = 500.0;
        resetAndSearch(mCurrentQuery);
      } else if (checkedId == R.id.chip_1km) {
        mSelectedRadiusMeters = 1000.0;
        resetAndSearch(mCurrentQuery);
      } else if (checkedId == R.id.chip_5km) {
        mSelectedRadiusMeters = 5000.0;
        resetAndSearch(mCurrentQuery);
      } else if (checkedId == R.id.chip_10km) {
        mSelectedRadiusMeters = 10000.0;
        resetAndSearch(mCurrentQuery);
      } else if (checkedId == R.id.chip_custom) {
        promptCustomRadius();
      }
    });
  }

  private void promptCustomRadius() {
    AlertDialog.Builder builder = new AlertDialog.Builder(this);
    builder.setTitle("Set Custom Radius");
    builder.setMessage("Enter radius in meters (e.g. 250) or kilometers (e.g. 15 km):");

    final EditText input = new EditText(this);
    input.setInputType(InputType.TYPE_CLASS_TEXT);
    input.setHint("e.g. 250m or 15km");
    builder.setView(input);

    builder.setPositiveButton("Apply", (dialog, which) -> {
      String text = input.getText().toString().trim().toLowerCase();
      double meters = parseRadiusStringToMeters(text);
      if (meters > 0) {
        mSelectedRadiusMeters = meters;
        Chip customChip = findViewById(R.id.chip_custom);
        if (customChip != null) {
          mCustomRadiusLabel = meters < 1000 ? "< " + Math.round(meters) + " m" : "< " + String.format(java.util.Locale.US, "%.1f km", meters / 1000.0);
          customChip.setText(mCustomRadiusLabel);
        }
        resetAndSearch(mCurrentQuery);
      } else {
        Toast.makeText(this, "Invalid radius value entered", Toast.LENGTH_SHORT).show();
        mCgRadius.check(R.id.chip_all);
      }
    });

    builder.setNegativeButton("Cancel", (dialog, which) -> {
      if (mSelectedRadiusMeters <= 0) {
        mCgRadius.check(R.id.chip_all);
      }
    });

    builder.show();
  }

  private double parseRadiusStringToMeters(String text) {
    if (text == null || text.isEmpty()) return -1.0;
    try {
      if (text.endsWith("km")) {
        double val = Double.parseDouble(text.replace("km", "").trim());
        return val * 1000.0;
      } else if (text.endsWith("m")) {
        return Double.parseDouble(text.replace("m", "").trim());
      } else {
        // default to meters if < 100, or km if float like 2.5
        double val = Double.parseDouble(text.trim());
        if (val < 50.0 && text.contains(".")) {
          return val * 1000.0;
        }
        return val;
      }
    } catch (Exception e) {
      return -1.0;
    }
  }

  @Nullable
  private double[] getCurrentLocation() {
    try {
      MapObject myPos = MwmApplication.from(this).getLocationHelper().getMyPosition();
      if (myPos != null && (myPos.getLat() != 0.0 || myPos.getLon() != 0.0)) {
        return new double[]{myPos.getLat(), myPos.getLon()};
      }
      Location saved = MwmApplication.from(this).getLocationHelper().getSavedLocation();
      if (saved != null && (saved.getLatitude() != 0.0 || saved.getLongitude() != 0.0)) {
        return new double[]{saved.getLatitude(), saved.getLongitude()};
      }
    } catch (Exception ignored) {
    }
    return null;
  }

  private void resetAndSearch(@NonNull String query) {
    mCurrentQuery = query;
    mCurrentOffset = 0;
    mHasMore = true;
    mIsLoading = true;

    new Thread(() -> {
      TelecomDatabaseHelper db = TelecomDatabaseHelper.getInstance(this);

      double[] loc = getCurrentLocation();

      if (mSelectedRadiusMeters > 0.0) {
        // Radius-based search around GPS location
        if (loc == null) {
          runOnUiThread(() -> {
            mIsLoading = false;
            mAdapter.setSites(new ArrayList<>());
            mTvEmpty.setVisibility(View.VISIBLE);
            mTvEmpty.setText("GPS location unavailable.\nPlease enable location services or turn on GPS to filter sites by radius.");
            mTvResultCount.setVisibility(View.GONE);
            mBtnShowOnMap.setVisibility(View.GONE);
          });
          return;
        }

        List<TelecomSite> withinRadius = db.getSitesWithinRadius(loc[0], loc[1], mSelectedRadiusMeters);

        // If text query or JC / Tech filters are present, filter within radius
        List<TelecomSite> filtered = new ArrayList<>();
        List<String> tokens = !query.trim().isEmpty() ? TelecomDatabaseHelper.extractSearchTokens(query) : new ArrayList<>();
        for (TelecomSite s : withinRadius) {
          if (!tokens.isEmpty() && !matchesTokens(s, tokens)) {
            continue;
          }
          if (mSelectedJc != null && !mSelectedJc.equalsIgnoreCase(s.getLocation())) {
            continue;
          }
          if (mSelectedTech != null && !mSelectedTech.equalsIgnoreCase(s.getTechnicianName())) {
            continue;
          }
          filtered.add(s);
        }

        final List<TelecomSite> finalResults = filtered;
        final String radiusDesc = mSelectedRadiusMeters < 1000
            ? Math.round(mSelectedRadiusMeters) + " m"
            : String.format(java.util.Locale.US, "%.1f km", mSelectedRadiusMeters / 1000.0);

        runOnUiThread(() -> {
          mIsLoading = false;
          mCurrentOffset = finalResults.size();
          mHasMore = false;
          mAdapter.setSites(finalResults);

          if (finalResults.isEmpty()) {
            mTvEmpty.setVisibility(View.VISIBLE);
            mTvEmpty.setText("No telecom sites found matching current filters within " + radiusDesc + ".");
            mTvResultCount.setVisibility(View.GONE);
            mBtnShowOnMap.setVisibility(View.GONE);
          } else {
            mTvEmpty.setVisibility(View.GONE);
            mTvResultCount.setVisibility(View.VISIBLE);
            mTvResultCount.setText("Found " + finalResults.size() + " sites within " + radiusDesc);
            mBtnShowOnMap.setVisibility(View.VISIBLE);
            mBtnShowOnMap.setText("Show on Map (" + finalResults.size() + ")");
          }
        });

      } else {
        // Standard search across all sites
        List<TelecomSite> results;
        int count;

        boolean hasJcOrTechFilter = mSelectedJc != null || mSelectedTech != null;
        if (hasJcOrTechFilter) {
          // When JC or Tech filter is active, fetch candidates and filter
          List<TelecomSite> allCandidates = query.trim().isEmpty()
              ? db.getAllSites(-1, 0)
              : db.searchSites(query, -1, 0);

          List<TelecomSite> filtered = new ArrayList<>();
          for (TelecomSite s : allCandidates) {
            if (mSelectedJc != null && !mSelectedJc.equalsIgnoreCase(s.getLocation())) {
              continue;
            }
            if (mSelectedTech != null && !mSelectedTech.equalsIgnoreCase(s.getTechnicianName())) {
              continue;
            }
            filtered.add(s);
          }
          results = filtered;
          count = filtered.size();
        } else {
          count = db.countSites(query);
          results = db.searchSites(query, PAGE_SIZE, 0);
        }

        mTotalCount = count;

        // Populate distance if location is available
        if (loc != null) {
          for (TelecomSite site : results) {
            if (site.hasValidCoords()) {
              double d = TelecomDatabaseHelper.calculateDistanceMeters(loc[0], loc[1], site.getLatitude(), site.getLongitude());
              site.setDistanceMeters(d);
            }
          }
        }

        final List<TelecomSite> displayResults = results;
        runOnUiThread(() -> {
          mIsLoading = false;
          mCurrentOffset = displayResults.size();
          mHasMore = !hasJcOrTechFilter && displayResults.size() >= PAGE_SIZE && mCurrentOffset < mTotalCount;
          mAdapter.setSites(displayResults);

          if (displayResults.isEmpty()) {
            mTvEmpty.setVisibility(View.VISIBLE);
            if (db.getSitesCount() == 0) {
              mTvEmpty.setText("No telecom sites found.\nPlease import your site tracker file from Telecom Data Management.");
            } else {
              mTvEmpty.setText("No matching telecom sites found.\nTry adjusting your search or active filters.");
            }
            mTvResultCount.setVisibility(View.GONE);
            mBtnShowOnMap.setVisibility(View.GONE);
          } else {
            mTvEmpty.setVisibility(View.GONE);
            mTvResultCount.setVisibility(View.VISIBLE);
            if (!hasJcOrTechFilter && mTotalCount > displayResults.size()) {
              mTvResultCount.setText("Showing " + displayResults.size() + " of " + mTotalCount + " sites (scroll for more)");
            } else {
              mTvResultCount.setText("Found " + displayResults.size() + " matching sites");
            }
            mBtnShowOnMap.setVisibility(View.VISIBLE);
            mBtnShowOnMap.setText("Show on Map");
          }
        });
      }
    }).start();
  }

  private boolean matchesTokens(TelecomSite site, List<String> tokens) {
    if (tokens.isEmpty()) return true;
    String id = site.getSiteId().toLowerCase();
    String name = site.getSiteName().toLowerCase();
    String loc = site.getLocation().toLowerCase();
    String tech = site.getTechnicianName().toLowerCase();

    for (String token : tokens) {
      String t = token.toLowerCase();
      if (!id.contains(t) && !name.contains(t) && !loc.contains(t) && !tech.contains(t)) {
        return false;
      }
    }
    return true;
  }

  private void loadNextPage() {
    if (mIsLoading || !mHasMore || mSelectedRadiusMeters > 0.0) return;
    mIsLoading = true;
    final int offset = mCurrentOffset;

    new Thread(() -> {
      TelecomDatabaseHelper db = TelecomDatabaseHelper.getInstance(this);
      List<TelecomSite> nextBatch = db.searchSites(mCurrentQuery, PAGE_SIZE, offset);

      double[] loc = getCurrentLocation();
      if (loc != null) {
        for (TelecomSite site : nextBatch) {
          if (site.hasValidCoords()) {
            double d = TelecomDatabaseHelper.calculateDistanceMeters(loc[0], loc[1], site.getLatitude(), site.getLongitude());
            site.setDistanceMeters(d);
          }
        }
      }

      runOnUiThread(() -> {
        mIsLoading = false;
        if (nextBatch.isEmpty()) {
          mHasMore = false;
          return;
        }
        mCurrentOffset += nextBatch.size();
        mAdapter.appendSites(nextBatch);
        mHasMore = nextBatch.size() >= PAGE_SIZE && mCurrentOffset < mTotalCount;
        mTvResultCount.setText("Showing " + mAdapter.getItemCount() + " of " + mTotalCount + " sites");
      });
    }).start();
  }

  private void showCurrentSitesOnMap() {
    List<TelecomSite> sites = mAdapter.getSites();
    if (sites.isEmpty()) {
      Toast.makeText(this, "No sites to display on map", Toast.LENGTH_SHORT).show();
      return;
    }

    ProgressDialog pd = new ProgressDialog(this);
    pd.setMessage("Displaying " + sites.size() + " site markers on map...");
    pd.setCancelable(false);
    pd.show();

    TelecomMapLayerManager.getInstance().syncSitesToMap(this, sites, (success, count) -> {
      runOnUiThread(() -> {
        pd.dismiss();
        if (success) {
          Toast.makeText(this, count + " site markers placed on map", Toast.LENGTH_SHORT).show();
          // Find first site with valid coords to focus the map
          for (TelecomSite s : sites) {
            if (s.hasValidCoords()) {
              ExternalNavigationHelper.openInCoMaps(this, s);
              return;
            }
          }
        } else {
          Toast.makeText(this, "Failed to sync markers to map", Toast.LENGTH_SHORT).show();
        }
      });
    });
  }

  private class SearchAdapter extends RecyclerView.Adapter<SearchViewHolder> {
    private final List<TelecomSite> mSites = new ArrayList<>();

    public void setSites(List<TelecomSite> sites) {
      mSites.clear();
      if (sites != null) {
        mSites.addAll(sites);
      }
      notifyDataSetChanged();
    }

    public void appendSites(List<TelecomSite> sites) {
      if (sites != null && !sites.isEmpty()) {
        int startPos = mSites.size();
        mSites.addAll(sites);
        notifyItemRangeInserted(startPos, sites.size());
      }
    }

    public List<TelecomSite> getSites() {
      return new ArrayList<>(mSites);
    }

    @NonNull
    @Override
    public SearchViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
      View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_telecom_search_result, parent, false);
      return new SearchViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull SearchViewHolder holder, int position) {
      TelecomSite site = mSites.get(position);
      holder.tvSiteId.setText(site.getSiteId());
      holder.tvSiteType.setText(site.getSiteType().isEmpty() ? "SITE" : site.getSiteType());
      holder.tvSiteName.setText(site.getSiteName().isEmpty() ? "(No Site Name)" : site.getSiteName());

      if (site.hasDistance()) {
        holder.tvDistance.setVisibility(View.VISIBLE);
        holder.tvDistance.setText(site.getFormattedDistance());
      } else {
        holder.tvDistance.setVisibility(View.GONE);
      }

      StringBuilder locTech = new StringBuilder();
      if (!site.getLocation().isEmpty()) {
        locTech.append("JC: ").append(site.getLocation());
      }
      if (!site.getTechnicianName().isEmpty()) {
        if (locTech.length() > 0) locTech.append(" | ");
        locTech.append("Tech: ").append(site.getTechnicianName());
      }
      holder.tvLocationTech.setText(locTech.toString());
      holder.tvCoordinates.setText(site.hasValidCoords() ? site.getFormattedCoordinates() : "No coordinates");

      holder.itemView.setOnClickListener(v -> TelecomSiteDetailsActivity.start(TelecomSearchActivity.this, site.getSiteId()));

      holder.btnQuickNav.setOnClickListener(v -> {
        if (site.hasValidCoords()) {
          ExternalNavigationHelper.openInGoogleMaps(TelecomSearchActivity.this, site, true);
        } else {
          ExternalNavigationHelper.openInCoMaps(TelecomSearchActivity.this, site);
        }
      });

      holder.btnQuickCopy.setOnClickListener(v -> ExternalNavigationHelper.copyCoordinates(TelecomSearchActivity.this, site));
    }

    @Override
    public int getItemCount() {
      return mSites.size();
    }
  }

  private static class SearchViewHolder extends RecyclerView.ViewHolder {
    TextView tvSiteId;
    TextView tvSiteType;
    TextView tvDistance;
    TextView tvSiteName;
    TextView tvLocationTech;
    TextView tvCoordinates;
    ImageView btnQuickNav;
    ImageView btnQuickCopy;

    public SearchViewHolder(@NonNull View itemView) {
      super(itemView);
      tvSiteId = itemView.findViewById(R.id.tv_site_id);
      tvSiteType = itemView.findViewById(R.id.tv_site_type);
      tvDistance = itemView.findViewById(R.id.tv_distance);
      tvSiteName = itemView.findViewById(R.id.tv_site_name);
      tvLocationTech = itemView.findViewById(R.id.tv_location_tech);
      tvCoordinates = itemView.findViewById(R.id.tv_coordinates);
      btnQuickNav = itemView.findViewById(R.id.btn_quick_nav);
      btnQuickCopy = itemView.findViewById(R.id.btn_quick_copy);
    }
  }
}

