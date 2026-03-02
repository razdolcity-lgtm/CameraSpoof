package com.example.cameraspoof;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.MediaMetadataRetriever;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import java.io.File;
import java.util.concurrent.atomic.AtomicBoolean;

public class VideoStreamService {
    private static final String TAG = "VideoStreamService";
    private static final int FRAME_RATE = 30;
    private static final int FRAME_DELAY_MS = 1000 / FRAME_RATE;
    
    private Thread streamingThread;
    private AtomicBoolean isStreaming = new AtomicBoolean(false);
    private VideoFrameProvider frameProvider;
    private Handler mainHandler;
    private List<VideoFrameConsumer> consumers = new ArrayList<>();
    
    public interface VideoFrameConsumer {
        void onFrameAvailable(byte[] frameData, int width, int height);
    }

    public VideoStreamService() {
        frameProvider = new VideoFrameProvider();
        mainHandler = new Handler(Looper.getMainLooper());
    }

    public void startStreaming() {
        if (isStreaming.get()) return;
        
        isStreaming.set(true);
        streamingThread = new Thread(() -> {
            Log.d(TAG, "Video streaming started");
            
            while (isStreaming.get()) {
                try {
                    // Get next frame from video provider
                    VideoFrame frame = frameProvider.getNextFrame();
                    
                    if (frame != null) {
                        // Distribute frame to all consumers
                        distributeFrame(frame);
                    }
                    
                    Thread.sleep(FRAME_DELAY_MS);
                    
                } catch (InterruptedException e) {
                    Log.d(TAG, "Streaming interrupted");
                    break;
                } catch (Exception e) {
                    Log.e(TAG, "Error in streaming loop", e);
                }
            }
            
            Log.d(TAG, "Video streaming stopped");
        });
        
        streamingThread.start();
    }

    public void stopStreaming() {
        isStreaming.set(false);
        
        if (streamingThread != null) {
            streamingThread.interrupt();
            try {
                streamingThread.join(1000);
            } catch (InterruptedException e) {
                Log.e(TAG, "Error stopping streaming thread", e);
            }
        }
    }

    public void loadVideo(String videoPath) {
        frameProvider.loadVideo(videoPath);
    }

    public void setLoopEnabled(boolean enabled) {
        frameProvider.setLoopEnabled(enabled);
    }

    public void addConsumer(VideoFrameConsumer consumer) {
        if (!consumers.contains(consumer)) {
            consumers.add(consumer);
        }
    }

    public void removeConsumer(VideoFrameConsumer consumer) {
        consumers.remove(consumer);
    }

    private void distributeFrame(VideoFrame frame) {
        // Copy consumers list to avoid concurrent modification
        List<VideoFrameConsumer> currentConsumers = new ArrayList<>(consumers);
        
        for (VideoFrameConsumer consumer : currentConsumers) {
            try {
                consumer.onFrameAvailable(frame.data, frame.width, frame.height);
            } catch (Exception e) {
                Log.e(TAG, "Error delivering frame to consumer", e);
            }
        }
    }

    private static class VideoFrame {
        byte[] data;
        int width;
        int height;
        
        VideoFrame(byte[] data, int width, int height) {
            this.data = data;
            this.width = width;
            this.height = height;
        }
    }

    private static class VideoFrameProvider {
        private MediaMetadataRetriever retriever;
        private List<byte[]> videoFrames;
        private int currentFrameIndex = 0;
        private boolean loopEnabled = true;
        private int width = 1920;
        private int height = 1080;

        public VideoFrameProvider() {
            videoFrames = new ArrayList<>();
            createSyntheticFrames();
        }

        private void createSyntheticFrames() {
            videoFrames.clear();
            
            // Create synthetic test pattern frames
            for (int i = 0; i < 30; i++) {
                byte[] frame = createTestPatternFrame(i);
                videoFrames.add(frame);
            }
            
            Log.d(TAG, "Created " + videoFrames.size() + " synthetic frames");
        }

        private byte[] createTestPatternFrame(int frameNumber) {
            int frameSize = width * height * 3 / 2; // NV21 format
            byte[] frame = new byte[frameSize];
            
            // Create moving test pattern
            for (int y = 0; y < height; y++) {
                for (int x = 0; x < width; x++) {
                    int index = y * width + x;
                    int value = (x + y + frameNumber * 10) % 256;
                    frame[index] = (byte) value;
                }
            }
            
            // Add UV data
            int uvStart = width * height;
            for (int i = uvStart; i < frameSize; i++) {
                frame[i] = (byte) 128;
            }
            
            return frame;
        }

        public void loadVideo(String videoPath) {
            try {
                if (retriever != null) {
                    retriever.release();
                }
                
                retriever = new MediaMetadataRetriever();
                retriever.setDataSource(videoPath);
                
                extractVideoFrames();
                
            } catch (Exception e) {
                Log.e(TAG, "Error loading video: " + videoPath, e);
                createSyntheticFrames();
            }
        }

        private void extractVideoFrames() {
            videoFrames.clear();
            
            try {
                String durationStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION);
                long duration = Long.parseLong(durationStr);
                int totalFrames = (int) (duration * FRAME_RATE / 1000);
                
                for (int i = 0; i < totalFrames; i++) {
                    long timeUs = (i * 1000000L) / FRAME_RATE;
                    Bitmap bitmap = retriever.getFrameAtTime(timeUs);
                    
                    if (bitmap != null) {
                        byte[] frameData = bitmapToNV21(bitmap);
                        videoFrames.add(frameData);
                    }
                }
                
                currentFrameIndex = 0;
                Log.d(TAG, "Extracted " + videoFrames.size() + " frames");
                
            } catch (Exception e) {
                Log.e(TAG, "Error extracting frames", e);
                createSyntheticFrames();
            }
        }

        private byte[] bitmapToNV21(Bitmap bitmap) {
            int[] pixels = new int[width * height];
            bitmap.getPixels(pixels, 0, width, 0, 0, width, height);
            
            int frameSize = width * height * 3 / 2;
            byte[] nv21 = new byte[frameSize];
            
            // Convert RGB to YUV420 (NV21)
            for (int i = 0; i < width * height; i++) {
                int pixel = pixels[i];
                int r = (pixel >> 16) & 0xFF;
                int g = (pixel >> 8) & 0xFF;
                int b = pixel & 0xFF;
                
                nv21[i] = (byte) ((66 * r + 129 * g + 25 * b + 128) >> 8 + 16);
            }
            
            // UV values
            int uvStart = width * height;
            for (int i = uvStart; i < frameSize; i++) {
                nv21[i] = (byte) 128;
            }
            
            return nv21;
        }

        public VideoFrame getNextFrame() {
            if (videoFrames.isEmpty()) {
                return null;
            }
            
            if (currentFrameIndex >= videoFrames.size()) {
                if (loopEnabled) {
                    currentFrameIndex = 0;
                } else {
                    return null;
                }
            }
            
            byte[] frameData = videoFrames.get(currentFrameIndex);
            currentFrameIndex++;
            
            return new VideoFrame(frameData, width, height);
        }

        public void setLoopEnabled(boolean enabled) {
            this.loopEnabled = enabled;
        }
    }
}
