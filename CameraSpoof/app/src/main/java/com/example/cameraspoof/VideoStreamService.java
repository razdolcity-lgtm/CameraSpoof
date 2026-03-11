package com.example.cameraspoof;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.MediaMetadataRetriever;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import java.io.File;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.List;
import java.util.ArrayList;

public class VideoStreamService {
    private static final String TAG = "VideoStreamService";
    private static final int FRAME_RATE = 30;
    private static final int FRAME_DELAY_MS = 1000 / FRAME_RATE;
    private static final int TARGET_WIDTH = 1920;
    private static final int TARGET_HEIGHT = 1080;

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
                    VideoFrame frame = frameProvider.getNextFrame();

                    if (frame != null) {
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
        private byte[] singleFrameBuffer;        // ⭐ ОДИН буфер на всё
        private int currentFrameIndex = 0;
        private boolean loopEnabled = true;
        private boolean useVideoSource = false;
        private long videoFrameCount = 0;

        // Для тестового паттерна
        private int testPatternIndex = 0;

        public VideoFrameProvider() {
            // Выделяем память ОДИН РАЗ на всё приложение
            int frameSize = TARGET_WIDTH * TARGET_HEIGHT * 3 / 2;
            singleFrameBuffer = new byte[frameSize];
            Log.d(TAG, "Allocated single frame buffer: " + frameSize + " bytes");
        }

        public void loadVideo(String videoPath) {
            try {
                if (retriever != null) {
                    retriever.release();
                }

                retriever = new MediaMetadataRetriever();
                retriever.setDataSource(videoPath);

                // Проверяем, что видео открылось
                String durationStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION);
                if (durationStr != null) {
                    long duration = Long.parseLong(durationStr);
                    videoFrameCount = (duration * FRAME_RATE) / 1000;
                    useVideoSource = true;
                    currentFrameIndex = 0;
                    Log.d(TAG, "Video loaded: " + videoPath + ", frames: " + videoFrameCount);
                } else {
                    throw new Exception("Could not get video duration");
                }

            } catch (Exception e) {
                Log.e(TAG, "Error loading video: " + videoPath, e);
                useVideoSource = false;
                // Оставляем тестовый паттерн
            }
        }

        public VideoFrame getNextFrame() {
            if (useVideoSource && retriever != null) {
                // Режим видеофайла
                try {
                    long timeUs = (currentFrameIndex * 1000000L) / FRAME_RATE;
                    Bitmap bitmap = retriever.getFrameAtTime(timeUs);

                    if (bitmap != null) {
                        // Конвертируем прямо в существующий буфер
                        bitmapToNV21(bitmap, singleFrameBuffer);
                        bitmap.recycle(); // сразу освобождаем

                        currentFrameIndex++;
                        if (currentFrameIndex >= videoFrameCount) {
                            currentFrameIndex = loopEnabled ? 0 : (int)(videoFrameCount - 1);
                        }

                        return new VideoFrame(singleFrameBuffer, TARGET_WIDTH, TARGET_HEIGHT);
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Error getting video frame", e);
                    useVideoSource = false; // падаем на тестовый паттерн
                }
            }

            // Режим тестового паттерна (резервный)
            generateTestPattern(singleFrameBuffer, testPatternIndex);
            testPatternIndex = (testPatternIndex + 1) % 30;

            return new VideoFrame(singleFrameBuffer, TARGET_WIDTH, TARGET_HEIGHT);
        }

        private void bitmapToNV21(Bitmap bitmap, byte[] outputBuffer) {
            // Масштабируем если нужно
            Bitmap source = bitmap;
            if (bitmap.getWidth() != TARGET_WIDTH || bitmap.getHeight() != TARGET_HEIGHT) {
                source = Bitmap.createScaledBitmap(bitmap, TARGET_WIDTH, TARGET_HEIGHT, true);
            }

            int[] pixels = new int[TARGET_WIDTH * TARGET_HEIGHT];
            source.getPixels(pixels, 0, TARGET_WIDTH, 0, 0, TARGET_WIDTH, TARGET_HEIGHT);

            // Конвертация RGB -> NV21
            for (int i = 0; i < TARGET_WIDTH * TARGET_HEIGHT; i++) {
                int pixel = pixels[i];
                int r = (pixel >> 16) & 0xFF;
                int g = (pixel >> 8) & 0xFF;
                int b = pixel & 0xFF;

                // Y = 0.299R + 0.587G + 0.114B
                int y = ( (66 * r + 129 * g + 25 * b + 128) >> 8 ) + 16;
                outputBuffer[i] = (byte) Math.max(0, Math.min(255, y));
            }

            // UV plane (просто серый)
            int uvStart = TARGET_WIDTH * TARGET_HEIGHT;
            for (int i = uvStart; i < outputBuffer.length; i++) {
                outputBuffer[i] = (byte) 128;
            }

            if (source != bitmap) {
                source.recycle(); // освобождаем временный bitmap
            }
        }

        private void generateTestPattern(byte[] buffer, int frameNum) {
            int frameSize = TARGET_WIDTH * TARGET_HEIGHT;

            // Y plane
            for (int y = 0; y < TARGET_HEIGHT; y++) {
                for (int x = 0; x < TARGET_WIDTH; x++) {
                    int index = y * TARGET_WIDTH + x;
                    int value = (x + y + frameNum * 10) % 256;
                    buffer[index] = (byte) value;
                }
            }

            // UV plane (серый)
            for (int i = frameSize; i < buffer.length; i++) {
                buffer[i] = (byte) 128;
            }
        }

        public void setLoopEnabled(boolean enabled) {
            this.loopEnabled = enabled;
        }
    }
}