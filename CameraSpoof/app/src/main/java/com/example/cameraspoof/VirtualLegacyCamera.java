package com.example.cameraspoof;

import android.hardware.Camera;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.util.Log;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

public class VirtualLegacyCamera {
    private static final String TAG = "VirtualLegacyCamera";
    private VideoStreamService videoStreamService;
    private Camera.Parameters parameters;
    private boolean isPreviewActive = false;
    private List<Camera.PreviewCallback> previewCallbacks = new ArrayList<>();

    public VirtualLegacyCamera() {
        videoStreamService = new VideoStreamService();
        videoStreamService.addConsumer(new VideoStreamService.VideoFrameConsumer() {
            @Override
            public void onFrameAvailable(byte[] frameData, int width, int height) {
                deliverPreviewFrame(frameData, width, height);
            }
        });
        
        initializeParameters();
    }

    private void initializeParameters() {
        try {
            // Create a mock Camera.Parameters object
            parameters = createMockParameters();
        } catch (Exception e) {
            Log.e(TAG, "Error initializing parameters", e);
        }
    }

    private Camera.Parameters createMockParameters() {
        // Since we can't directly create Camera.Parameters, we'll use reflection
        // or return null and handle it gracefully
        return null; // Apps should handle null parameters
    }

    public void startPreview() {
        if (isPreviewActive) return;
        
        isPreviewActive = true;
        videoStreamService.startStreaming();
        
        Log.d(TAG, "Legacy camera preview started");
    }

    public void stopPreview() {
        isPreviewActive = false;
        videoStreamService.stopStreaming();
        
        Log.d(TAG, "Legacy camera preview stopped");
    }

    public void setPreviewDisplay(SurfaceHolder holder) {
        // Virtual camera doesn't need actual surface display
        Log.d(TAG, "Preview display set (virtual)");
    }

    public void setPreviewCallback(Camera.PreviewCallback callback) {
        if (callback != null && !previewCallbacks.contains(callback)) {
            previewCallbacks.add(callback);
        }
    }

    public void setOneShotPreviewCallback(Camera.PreviewCallback callback) {
        if (callback != null) {
            previewCallbacks.add(callback);
            // Will be removed after one frame
        }
    }

    private void deliverPreviewFrame(byte[] frameData, int width, int height) {
        if (!isPreviewActive || previewCallbacks.isEmpty()) return;
        
        List<Camera.PreviewCallback> currentCallbacks = new ArrayList<>(previewCallbacks);
        List<Camera.PreviewCallback> oneShotCallbacks = new ArrayList<>();
        
        for (Camera.PreviewCallback callback : currentCallbacks) {
            try {
                callback.onPreviewFrame(frameData, this);
                
                // Check if this was a one-shot callback
                if (isOneShotCallback(callback)) {
                    oneShotCallbacks.add(callback);
                }
                
            } catch (Exception e) {
                Log.e(TAG, "Error delivering preview frame", e);
            }
        }
        
        // Remove one-shot callbacks
        previewCallbacks.removeAll(oneShotCallbacks);
    }

    private boolean isOneShotCallback(Camera.PreviewCallback callback) {
        // This is a simplified check - in reality, you'd need to track
        // which callbacks were set with setOneShotPreviewCallback
        return false;
    }

    public Camera.Parameters getParameters() {
        return parameters;
    }

    public void setParameters(Camera.Parameters params) {
        // Virtual camera ignores parameter changes
        Log.d(TAG, "Parameters set (ignored in virtual camera)");
    }

    public void release() {
        stopPreview();
        previewCallbacks.clear();
        Log.d(TAG, "Legacy camera released");
    }

    public void autoFocus(Camera.AutoFocusCallback callback) {
        // Virtual camera simulates auto-focus
        if (callback != null) {
            callback.onAutoFocus(true, this);
        }
    }

    public void takePicture(Camera.ShutterCallback shutter, 
                          Camera.PictureCallback raw, 
                          Camera.PictureCallback jpeg) {
        // Simulate picture taking
        if (shutter != null) {
            shutter.onShutter();
        }
        
        if (jpeg != null) {
            // Create a mock JPEG image
            byte[] mockJpeg = createMockJpegImage();
            jpeg.onPictureTaken(mockJpeg, this);
        }
    }

    private byte[] createMockJpegImage() {
        // Create a minimal JPEG header
        byte[] jpegHeader = new byte[] {
            (byte) 0xFF, (byte) 0xD8, // JPEG SOI marker
            (byte) 0xFF, (byte) 0xE0, // APP0 marker
            0x00, 0x10, // Length
            'J', 'F', 'I', 'F', 0x00, // JFIF identifier
            0x01, 0x01, // Version
            0x01, // Density unit
            0x48, 0x00, 0x48, 0x00, // X and Y density (72x72 DPI)
            0x00, // Thumbnail width
            0x00, // Thumbnail height
            (byte) 0xFF, (byte) 0xD9 // JPEG EOI marker
        };
        
        return jpegHeader;
    }

    public int getNumberOfCameras() {
        return 1; // Virtual camera reports as single camera
    }

    public Camera.CameraInfo getCameraInfo(int cameraId) {
        Camera.CameraInfo info = new Camera.CameraInfo();
        info.facing = Camera.CameraInfo.CAMERA_FACING_FRONT;
        info.orientation = 90; // Standard front camera orientation
        return info;
    }
}
