# Camera Spoof - Automatic Virtual Camera

A powerful Android app that automatically intercepts camera requests from any application and replaces them with pre-recorded video. Requires root access and Xposed Framework.

## Features

- **Automatic Detection**: Intercepts camera requests from any app automatically
- **Universal Compatibility**: Works with Camera2 API and legacy Camera API
- **Real-time Spoofing**: Streams pre-recorded video as live camera feed
- **Root Required**: Uses Xposed Framework for system-level hooking
- **Video Loop**: Continuous playback of pre-recorded content
- **System-wide**: All apps using camera will receive the virtual feed

## Requirements

- **Root Access**: Device must be rooted
- **Xposed Framework**: Must be installed and activated
- **Android 7.0+**: API level 24 or higher
- **Storage Permissions**: To load video files

## Installation

### Prerequisites

1. **Root your device** using Magisk, SuperSU, or other root method
2. **Install Xposed Framework** from [Xposed Installer](https://github.com/rovo89/XposedInstaller/releases)
3. **Reboot device** after installing Xposed
4. **Enable this module** in Xposed Installer

### App Installation

1. Clone this repository
2. Build the APK using Android Studio
3. Install the APK on your rooted device
4. Grant root permissions when prompted
5. Enable the module in Xposed Installer
6. Reboot device to activate hooks

## Usage

1. **Open Camera Spoof app**
2. **Tap "Enable Camera Spoofing"**
3. **Select video file** (optional - uses test pattern by default)
4. **Enable/disable video looping** with the switch
5. **All camera apps** will now show your pre-recorded video

## How It Works

### System-Level Hooking

The app uses Xposed Framework to hook into Android's camera system:

1. **CameraManager.openCamera()** - Intercepts camera opening requests
2. **CameraManager.getCameraIdList()** - Injects virtual camera into device list
3. **CameraDevice.createCaptureSession()** - Replaces surfaces with virtual surfaces
4. **Legacy Camera.open()** - Hooks old Camera API for compatibility

### Virtual Camera System

- **VirtualCameraManager**: Coordinates all virtual camera operations
- **VideoStreamService**: Handles video playback and frame distribution
- **VirtualCameraDevice**: Creates virtual surfaces for Camera2 API
- **VirtualLegacyCamera**: Provides compatibility with old Camera API

### Frame Processing

1. Video frames are extracted from pre-recorded files
2. Frames are converted to NV21 format (YUV420)
3. Frames are distributed to virtual surfaces
4. Apps receive frames as if from real camera

## Technical Details

### Hooked Methods

```java
// Camera2 API hooks
CameraManager.openCamera(String cameraId, StateCallback callback, Handler handler)
CameraManager.getCameraIdList()
CameraDevice.createCaptureSession(List<Surface> surfaces, StateCallback callback, Handler handler)

// Legacy Camera API hooks  
Camera.open(int cameraId)
Camera.open()
```

### Virtual Camera ID

The virtual camera appears as `"virtual_camera_0"` and is always placed first in the camera list, ensuring apps select it by default.

### Frame Format

- **Format**: NV21 (YUV420)
- **Resolution**: 1920x1080 (configurable)
- **Frame Rate**: 30 FPS
- **Encoding**: Hardware-accelerated when available

## Security & Privacy

- **Root Required**: This app needs system-level access to function
- **Xposed Hooks**: Uses legitimate hooking framework
- **No Network**: All processing happens locally
- **User Control**: Can be enabled/disabled at any time

## Troubleshooting

### Module Not Working

1. **Check Xposed Installation**: Ensure Xposed is properly installed
2. **Verify Root**: Confirm device is rooted and app has root access
3. **Reboot Device**: Some hooks require reboot to activate
4. **Check Logs**: Use Xposed logs to debug issues

### Apps Still Use Real Camera

1. **Module Not Active**: Ensure module is enabled in Xposed
2. **Hook Failed**: Check for conflicts with other Xposed modules
3. **App Protection**: Some apps may have anti-hook protections

### Performance Issues

1. **Video Resolution**: Lower resolution videos perform better
2. **Hardware Acceleration**: Ensure device supports hardware encoding
3. **Background Apps**: Close unnecessary apps to free resources

## Development

### Building from Source

```bash
# Clone repository
git clone https://github.com/your-repo/camera-spoof.git
cd camera-spoof

# Build with Android Studio
# Or use gradle:
./gradlew assembleDebug
```

### Key Classes

- `CameraHookModule` - Main Xposed hook implementation
- `VirtualCameraManager` - Virtual camera coordination
- `VideoStreamService` - Video playback and distribution
- `VirtualCameraDevice` - Camera2 API virtual device
- `VirtualLegacyCamera` - Legacy Camera API compatibility

## Legal & Ethical

This tool is for educational and legitimate purposes only:
- **Privacy Testing**: Test app security and privacy protections
- **Development**: Debug camera-dependent applications
- **Accessibility**: Assist users who cannot use real cameras
- **Research**: Study Android camera system behavior

**Do not use for**: Fraud, deception, or any illegal activities

## Contributing

Contributions welcome! Please ensure:
- Code follows Android development best practices
- Hooks are properly tested on various Android versions
- Documentation is updated for new features
- Security implications are considered

## License

MIT License - see LICENSE file for details

## Support

For issues and questions:
1. Check the troubleshooting section
2. Review Xposed logs for errors
3. Create an issue with device details and logs
4. Include Android version and Xposed version
