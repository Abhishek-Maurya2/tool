package io.materialdesign.catalog.settings;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import io.materialdesign.catalog.BuildConfig;
import io.materialdesign.catalog.R;

public class SettingsFragment extends Fragment {

  private TextView appVersion;
  private TextView newVersionText;
  private Button btnCheckUpdate;
  private ProgressBar progressBar;
  private TextView statusText;
  private LinearLayout errorLayout;
  private TextView errorText;
  private Button btnCopyError;

  @Nullable
  @Override
  public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
    View view = inflater.inflate(R.layout.cat_settings_fragment, container, false);
    
    appVersion = view.findViewById(R.id.app_version);
    newVersionText = view.findViewById(R.id.new_version);
    btnCheckUpdate = view.findViewById(R.id.btn_check_update);
    progressBar = view.findViewById(R.id.progress_bar);
    statusText = view.findViewById(R.id.status_text);
    errorLayout = view.findViewById(R.id.error_layout);
    errorText = view.findViewById(R.id.error_text);
    btnCopyError = view.findViewById(R.id.btn_copy_error);

    appVersion.setText("Current Version: " + BuildConfig.VERSION_NAME);

    btnCheckUpdate.setOnClickListener(v -> checkUpdate());
    
    btnCopyError.setOnClickListener(v -> {
        copyToClipboard(errorText.getText().toString());
    });

    return view;
  }

  private void checkUpdate() {
    statusText.setText("Status: Checking for updates...");
    statusText.setVisibility(View.VISIBLE);
    progressBar.setVisibility(View.VISIBLE);
    
    // Reset UI
    newVersionText.setVisibility(View.GONE);
    errorLayout.setVisibility(View.GONE);
    btnCheckUpdate.setEnabled(false);
    
    // Simulate delay or mostly ensuring UI update
    btnCheckUpdate.postDelayed(() -> {
        if (getContext() == null) return;
        
        UpdateManager updateManager = new UpdateManager(getContext());
        updateManager.checkForUpdates(new UpdateManager.UpdateCallback() {
            @Override
            public void onUpdateAvailable(String version, String downloadUrl) {
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        progressBar.setVisibility(View.GONE);
                        statusText.setText("Update Available!");
                        
                        newVersionText.setText("New Version: " + version);
                        newVersionText.setVisibility(View.VISIBLE);
                        
                        btnCheckUpdate.setEnabled(true);
                        btnCheckUpdate.setText("Download & Install");
                        btnCheckUpdate.setOnClickListener(v -> updateManager.downloadAndInstall(downloadUrl));
                    });
                }
            }

            @Override
            public void onNoUpdate() {
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                         progressBar.setVisibility(View.GONE);
                         btnCheckUpdate.setEnabled(true);
                         btnCheckUpdate.setText("Check for Update");
                         statusText.setText("App is up to date");
                    });
                }
            }

            @Override
            public void onError(String error) {
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        progressBar.setVisibility(View.GONE);
                        btnCheckUpdate.setEnabled(true);
                        
                        statusText.setText("Check Failed");
                        
                        errorText.setText(error);
                        errorLayout.setVisibility(View.VISIBLE);
                    });
                }
            }
        });

    }, 500);
  }

  private void copyToClipboard(String text) {
      if (getContext() == null) return;
      ClipboardManager clipboard = (ClipboardManager) getContext().getSystemService(Context.CLIPBOARD_SERVICE);
      ClipData clip = ClipData.newPlainText("Error Message", text);
      clipboard.setPrimaryClip(clip);
      Toast.makeText(getContext(), "Error copied to clipboard", Toast.LENGTH_SHORT).show();
  }
}
