package app.organicmaps.telecom.ui;

import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
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
import app.organicmaps.telecom.model.TelecomSite;
import app.organicmaps.telecom.navigation.ExternalNavigationHelper;
import app.organicmaps.telecom.planner.VisitPlannerManager;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.switchmaterial.SwitchMaterial;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class TelecomVisitPlannerActivity extends AppCompatActivity {

  private TextView mTvStopsCount;
  private TextView mTvTotalDistance;
  private TextView mTvStartLocation;
  private TextView mTvEmptyPlan;
  private SwitchMaterial mSwitchReturnToStart;
  private MaterialCardView mCardReturnStop;
  private TextView mTvReturnDestination;
  private TextView mTvReturnDistance;
  private RecyclerView mRvStops;
  private StopsAdapter mAdapter;
  private VisitPlannerManager mPlannerManager;

  public static void start(@NonNull Context context) {
    Intent intent = new Intent(context, TelecomVisitPlannerActivity.class);
    context.startActivity(intent);
  }

  @Override
  protected void onCreate(@Nullable Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_telecom_visit_planner);

    MaterialToolbar toolbar = findViewById(R.id.toolbar);
    setSupportActionBar(toolbar);
    if (getSupportActionBar() != null) {
      getSupportActionBar().setDisplayHomeAsUpEnabled(true);
    }
    toolbar.setNavigationOnClickListener(v -> finish());

    mPlannerManager = VisitPlannerManager.getInstance(this);

    mTvStopsCount = findViewById(R.id.tv_stops_count);
    mTvTotalDistance = findViewById(R.id.tv_total_distance);
    mTvStartLocation = findViewById(R.id.tv_start_location);
    mTvEmptyPlan = findViewById(R.id.tv_empty_plan);
    mSwitchReturnToStart = findViewById(R.id.switch_return_to_start);
    mCardReturnStop = findViewById(R.id.card_return_stop);
    mTvReturnDestination = findViewById(R.id.tv_return_destination);
    mTvReturnDistance = findViewById(R.id.tv_return_distance);
    mRvStops = findViewById(R.id.rv_stops);

    mAdapter = new StopsAdapter();
    mRvStops.setLayoutManager(new LinearLayoutManager(this));
    mRvStops.setAdapter(mAdapter);

    Button btnSetMyLocation = findViewById(R.id.btn_set_my_location);
    Button btnResetStart = findViewById(R.id.btn_reset_start);
    Button btnOptimize = findViewById(R.id.btn_optimize);
    Button btnNavigateNext = findViewById(R.id.btn_navigate_next);
    Button btnShowRouteMap = findViewById(R.id.btn_show_route_map);
    Button btnClearPlan = findViewById(R.id.btn_clear_plan);

    btnSetMyLocation.setOnClickListener(v -> setStartToCurrentGps());
    btnResetStart.setOnClickListener(v -> {
      mPlannerManager.resetStartLocation(this);
      refreshPlanUi();
      Toast.makeText(this, "Start point reset to first site", Toast.LENGTH_SHORT).show();
    });

    mSwitchReturnToStart.setChecked(mPlannerManager.isReturnToStart());
    mSwitchReturnToStart.setOnCheckedChangeListener((buttonView, isChecked) -> {
      mPlannerManager.setReturnToStart(isChecked, this);
      refreshPlanUi();
    });

    btnOptimize.setOnClickListener(v -> showOptimizeConfirmDialog());

    btnNavigateNext.setOnClickListener(v -> {
      List<TelecomSite> sites = mPlannerManager.getPlannedSites();
      if (sites.isEmpty()) {
        Toast.makeText(this, "No stops in the visit plan", Toast.LENGTH_SHORT).show();
        return;
      }
      TelecomSite nextStop = sites.get(0);
      ExternalNavigationHelper.openInGoogleMaps(this, nextStop, true);
    });

    btnShowRouteMap.setOnClickListener(v -> showRouteOnMap());

    btnClearPlan.setOnClickListener(v -> {
      new AlertDialog.Builder(this)
          .setTitle("Clear Visit Plan")
          .setMessage("Are you sure you want to remove all stops from the current visit plan?")
          .setPositiveButton("Clear", (dialog, which) -> {
            mPlannerManager.clearPlan(this);
            refreshPlanUi();
          })
          .setNegativeButton("Cancel", null)
          .show();
    });

    refreshPlanUi();
  }

  @Override
  protected void onResume() {
    super.onResume();
    refreshPlanUi();
  }

  private void setStartToCurrentGps() {
    try {
      Location loc = MwmApplication.from(this).getLocationHelper().getSavedLocation();
      if (loc == null) {
        MapObject myPos = MwmApplication.from(this).getLocationHelper().getMyPosition();
        if (myPos != null) {
          loc = new Location("gps");
          loc.setLatitude(myPos.getLat());
          loc.setLongitude(myPos.getLon());
        }
      }

      if (loc == null) {
        LocationManager lm = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        if (lm != null) {
          try {
            loc = lm.getLastKnownLocation(LocationManager.GPS_PROVIDER);
            if (loc == null) {
              loc = lm.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
            }
          } catch (SecurityException ignored) {}
        }
      }

      if (loc != null) {
        mPlannerManager.setStartFromGps(loc.getLatitude(), loc.getLongitude(), this);
        refreshPlanUi();
        Toast.makeText(this, String.format(Locale.US, "Start location set: %.4f, %.4f", loc.getLatitude(), loc.getLongitude()), Toast.LENGTH_SHORT).show();
      } else {
        Toast.makeText(this, "GPS location not available yet. Please enable GPS and open map once.", Toast.LENGTH_LONG).show();
      }
    } catch (Exception e) {
      Toast.makeText(this, "Unable to get GPS location: " + e.getMessage(), Toast.LENGTH_SHORT).show();
    }
  }

  private void refreshPlanUi() {
    List<TelecomSite> sites = mPlannerManager.getPlannedSites();
    mAdapter.setSites(sites);

    int count = sites.size();
    mTvStopsCount.setText("Stops: " + count);

    double totalDist = mPlannerManager.calculateTotalDistanceKm();
    mTvTotalDistance.setText(String.format(Locale.US, "Est. Distance: %.1f km", totalDist));

    mTvStartLocation.setText("Starting From: " + mPlannerManager.getStartLabel());
    mSwitchReturnToStart.setChecked(mPlannerManager.isReturnToStart());

    if (mPlannerManager.isReturnToStart() && count > 0) {
      mCardReturnStop.setVisibility(View.VISIBLE);
      mTvReturnDestination.setText("Back to " + mPlannerManager.getStartLabel());
      double returnDist = mPlannerManager.calculateReturnLegDistanceKm();
      mTvReturnDistance.setText(String.format(Locale.US, "Return leg: +%.1f km", returnDist));
    } else {
      mCardReturnStop.setVisibility(View.GONE);
    }

    if (count == 0) {
      mTvEmptyPlan.setVisibility(View.VISIBLE);
      mRvStops.setVisibility(View.GONE);
    } else {
      mTvEmptyPlan.setVisibility(View.GONE);
      mRvStops.setVisibility(View.VISIBLE);
    }
  }

  private void showOptimizeConfirmDialog() {
    new AlertDialog.Builder(this)
        .setTitle("Arrange & Optimize Route Sequence")
        .setMessage("This will rearrange your stops starting from " + mPlannerManager.getStartLabel()
            + (mPlannerManager.isReturnToStart() ? " and returning back to base." : ".")
            + "\n\nGeospatial shortest-distance heuristics (Nearest-Neighbor & 2-Opt) will minimize total traveling distance.")
        .setPositiveButton("Optimize", (dialog, which) -> {
          mPlannerManager.optimizeSequence(this);
          refreshPlanUi();
          Toast.makeText(this, "Sequence optimized from start location", Toast.LENGTH_SHORT).show();
        })
        .setNegativeButton("Cancel", null)
        .show();
  }

  private void showRouteOnMap() {
    List<TelecomSite> sites = mPlannerManager.getPlannedSites();
    if (sites.isEmpty()) {
      Toast.makeText(this, "No stops in the visit plan to display", Toast.LENGTH_SHORT).show();
      return;
    }

    double startLat = mPlannerManager.getStartLat();
    double startLon = mPlannerManager.getStartLon();
    String startLabel = mPlannerManager.getStartLabel();
    boolean returnToStart = mPlannerManager.isReturnToStart();

    CharSequence[] options = {"Show in CoMaps (Offline Map & Route)", "Open in Google Maps (Multi-Stop)"};
    new AlertDialog.Builder(this)
        .setTitle("Show Route in Map")
        .setItems(options, (dialog, which) -> {
          if (which == 0) {
            ExternalNavigationHelper.showRouteInCoMaps(this, startLat, startLon, startLabel, sites, returnToStart);
          } else {
            ExternalNavigationHelper.openMultiStopInGoogleMaps(this, startLat, startLon, sites, returnToStart);
          }
        })
        .show();
  }

  private class StopsAdapter extends RecyclerView.Adapter<StopViewHolder> {
    private final List<TelecomSite> mSites = new ArrayList<>();

    public void setSites(List<TelecomSite> sites) {
      mSites.clear();
      if (sites != null) {
        mSites.addAll(sites);
      }
      notifyDataSetChanged();
    }

    @NonNull
    @Override
    public StopViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
      View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_telecom_visit_stop, parent, false);
      return new StopViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull StopViewHolder holder, int position) {
      TelecomSite site = mSites.get(position);
      holder.tvStopNumber.setText(String.valueOf(position + 1));
      holder.tvSiteId.setText(site.getSiteId());

      String nameLoc = site.getSiteName() + (site.getLocation().isEmpty() ? "" : " | " + site.getLocation());
      holder.tvSiteName.setText(nameLoc);
      holder.tvCoordinates.setText(site.hasValidCoords() ? site.getFormattedCoordinates() : "No coordinates");

      double legDist = mPlannerManager.calculateLegDistanceKm(position);
      if (position == 0 && !mPlannerManager.hasCustomStart()) {
        holder.tvLegDistance.setText("Start of journey");
      } else {
        holder.tvLegDistance.setText(String.format(Locale.US, "+%.1f km from %s", legDist, (position == 0 ? "start" : "stop " + position)));
      }

      holder.itemView.setOnClickListener(v -> TelecomSiteDetailsActivity.start(TelecomVisitPlannerActivity.this, site.getSiteId()));

      holder.btnMoveUp.setEnabled(position > 0);
      holder.btnMoveUp.setAlpha(position > 0 ? 1.0f : 0.3f);
      holder.btnMoveUp.setOnClickListener(v -> {
        mPlannerManager.moveUp(position, TelecomVisitPlannerActivity.this);
        refreshPlanUi();
      });

      holder.btnMoveDown.setEnabled(position < mSites.size() - 1);
      holder.btnMoveDown.setAlpha(position < mSites.size() - 1 ? 1.0f : 0.3f);
      holder.btnMoveDown.setOnClickListener(v -> {
        mPlannerManager.moveDown(position, TelecomVisitPlannerActivity.this);
        refreshPlanUi();
      });

      holder.btnNav.setOnClickListener(v -> {
        if (site.hasValidCoords()) {
          ExternalNavigationHelper.openInGoogleMaps(TelecomVisitPlannerActivity.this, site, true);
        } else {
          ExternalNavigationHelper.openInCoMaps(TelecomVisitPlannerActivity.this, site);
        }
      });

      holder.btnRemove.setOnClickListener(v -> {
        mPlannerManager.removeSite(TelecomVisitPlannerActivity.this, site.getSiteId());
        refreshPlanUi();
      });
    }

    @Override
    public int getItemCount() {
      return mSites.size();
    }
  }

  private static class StopViewHolder extends RecyclerView.ViewHolder {
    TextView tvStopNumber;
    TextView tvSiteId;
    TextView tvSiteName;
    TextView tvCoordinates;
    TextView tvLegDistance;
    ImageView btnMoveUp;
    ImageView btnMoveDown;
    ImageView btnNav;
    ImageView btnRemove;

    public StopViewHolder(@NonNull View itemView) {
      super(itemView);
      tvStopNumber = itemView.findViewById(R.id.tv_stop_number);
      tvSiteId = itemView.findViewById(R.id.tv_site_id);
      tvSiteName = itemView.findViewById(R.id.tv_site_name);
      tvCoordinates = itemView.findViewById(R.id.tv_coordinates);
      tvLegDistance = itemView.findViewById(R.id.tv_leg_distance);
      btnMoveUp = itemView.findViewById(R.id.btn_move_up);
      btnMoveDown = itemView.findViewById(R.id.btn_move_down);
      btnNav = itemView.findViewById(R.id.btn_nav);
      btnRemove = itemView.findViewById(R.id.btn_remove);
    }
  }
}
