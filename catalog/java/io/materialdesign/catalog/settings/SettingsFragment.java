package io.materialdesign.catalog.settings;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import io.materialdesign.catalog.BuildConfig;
import io.materialdesign.catalog.R;

public class SettingsFragment extends Fragment {

  private TextView appVersion;
  private Button btnCheckUpdate;
  private ProgressBar progressBar;
  private TextView statusText;

  @Nullable
  @Override
  public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
    View view = inflater.inflate(R.layout.cat_settings_fragment, container, false);
    
    appVersion = view.findViewById(R.id.app_version);
    btnCheckUpdate = view.findViewById(R.id.btn_check_update);
    progressBar = view.findViewById(R.id.progress_bar);
    statusText = view.findViewById(R.id.status_text);

    appVersion.setText("Version: " + BuildConfig.VERSION_NAME);

    btnCheckUpdate.setOnClickListener(v -> checkUpdate());

    return view;
  }

  private void checkUpdate() {
    // Placeholder for update logic
    statusText.setText("Status: Checking for updates...");
    progressBar.setVisibility(View.VISIBLE);
    
    // Simulate delay
    btnCheckUpdate.postDelayed(() -> {
        if (getContext() == null) return;
        
        // This will be replaced by actual logic later
        UpdateManager updateManager = new UpdateManager(getContext());
        updateManager.checkForUpdates(new UpdateManager.UpdateCallback() {
            @Override
            public void onUpdateAvailable(String version, String downloadUrl) {
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        progressBar.setVisibility(View.GONE);
                        statusText.setText("Update available: " + version);
                        btnCheckUpdate.setText("Update");
                        btnCheckUpdate.setOnClickListener(v -> updateManager.downloadAndInstall(downloadUrl));
                    });
                }
            }

            @Override
            public void onNoUpdate() {
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                         progressBar.setVisibility(View.GONE);
                         statusText.setText("App is up to date");
                    });
                }
            }

            @Override
            public void onError(String error) {
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        progressBar.setVisibility(View.GONE);
                        statusText.setText("Error: " + error);
                    });
                }
            }
        });

    }, 1000);
  }
}
