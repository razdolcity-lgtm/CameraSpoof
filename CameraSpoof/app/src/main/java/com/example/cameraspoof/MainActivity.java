package com.example.cameraspoof;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Switch;
import androidx.appcompat.app.AppCompatActivity;
import android.provider.Settings;
import android.net.Uri;
import android.widget.Toast;

public class MainActivity extends AppCompatActivity {
    private TextView statusText;
    private Button enableButton;
    private Button disableButton;
    private Button selectVideoButton;
    private Switch loopSwitch;
    private CameraSpoofService spoofService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        statusText = findViewById(R.id.statusText);
        enableButton = findViewById(R.id.enableButton);
        disableButton = findViewById(R.id.disableButton);
        selectVideoButton = findViewById(R.id.selectVideoButton);
        loopSwitch = findViewById(R.id.loopSwitch);

        spoofService = new CameraSpoofService(this);

        enableButton.setOnClickListener(v -> enableCameraSpoofing());
        disableButton.setOnClickListener(v -> disableCameraSpoofing());
        selectVideoButton.setOnClickListener(v -> selectVideoFile());
        loopSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            spoofService.setLoopEnabled(isChecked);
        });

        checkXposedInstalled();
        updateStatus();
    }

    private void checkXposedInstalled() {
        try {
            Class.forName("de.robv.android.xposed.IXposedHookLoadPackage");
            statusText.setText("Xposed Framework detected");
        } catch (ClassNotFoundException e) {
            statusText.setText("Xposed Framework not found - please install Xposed");
            showXposedInstallDialog();
        }
    }

    private void showXposedInstallDialog() {
        new androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Xposed Required")
            .setMessage("This app requires Xposed Framework to work. Install Xposed and reboot your device.")
            .setPositiveButton("Install Xposed", (dialog, which) -> openXposedDownload())
            .setNegativeButton("Cancel", null)
            .show();
    }

    private void openXposedDownload() {
        try {
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/rovo89/XposedInstaller/releases"));
            startActivity(intent);
        } catch (Exception e) {
            Toast.makeText(this, "Could not open download link", Toast.LENGTH_SHORT).show();
        }
    }

    private void enableCameraSpoofing() {
        if (!checkRootAccess()) {
            Toast.makeText(this, "Root access required", Toast.LENGTH_LONG).show();
            return;
        }

        boolean success = spoofService.startSpoofing();
        if (success) {
            statusText.setText("Camera Spoofing Active");
            enableButton.setEnabled(false);
            disableButton.setEnabled(true);
            Toast.makeText(this, "Camera spoofing enabled - all apps will use virtual camera", Toast.LENGTH_LONG).show();
        } else {
            Toast.makeText(this, "Failed to enable camera spoofing", Toast.LENGTH_SHORT).show();
        }
    }

    private void disableCameraSpoofing() {
        boolean success = spoofService.stopSpoofing();
        if (success) {
            statusText.setText("Camera Spoofing Disabled");
            enableButton.setEnabled(true);
            disableButton.setEnabled(false);
            Toast.makeText(this, "Camera spoofing disabled - apps will use real cameras", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(this, "Failed to disable camera spoofing", Toast.LENGTH_SHORT).show();
        }
    }

    private void selectVideoFile() {
        // TODO: Implement video file selection
        Toast.makeText(this, "Video selection coming soon", Toast.LENGTH_SHORT).show();
    }

    private boolean checkRootAccess() {
        try {
            Process process = Runtime.getRuntime().exec("su");
            process.getOutputStream().write("exit\n".getBytes());
            process.getOutputStream().flush();
            return process.waitFor() == 0;
        } catch (Exception e) {
            return false;
        }
    }

    private void updateStatus() {
        if (spoofService.isSpoofingActive()) {
            statusText.setText("Camera Spoofing Active");
            enableButton.setEnabled(false);
            disableButton.setEnabled(true);
        } else {
            statusText.setText("Camera Spoofing Inactive");
            enableButton.setEnabled(true);
            disableButton.setEnabled(false);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateStatus();
    }
}
