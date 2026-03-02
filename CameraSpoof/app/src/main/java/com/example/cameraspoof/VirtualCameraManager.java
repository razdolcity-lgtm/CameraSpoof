package com.example.cameraspoof;

import android.hardware.Camera;
import android.view.Surface;
import android.util.Log;
import java.util.ArrayList;
import java.util.List;
import java.util.Arrays;

public class VirtualCameraManager {
    private static final String TAG = "VirtualCameraManager";
    private static final String VIRTUAL_CAMERA_ID = "virtual_camera_0";
    private VideoStreamService videoStreamService;
    private VirtualCameraDevice virtualCameraDevice;
    private VirtualLegacyCamera virtualLegacyCamera;

    public VirtualCameraManager() {
        videoStreamService = new VideoStreamService();
        virtualCameraDevice = new VirtualCameraDevice();
        virtualLegacyCamera = new VirtualLegacyCamera();
        
        // Start video streaming service
        videoStreamService.startStreaming();
        
        Log.d(TAG, "VirtualCameraManager initialized");
    }

    public String getVirtualCameraId() {
        return VIRTUAL_CAMERA_ID;
    }

    public String[] injectVirtualCamera(String[] originalCameraIds) {
        List<String> cameraList = new ArrayList<>();
        
        // Add virtual camera first (so apps pick it by default)
        cameraList.add(VIRTUAL_CAMERA_ID);
        
        // Add original cameras
        if (originalCameraIds != null) {
            cameraList.addAll(Arrays.asList(originalCameraIds));
        }
        
        return cameraList.toArray(new String[0]);
    }

    public List<Surface> getVirtualSurfaces(List<Surface> originalSurfaces) {
        List<Surface> virtualSurfaces = new ArrayList<>();
        
        // Replace each surface with virtual camera surface
        for (Surface originalSurface : originalSurfaces) {
            Surface virtualSurface = virtualCameraDevice.createVirtualSurface(originalSurface);
            virtualSurfaces.add(virtualSurface);
        }
        
        return virtualSurfaces;
    }

    public Camera getLegacyVirtualCamera() {
        return virtualLegacyCamera;
    }

    public void updateVideoSource(String videoPath) {
        videoStreamService.loadVideo(videoPath);
    }

    public void setVideoLoopEnabled(boolean enabled) {
        videoStreamService.setLoopEnabled(enabled);
    }

    public void cleanup() {
        if (videoStreamService != null) {
            videoStreamService.stopStreaming();
        }
        if (virtualCameraDevice != null) {
            virtualCameraDevice.cleanup();
        }
        if (virtualLegacyCamera != null) {
            virtualLegacyCamera.release();
        }
    }
}
