package io.materialdesign.catalog.settings;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import androidx.core.content.FileProvider;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.json.JSONException;
import org.json.JSONObject;

public class UpdateManager {

    private final Context context;
    private final OkHttpClient client;
    private static final String GITHUB_REPO_OWNER = "Abhishek-Maurya2";
    private static final String GITHUB_REPO_NAME = "tool";

    public interface UpdateCallback {
        void onUpdateAvailable(String version, String downloadUrl);
        void onNoUpdate();
        void onError(String error);
    }

    public UpdateManager(Context context) {
        this.context = context;
        this.client = new OkHttpClient();
    }

    public void checkForUpdates(UpdateCallback callback) {
        String url = "https://api.github.com/repos/" + GITHUB_REPO_OWNER + "/" + GITHUB_REPO_NAME + "/releases/latest";
        Request request = new Request.Builder().url(url).build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                callback.onError(e.getMessage());
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (!response.isSuccessful()) {
                    String errorBody = response.body() != null ? response.body().string() : "No error body";
                    callback.onError("Failed to check for updates (" + response.code() + "): " + errorBody);
                    return;
                }

                try {
                    String jsonData = response.body().string();
                    JSONObject jsonObject = new JSONObject(jsonData);
                    String latestVersion = jsonObject.getString("tag_name"); // Assuming tag_name is like "v1.0.1"
                    String downloadUrl = jsonObject.getJSONArray("assets")
                            .getJSONObject(0) // Assuming the first asset is the APK
                            .getString("browser_download_url");

                    // Simple version compare logic (lexicographical for now, should be robust semver in prod)
                    String currentVersion = "v" + io.materialdesign.catalog.BuildConfig.VERSION_NAME;
                    
                    // You might want to strip 'v' prefix for comparison
                    if (!latestVersion.equals(currentVersion)) { 
                         callback.onUpdateAvailable(latestVersion, downloadUrl);
                    } else {
                         callback.onNoUpdate();
                    }

                } catch (JSONException e) {
                    callback.onError("Failed to parse update info: " + e.getMessage());
                }
            }
        });
    }

    public void downloadAndInstall(String downloadUrl) {
         Request request = new Request.Builder().url(downloadUrl).build();
         client.newCall(request).enqueue(new Callback() {
             @Override
             public void onFailure(Call call, IOException e) {
                 // Handle error (maybe broadcast intent or callback)
             }

             @Override
             public void onResponse(Call call, Response response) throws IOException {
                 if (!response.isSuccessful()) return;

                 File file = new File(context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS), "update.apk");
                 try (FileOutputStream fos = new FileOutputStream(file)) {
                     fos.write(response.body().bytes());
                 }
                 
                 installApk(file);
             }
         });
    }

    private void installApk(File file) {
        Intent intent = new Intent(Intent.ACTION_VIEW);
        Uri contentUri = FileProvider.getUriForFile(context, context.getPackageName() + ".provider", file);
        
        intent.setDataAndType(contentUri, "application/vnd.android.package-archive");
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        
        context.startActivity(intent);
    }
}
