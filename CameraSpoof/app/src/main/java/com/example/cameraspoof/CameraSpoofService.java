package com.example.cameraspoof;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;
import java.io.File;

public class CameraSpoofService {
    private static final String TAG = "CameraSpoofService";
    private static final String PREFS_NAME = "camera_spoof_prefs";
    private static final String KEY_SPOOFING_ACTIVE = "spoofing_active";
    private static final String KEY_VIDEO_PATH = "video_path";
    private static final String KEY_LOOP_ENABLED = "loop_enabled";
    
    private Context context;
    private SharedPreferences prefs;
    private VirtualCameraManager virtualCameraManager;
    private boolean isSpoofing = false;

    public CameraSpoofService(Context context) {
        this.context = context;
        this.prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        this.virtualCameraManager = new VirtualCameraManager();
        
        // Restore previous settings
        if (prefs.getBoolean(KEY_SPOOFING_ACTIVE, false)) {
            startSpoofing();
        }
        
        String videoPath = prefs.getString(KEY_VIDEO_PATH, "");
        if (!videoPath.isEmpty()) {
            virtualCameraManager.updateVideoSource(videoPath);
        }
        
        boolean loopEnabled = prefs.getBoolean(KEY_LOOP_ENABLED, true);
        virtualCameraManager.setVideoLoopEnabled(loopEnabled);
    }

    public boolean startSpoofing() {
        if (isSpoofing) {
            Log.d(TAG, "Spoofing already active");
            return true;
        }

        try {
            // Check if Xposed module is loaded
            if (!isXposedModuleLoaded()) {
                Log.e(TAG, "Xposed module not loaded");
                return false;
            }

            // Start virtual camera manager
            virtualCameraManager.setVideoLoopEnabled(prefs.getBoolean(KEY_LOOP_ENABLED, true));
            
            isSpoofing = true;
            prefs.edit().putBoolean(KEY_SPOOFING_ACTIVE, true).apply();
            
            Log.d(TAG, "Camera spoofing started successfully");
            return true;

        } catch (Exception e) {
            Log.e(TAG, "Failed to start camera spoofing", e);
            return false;
        }
    }

    public boolean stopSpoofing() {
        if (!isSpoofing) {
            Log.d(TAG, "Spoofing not active");
            return true;
        }

        try {
            // Cleanup virtual camera manager
            if (virtualCameraManager != null) {
                virtualCameraManager.cleanup();
            }
            
            isSpoofing = false;
            prefs.edit().putBoolean(KEY_SPOOFING_ACTIVE, false).apply();
            
            Log.d(TAG, "Camera spoofing stopped successfully");
            return true;

        } catch (Exception e) {
            Log.e(TAG, "Failed to stop camera spoofing", e);
            return false;
        }
    }

    public boolean isSpoofingActive() {
        return isSpoofing;
    }

    public void setVideoSource(String videoPath) {
        if (videoPath != null && !videoPath.isEmpty()) {
            File videoFile = new File(videoPath);
            if (videoFile.exists()) {
                virtualCameraManager.updateVideoSource(videoPath);
                prefs.edit().putString(KEY_VIDEO_PATH, videoPath).apply();
                Log.d(TAG, "Video source updated: " + videoPath);
            } else {
                Log.e(TAG, "Video file not found: " + videoPath);
            }
        }
    }

    public void setLoopEnabled(boolean enabled) {
        virtualCameraManager.setVideoLoopEnabled(enabled);
        prefs.edit().putBoolean(KEY_LOOP_ENABLED, enabled).apply();
        Log.d(TAG, "Video loop " + (enabled ? "enabled" : "disabled"));
    }

    private boolean isXposedModuleLoaded() {
        try {
            // Check if our hook module is loaded by looking for evidence
            // This is a simple check - in practice you might want more sophisticated detection
            Class.forName("de.robv.android.xposed.XposedHelpers");
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }

    public String getVideoSource() {
        return prefs.getString(KEY_VIDEO_PATH, "");
    }

    public boolean isLoopEnabled() {
        return prefs.getBoolean(KEY_LOOP_ENABLED, true);
    }

    public void cleanup() {
        stopSpoofing();
    }
}
