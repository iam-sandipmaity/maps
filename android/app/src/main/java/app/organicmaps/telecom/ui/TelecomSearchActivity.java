package app.organicmaps.telecom.ui;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import app.organicmaps.R;
import app.organicmaps.telecom.db.TelecomDatabaseHelper;
import app.organicmaps.telecom.model.TelecomSite;
import app.organicmaps.telecom.navigation.ExternalNavigationHelper;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.textfield.TextInputEditText;
import java.util.ArrayList;
import java.util.List;

public class TelecomSearchActivity extends AppCompatActivity {

  public static final String EXTRA_INITIAL_QUERY = "extra_initial_query";

  private TextInputEditText mEtSearch;
  private RecyclerView mRvResults;
  private TextView mTvResultCount;
  private TextView mTvEmpty;
  private SearchAdapter mAdapter;

  private final Handler mHandler = new Handler(Looper.getMainLooper());
  private Runnable mSearchRunnable;

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

    mEtSearch = findViewById(R.id.et_search);
    mRvResults = findViewById(R.id.rv_results);
    mTvResultCount = findViewById(R.id.tv_result_count);
    mTvEmpty = findViewById(R.id.tv_empty);

    mAdapter = new SearchAdapter();
    mRvResults.setLayoutManager(new LinearLayoutManager(this));
    mRvResults.setAdapter(mAdapter);

    mEtSearch.addTextChangedListener(new TextWatcher() {
      @Override
      public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

      @Override
      public void onTextChanged(CharSequence s, int start, int before, int count) {
        if (mSearchRunnable != null) {
          mHandler.removeCallbacks(mSearchRunnable);
        }
        mSearchRunnable = () -> performSearch(s != null ? s.toString() : "");
        mHandler.postDelayed(mSearchRunnable, 200); // 200ms debounce
      }

      @Override
      public void afterTextChanged(Editable s) {}
    });

    String initialQuery = getIntent().getStringExtra(EXTRA_INITIAL_QUERY);
    if (initialQuery != null && !initialQuery.isEmpty()) {
      mEtSearch.setText(initialQuery);
    } else {
      performSearch("");
    }
  }

  private void performSearch(@NonNull String query) {
    new Thread(() -> {
      TelecomDatabaseHelper db = TelecomDatabaseHelper.getInstance(this);
      List<TelecomSite> results = db.searchSites(query, 100);
      runOnUiThread(() -> {
        mAdapter.setSites(results);
        if (results.isEmpty()) {
          mTvEmpty.setVisibility(View.VISIBLE);
          mTvResultCount.setVisibility(View.GONE);
        } else {
          mTvEmpty.setVisibility(View.GONE);
          mTvResultCount.setVisibility(View.VISIBLE);
          mTvResultCount.setText("Found " + results.size() + (results.size() == 100 ? "+ matching sites" : " matching sites"));
        }
      });
    }).start();
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
    TextView tvSiteName;
    TextView tvLocationTech;
    TextView tvCoordinates;
    ImageView btnQuickNav;
    ImageView btnQuickCopy;

    public SearchViewHolder(@NonNull View itemView) {
      super(itemView);
      tvSiteId = itemView.findViewById(R.id.tv_site_id);
      tvSiteType = itemView.findViewById(R.id.tv_site_type);
      tvSiteName = itemView.findViewById(R.id.tv_site_name);
      tvLocationTech = itemView.findViewById(R.id.tv_location_tech);
      tvCoordinates = itemView.findViewById(R.id.tv_coordinates);
      btnQuickNav = itemView.findViewById(R.id.btn_quick_nav);
      btnQuickCopy = itemView.findViewById(R.id.btn_quick_copy);
    }
  }
}
