# Appium Mobile Automation Setup Guide (Java + Linux)

This guide configures a Linux machine to run this project's Appium tests. Android testing can run entirely on Linux with an emulator or connected device. Apple's XCUITest driver requires macOS, so iOS sessions need an Appium server running on a Mac; the Java test project can still run on Linux and connect to that server.

Commands below use Ubuntu/Debian-style package management. On another distribution, install the equivalent packages using its package manager.

## 1. What You Need

| Tool | Purpose |
|---|---|
| JDK 14 or newer | Compiles this project, which targets Java 14. JDK 17 or 21 is recommended. |
| Maven | Builds the project and runs TestNG tests. |
| Node.js LTS and npm | Runs and installs Appium. |
| Appium server | Receives WebDriver commands from the Java client. |
| Android Studio / Android SDK | Supplies `adb`, the emulator, and Android system images. |
| UiAutomator2 driver | Appium's Android automation driver. |
| Android emulator or USB-debuggable device | Android device under test. |

For iOS, use a Mac with Xcode, an iOS simulator or device, and Appium's XCUITest driver. Linux cannot run Xcode or host XCUITest sessions.

## 2. How Android Sessions Connect

```mermaid
flowchart LR
    A[Java test and TestNG on Linux] --> B[Appium Java Client]
    B -->|HTTP / WebDriver| C[Appium server]
    C --> D[UiAutomator2 driver]
    D -->|adb and UiAutomator2| E[Android emulator or device]
```

The Java client and Appium server are separate processes. Start Appium before running Maven tests. The server uses `adb` and UiAutomator2 to install and control the app on the Android device.

## 3. General Setup

### 3.1 Install Java and Maven

Install a JDK and Maven from your distribution's repositories. For Ubuntu/Debian:

```bash
sudo apt update
sudo apt install openjdk-17-jdk maven
```

Verify both tools:

```bash
java -version
javac -version
mvn -version
```

This project sets Java source and target to 14, so use JDK 14 or later. JDK 17 is a suitable default.

### 3.2 Install Node.js and Appium

Install a current Node.js LTS release and npm using the official Node.js instructions for Linux, then verify:

```bash
node --version
npm --version
```

Install Appium globally:

```bash
npm install --global appium
appium --version
```

If npm reports a permissions error, use a user-owned npm prefix or a Node version manager rather than running npm with `sudo`.

Install the Android driver and verify it is present:

```bash
appium driver install uiautomator2
appium driver list --installed
```

The Device Farm plugin is optional for this project's single-device setup. If you install and use it, start Appium with the platform argument; for Android:

```bash
appium plugin install --source=npm appium-device-farm
appium --use-plugins=device-farm --plugin-device-farm-platform=android
```

The `--plugin-device-farm-platform` argument is required when starting that plugin. Without it, the plugin may not scan for Android devices. If you do not use Device Farm, start the server with plain `appium`.

## 4. Android Setup

### 4.1 Install Android Studio and SDK Components

1. Download Android Studio for Linux from [developer.android.com/studio](https://developer.android.com/studio) and install it using its Linux installation instructions.
2. Open Android Studio and use **Tools > SDK Manager** to install:
   - Android SDK Platform for the API level you plan to test.
   - Android SDK Platform-Tools (includes `adb`).
   - Android SDK Build-Tools.
   - Android Emulator.
   - Android SDK Command-line Tools (latest).
3. Note the SDK location shown in SDK Manager. The common default is `$HOME/Android/Sdk`.

Set the SDK environment variables in `~/.bashrc` (change the path if your SDK is elsewhere):

```bash
export ANDROID_HOME="$HOME/Android/Sdk"
export ANDROID_SDK_ROOT="$ANDROID_HOME"
export PATH="$PATH:$ANDROID_HOME/platform-tools:$ANDROID_HOME/emulator:$ANDROID_HOME/cmdline-tools/latest/bin"
```

Reload the shell configuration and accept SDK licenses:

```bash
source ~/.bashrc
sdkmanager --licenses
adb version
emulator -version
```

### 4.2 Configure Emulator Acceleration

Android Emulator performance is best with hardware virtualization enabled in the machine's BIOS/UEFI and KVM available to your user. On Ubuntu/Debian:

```bash
sudo apt install qemu-kvm
sudo usermod -aG kvm "$USER"
```

Log out and back in so the group change applies, then check KVM access:

```bash
ls -l /dev/kvm
emulator -accel-check
```

If `/dev/kvm` is missing, enable Intel VT-x or AMD-V in BIOS/UEFI. Virtual machines may also need nested virtualization enabled by their host.

### 4.3 Create and Start an Android Virtual Device

1. In Android Studio, open **Tools > Device Manager**.
2. Create a device and select a downloaded system image. An x86_64 image is appropriate for most Intel/AMD Linux hosts.
3. Start the emulator and confirm Android reports it as ready:

```bash
adb devices
```

Expected output includes a device such as `emulator-5554` with status `device`.

For a physical device, enable **Developer options > USB debugging**, connect it over USB, and accept the authorization prompt. On Ubuntu/Debian, install Android's udev rules package if available:

```bash
sudo apt install android-sdk-platform-tools-common
```

Reconnect the device and verify it appears in `adb devices`. If the state is `unauthorized`, unlock the device and accept its RSA prompt.

### 4.4 Configure This Project

The repository includes `apps/ApiDemos-release.apk` and `runtimeConfig.jsonc` is configured for Android by default. Its Android app path is relative to the project root, so run Maven commands from the repository root. The relevant settings are:

```jsonc
"execution": {
  "platform": "android"
},
"appium": {
  "serverUrl": "http://127.0.0.1:4723",
  "android": {
    "app": "apps/ApiDemos-release.apk",
    "appPackage": "io.appium.android.apis"
  }
}
```

Keep the existing surrounding settings in the file; this snippet shows the values used for an Android run. To pin a run to a specific device, use the serial shown by `adb devices`:

```bash
mvn test -Dudid=emulator-5554
```

The `-Dplatform=...` system property overrides the platform in `runtimeConfig.jsonc`; `-Dudid=...` overrides the configured Android UDID.

### 4.5 Run the Android Test

Use separate terminals:

1. Start the emulator or connect the physical device and verify it with `adb devices`.
2. Start Appium and leave it running:

```bash
appium
```

3. From the project root, run the suite:

```bash
mvn clean test -Dplatform=android
```

TestNG runs the class registered in `testng.xml` (`tests.FirstAndroidTest`). The test opens the ApiDemos Accessibility menu item. A successful run ends with `BUILD SUCCESS`; Surefire reports are written to `target/surefire-reports/`, and the framework's HTML report is written under `test-output/`.

## 5. Run iOS Tests from Linux

Linux can run and compile the Java client, but the Appium server and XCUITest driver must run on macOS. Configure a Mac with Xcode, the XCUITest driver, and an available simulator or device, then start its Appium server:

```bash
appium --use-plugins=device-farm --plugin-device-farm-platform=ios
```

The Device Farm plugin is optional; without it, start plain `appium` instead. In `runtimeConfig.jsonc`, set `execution.platform` to `ios`, set `appium.serverUrl` to the Mac's reachable address, and configure the iOS app. The app path must be valid on the Mac running Appium because that server installs the app. Ensure network access to the Appium port (default `4723`) is allowed.

Then run the Java suite from Linux:

```bash
mvn clean test -Dplatform=ios
```

The test project and Maven dependencies remain on Linux; the Mac hosts the iOS automation stack and device.

## 6. Quick Troubleshooting

| Symptom | Check |
|---|---|
| `adb: command not found` | Confirm `ANDROID_HOME` and the `platform-tools` entry in `PATH`; reload the shell. |
| Emulator is very slow or reports no acceleration | Check BIOS/UEFI virtualization, `/dev/kvm` permissions, and `emulator -accel-check`. |
| Device does not appear in `adb devices` | Check USB debugging and cable/USB mode; install udev rules and reauthorize the device. |
| Appium says the driver is missing | Run `appium driver list --installed` and install `uiautomator2` for Android. |
| Appium cannot create a session | Confirm the server is running at the configured URL and the device state is `device`. |
| App path cannot be found | Run Maven from the repository root and confirm the configured app path exists. |
| iOS session fails from Linux | Confirm Appium and XCUITest run on a Mac, the Mac URL is reachable, and the app path is valid on that Mac. |

## 7. Setup Checklist

### General

- [ ] JDK 14 or newer installed; `java -version` and `javac -version` work.
- [ ] Maven installed; `mvn -version` works.
- [ ] Node.js LTS and npm installed; `node --version` and `npm --version` work.
- [ ] Appium installed and starts on port 4723.

### Android

- [ ] Android SDK Platform-Tools, Emulator, and a system image installed.
- [ ] `ANDROID_HOME`/`ANDROID_SDK_ROOT` and `PATH` configured.
- [ ] KVM available for emulator acceleration, or a physical device authorized in `adb devices`.
- [ ] UiAutomator2 driver installed.
- [ ] `runtimeConfig.jsonc` points to the APK and Appium server in use.
- [ ] `mvn clean test -Dplatform=android` reports `BUILD SUCCESS`.

### iOS from Linux

- [ ] Appium server and XCUITest driver installed on a Mac with Xcode.
- [ ] Linux can reach the Mac's Appium URL.
- [ ] iOS app path is valid on the Mac hosting Appium.
- [ ] `runtimeConfig.jsonc` selects iOS and points to the Mac server.

## Reference Links

- Appium: [appium.io/docs](https://appium.io/docs/en/latest/)
- Android Studio: [developer.android.com/studio](https://developer.android.com/studio)
- Android Emulator: [developer.android.com/studio/run/emulator](https://developer.android.com/studio/run/emulator)
- Node.js: [nodejs.org](https://nodejs.org/)
- ApiDemos sample app: [github.com/appium/android-apidemos/releases](https://github.com/appium/android-apidemos/releases)
- Appium XCUITest driver: [github.com/appium/appium-xcuitest-driver](https://github.com/appium/appium-xcuitest-driver)