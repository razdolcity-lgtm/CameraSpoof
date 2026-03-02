package com.example.cameraspoof;

import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CaptureRequest;
import android.util.Log;
import android.view.Surface;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public class VirtualCameraDevice {
    private static final String TAG = "VirtualCameraDevice";
    private Map<Surface, SurfaceTexture> surfaceTextures = new HashMap<>();
    private VideoStreamService videoStreamService;
    private boolean isActive = false;

    public VirtualCameraDevice() {
        videoStreamService = new VideoStreamService();
        videoStreamService.addConsumer(new VideoStreamService.VideoFrameConsumer() {
            @Override
            public void onFrameAvailable(byte[] frameData, int width, int height) {
                deliverFrameToSurfaces(frameData, width, height);
            }
        });
    }

    public Surface createVirtualSurface(Surface originalSurface) {
        try {
            // Create a virtual surface that mirrors the original
            SurfaceTexture virtualTexture = new SurfaceTexture(0);
            virtualTexture.setDefaultBufferSize(1920, 1080);
            
            Surface virtualSurface = new Surface(virtualTexture);
            surfaceTextures.put(virtualSurface, virtualTexture);
            
            Log.d(TAG, "Created virtual surface");
            return virtualSurface;
            
        } catch (Exception e) {
            Log.e(TAG, "Error creating virtual surface", e);
            return originalSurface; // Fallback to original
        }
    }

    private void deliverFrameToSurfaces(byte[] frameData, int width, int height) {
        for (Map.Entry<Surface, SurfaceTexture> entry : surfaceTextures.entrySet()) {
            Surface surface = entry.getKey();
            SurfaceTexture texture = entry.getValue();
            
            try {
                // Update surface texture with new frame data
                updateSurfaceTexture(texture, frameData, width, height);
                
                // Notify surface that texture is updated
                texture.updateTexImage();
                
            } catch (Exception e) {
                Log.e(TAG, "Error delivering frame to surface", e);
            }
        }
    }

    private void updateSurfaceTexture(SurfaceTexture texture, byte[] frameData, int width, int height) {
        // Convert NV21 frame data to texture format
        // This is a simplified version - you'd need proper YUV to RGB conversion
        try {
            // For now, we'll just mark the texture as updated
            // In a real implementation, you'd use OpenGL ES to render the frame
            texture.updateTexImage();
        } catch (Exception e) {
            Log.e(TAG, "Error updating surface texture", e);
        }
    }

    public void setActive(boolean active) {
        this.isActive = active;
        
        if (active) {
            videoStreamService.startStreaming();
            Log.d(TAG, "Virtual camera activated");
        } else {
            videoStreamService.stopStreaming();
            Log.d(TAG, "Virtual camera deactivated");
        }
    }

    public boolean isActive() {
        return isActive;
    }

    public void cleanup() {
        videoStreamService.stopStreaming();
        
        // Release all surfaces and textures
        for (Map.Entry<Surface, SurfaceTexture> entry : surfaceTextures.entrySet()) {
            try {
                entry.getKey().release();
                entry.getValue().release();
            } catch (Exception e) {
                Log.e(TAG, "Error releasing surface/texture", e);
            }
        }
        
        surfaceTextures.clear();
        Log.d(TAG, "Virtual camera device cleaned up");
    }
}
