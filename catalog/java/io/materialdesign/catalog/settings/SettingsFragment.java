package io.materialdesign.catalog.settings;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.os.Bundle;
import android.util.SparseIntArray;
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
import com.google.android.material.button.MaterialButton;
import com.google.android.material.button.MaterialButtonToggleGroup;
import dagger.android.support.DaggerFragment;
import io.materialdesign.catalog.BuildConfig;
import io.materialdesign.catalog.R;
import io.materialdesign.catalog.preferences.BaseCatalogPreferences;
import io.materialdesign.catalog.preferences.CatalogPreference;
import io.materialdesign.catalog.preferences.CatalogPreference.Option;
import javax.inject.Inject;

public class SettingsFragment extends DaggerFragment {

  @Inject BaseCatalogPreferences preferences;

  // UI Elements
  private LinearLayout preferencesContainer;
  
  // Updates Section
  private View itemCheckUpdate;
  private LinearLayout updateStatusContainer;
  private TextView newVersionText;
  private Button btnActionUpdate;
  private ProgressBar progressBar;
  private TextView statusText;
  private LinearLayout errorLayout;
  private TextView errorText;
  private Button btnCopyError;
  
  // App Version
  private View itemAppVersion;

  @Nullable
  @Override
  public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
    View view = inflater.inflate(R.layout.cat_settings_fragment, container, false);
    
    preferencesContainer = view.findViewById(R.id.preferences_container);
    setupPreferences(inflater);

    // Updates Section
    itemCheckUpdate = view.findViewById(R.id.item_check_update);
    updateStatusContainer = view.findViewById(R.id.update_status_container);
    newVersionText = view.findViewById(R.id.new_version);
    btnActionUpdate = view.findViewById(R.id.btn_action_update);
    progressBar = view.findViewById(R.id.progress_bar);
    statusText = view.findViewById(R.id.status_text);
    errorLayout = view.findViewById(R.id.error_layout);
    errorText = view.findViewById(R.id.error_text);
    btnCopyError = view.findViewById(R.id.btn_copy_error);

    setupSettingsItem(itemCheckUpdate, R.drawable.ic_home_black_24dp, "Check for Updates", null);
    itemCheckUpdate.setOnClickListener(v -> checkUpdate());
    btnCopyError.setOnClickListener(v -> copyToClipboard(errorText.getText().toString()));

    // About Section
    itemAppVersion = view.findViewById(R.id.item_app_version);
    setupSettingsItem(itemAppVersion, R.drawable.ic_home_black_24dp, "Version", BuildConfig.VERSION_NAME);

    return view;
  }

  private void setupPreferences(LayoutInflater inflater) {
      if (preferences == null) return;
      
      for (CatalogPreference preference : preferences.getPreferences()) {
          View prefView = inflater.inflate(R.layout.mtrl_preferences_dialog_preference, preferencesContainer, false);
          
          TextView description = prefView.findViewById(R.id.preference_description);
          description.setText(preference.description);
          description.setEnabled(preference.isEnabled());

          MaterialButtonToggleGroup toggleGroup = prefView.findViewById(R.id.preference_options);
          toggleGroup.setSingleSelection(true);
          toggleGroup.setSelectionRequired(true);
          toggleGroup.setEnabled(preference.isEnabled());

          SparseIntArray buttonIdToOptionId = new SparseIntArray();
          int selectedOptionId = preference.getSelectedOption(getContext()).id;

          for (Option option : preference.getOptions()) {
              MaterialButton button = (MaterialButton) inflater.inflate(
                  R.layout.mtrl_preferences_dialog_option_button, toggleGroup, false);
              
              int buttonId = View.generateViewId();
              button.setId(buttonId);
              button.setIconResource(option.icon);
              button.setText(option.description);
              button.setChecked(option.id == selectedOptionId);
              button.setEnabled(preference.isEnabled());
              
              buttonIdToOptionId.put(buttonId, option.id);
              toggleGroup.addView(button);
          }

          if (preference.isEnabled()) {
              toggleGroup.addOnButtonCheckedListener((group, checkedId, isChecked) -> {
                  if (isChecked) {
                      preference.setSelectedOption(getContext(), buttonIdToOptionId.get(checkedId));
                  }
              });
          }
          
          preferencesContainer.addView(prefView);
      }
  }

  private void setupSettingsItem(View view, int iconRes, String title, @Nullable String subtitle) {
      ((android.widget.ImageView) view.findViewById(R.id.settings_icon)).setImageResource(iconRes);
      ((TextView) view.findViewById(R.id.settings_title)).setText(title);
      TextView subtitleView = view.findViewById(R.id.settings_subtitle);
      if (subtitle != null) {
          subtitleView.setText(subtitle);
          subtitleView.setVisibility(View.VISIBLE);
      } else {
          subtitleView.setVisibility(View.GONE);
      }
  }

  private void checkUpdate() {
    updateStatusContainer.setVisibility(View.VISIBLE);
    statusText.setText("Status: Checking for updates...");
    statusText.setVisibility(View.VISIBLE);
    progressBar.setVisibility(View.VISIBLE);
    
    // Reset UI
    newVersionText.setVisibility(View.GONE);
    errorLayout.setVisibility(View.GONE);
    btnActionUpdate.setVisibility(View.GONE);
    itemCheckUpdate.setEnabled(false);
    
    // Simulate delay or mostly ensuring UI update
    itemCheckUpdate.postDelayed(() -> {
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
                        
                        btnActionUpdate.setVisibility(View.VISIBLE);
                        btnActionUpdate.setOnClickListener(v -> updateManager.downloadAndInstall(downloadUrl));
                        
                        itemCheckUpdate.setEnabled(true);
                    });
                }
            }

            @Override
            public void onNoUpdate() {
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                         progressBar.setVisibility(View.GONE);
                         itemCheckUpdate.setEnabled(true);
                         statusText.setText("App is up to date");
                    });
                }
            }

            @Override
            public void onError(String error) {
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        progressBar.setVisibility(View.GONE);
                        itemCheckUpdate.setEnabled(true);
                        
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
