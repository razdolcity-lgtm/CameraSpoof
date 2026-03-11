package com.example.cameraspoof;

import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraCaptureSession;
import android.content.Context;
import android.util.Log;
import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.callbacks.XC_LoadPackage;
import de.robv.android.xposed.XposedHelpers;
import java.lang.reflect.Method;
import android.os.Handler;

public class CameraHookModule implements IXposedHookLoadPackage {
    private static final String TAG = "CameraSpoof";
    private VirtualCameraManager virtualCameraManager;

    @Override
    public void handleLoadPackage(XC_LoadPackage.LoadPackageParam lpparam) throws Throwable {
        // Hook all apps except our own
        if (!lpparam.packageName.equals("com.example.cameraspoof")) {
            Log.d(TAG, "Hooking camera for package: " + lpparam.packageName);
            
            // Initialize virtual camera manager
            //virtualCameraManager = new VirtualCameraManager();
            
            // Hook CameraManager.openCamera
            hookCameraManager(lpparam.classLoader);
            
            // Hook Camera2 API methods
           // hookCameraDevice(lpparam.classLoader);
            
            // Hook legacy Camera API
            hookLegacyCamera(lpparam.classLoader);
        }
    }

    private void hookCameraManager(ClassLoader classLoader) {
        try {
            // Hook CameraManager.openCamera
            XposedHelpers.findAndHookMethod(
                "android.hardware.camera2.CameraManager",
                classLoader,
                "openCamera",
                String.class,
                "android.hardware.camera2.CameraDevice$StateCallback",
                android.os.Handler.class,
                new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                        String cameraId = (String) param.args[0];
                        Log.d(TAG, "Intercepted camera open request for camera: " + cameraId);

                        // Replace with virtual camera
                        param.args[0] = getManager().getVirtualCameraId();
                        Log.d(TAG, "Redirected to virtual camera: " + param.args[0]);
                    }
                }
            );

            // Hook CameraManager.getCameraIdList
            XposedHelpers.findAndHookMethod(
                "android.hardware.camera2.CameraManager",
                classLoader,
                "getCameraIdList",
                new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        String[] cameraIds = (String[]) param.getResult();
                        String[] modifiedIds = getManager().injectVirtualCamera(cameraIds);
                        param.setResult(modifiedIds);
                        Log.d(TAG, "Injected virtual camera into camera list");
                    }
                }
            );

        } catch (Exception e) {
            Log.e(TAG, "Failed to hook CameraManager", e);
        }
    }

//    private void hookCameraDevice(ClassLoader classLoader) {
//        try {
//            // Hook CameraDevice.createCaptureSession
//            XposedHelpers.findAndHookMethod(
//                "android.hardware.camera2.CameraDevice",
//                classLoader,
//                "createCaptureSession",
//                java.util.List.class,
//                "android.hardware.camera2.CameraCaptureSession$StateCallback",
//                android.os.Handler.class,
//                new XC_MethodHook() {
//                    @Override
//                    protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
//                        Log.d(TAG, "Intercepted capture session creation");
//
//                        // Replace surfaces with virtual camera surfaces
//                        param.args[0] = getManager().getVirtualSurfaces((java.util.List) param.args[0]);
//                    }
//                }
//            );
//
//        } catch (Exception e) {
//            Log.e(TAG, "Failed to hook CameraDevice", e);
//        }
//    }

    //новое
//private void hookCameraDevice(ClassLoader classLoader) {
//    try {
//        XposedHelpers.findAndHookMethod(
//                android.hardware.camera2.CameraDevice.class,
//                "createCaptureSession",
//                java.util.List.class,
//                android.hardware.camera2.CameraCaptureSession.StateCallback.class,
//                android.os.Handler.class,
//                new XC_MethodHook() {
//                    @Override
//                    protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
//                        // Ничего не делаем, просто логируем
//                        Log.d(TAG, "createCaptureSession called");
//                    }
//                }
//        );
//        Log.d(TAG, "CameraDevice hook registered");
//    } catch (Exception e) {
//        Log.e(TAG, "Failed to hook CameraDevice", e);
//    }
//}

    private void hookLegacyCamera(ClassLoader classLoader) {
        try {
            // Hook Camera.open()
            XposedHelpers.findAndHookMethod(
                "android.hardware.Camera",
                classLoader,
                "open",
                int.class,
                new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                        Log.d(TAG, "Intercepted legacy camera open request");
                        
                        // Always return virtual camera
                        param.setResult(getManager().getLegacyCamera());
                    }
                }
            );

            // Hook Camera.open() without parameters
            XposedHelpers.findAndHookMethod(
                "android.hardware.Camera",
                classLoader,
                "open",
                new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                        Log.d(TAG, "Intercepted legacy camera open request (default)");
                        param.setResult(getManager().getLegacyCamera());
                    }
                }
            );

        } catch (Exception e) {
            Log.e(TAG, "Failed to hook legacy Camera", e);
        }
    }
    private VirtualCameraManager getManager() {
        if (virtualCameraManager == null) {
            virtualCameraManager = new VirtualCameraManager();
        }
        return virtualCameraManager;
    }
}
