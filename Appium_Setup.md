# Appium Mobile Automation Setup Guide (Java + Windows)
 
 This guide takes a new machine from no mobile-automation setup to running an Appium test written in Java. It is organized in three parts:
 
 1. **General setup** — tools and steps required regardless of target platform.
 2. **Android-specific setup** — everything additional needed to test Android apps. This works fully on Windows.
 3. **iOS-specific setup** — everything additional needed to test iOS apps. Apple's XCUITest driver only runs on **macOS**, so the Appium server and Xcode toolchain for iOS must live on a Mac (local Mac, cloud Mac CI, or a device-farm service). The Java test code itself can still be authored/compiled on Windows.
 
 ## 1. What Each Tool Does
 
 ### General (both platforms)
 
 | Tool | Purpose |
 |---|---|
 | JDK 11 or newer | Compiles and runs Java tests. |
 | Maven | Downloads Java dependencies and runs the test suite. |
 | Node.js + npm | Installs and runs the Appium server. |
 | Appium server | Receives WebDriver HTTP commands from the Java test. |
 | Appium Java Client | Maven dependency used by Java code to create Appium sessions and issue commands. |
 | TestNG | Java test framework that runs `@BeforeClass`, `@Test`, and `@AfterClass`. |
 
 ### Android-specific
 
 | Tool | Purpose |
 |---|---|
 | Android Studio / Android SDK | Installs Android SDK tools, `adb`, emulator, and Android system images. |
 | Android Emulator or a real device | The Android device under test. |
 | UiAutomator2 driver | Appium's Android-specific driver; it controls Android using `adb` and UiAutomator2. |
 
 ### iOS-specific
 
 | Tool | Purpose |
 |---|---|
 | macOS + Xcode | Required to run the XCUITest driver and build/sign WebDriverAgent. Appium's iOS automation cannot run on Windows or Linux. |
 | iOS Simulator or a real device | The iOS device under test. |
 | XCUITest driver | Appium's iOS-specific driver; it controls iOS using Apple's XCUITest framework via WebDriverAgent. |
 | WebDriverAgent (WDA) | A test-runner app Appium installs on the simulator/device to translate WebDriver commands into XCUITest calls. |
 | libimobiledevice / ios-deploy | Command-line tools Appium/Node use to communicate with real iOS devices over USB. |
 
 ## 2. How the Parts Connect
 
 ### Android
 
 ```mermaid
 flowchart LR
     A[Java test and TestNG] --> B[Appium Java Client]
     B -->|HTTP / WebDriver| C[Appium server]
     C --> D[UiAutomator2 driver]
     D -->|adb and UiAutomator2| E[Android emulator or real device]
 ```
 
 - `UiAutomator2Options` in Java is only a configuration object. It tells Appium which device, app, and Android automation driver to use.
 - The `appium` command starts the server process that receives those requests.
 - `appium driver install uiautomator2` installs the Android automation implementation into that server. It is different from the Java dependency in `pom.xml`.
 - `adb` is Google's Android Debug Bridge. Appium uses it to locate devices, install apps, and communicate with Android.
 
 ### iOS
 
 ```mermaid
 flowchart LR
     A[Java test and TestNG] --> B[Appium Java Client]
     B -->|HTTP / WebDriver| C[Appium server on macOS]
     C --> D[XCUITest driver]
     D -->|installs and drives| F[WebDriverAgent]
     F -->|XCUITest| E[iOS simulator or real device]
 ```
 
 - `XCUITestOptions` in Java is only a configuration object. It tells Appium which device, app, and iOS automation driver to use.
 - The `appium` command must run on a Mac for iOS sessions, because it shells out to Xcode tooling (`xcodebuild`, `xcrun simctl`, etc.).
 - `appium driver install xcuitest` installs the iOS automation implementation into the server. It is different from the Java dependency in `pom.xml`.
 - WebDriverAgent is built and code-signed by Appium the first time it runs against a given device/simulator; on real devices this requires a valid Apple signing identity.
 
 ## 3. General Setup (Applies to Both Platforms)
 
 ### 3.1 Install the JDK
 
 1. Install a supported JDK, preferably JDK 17 or JDK 21 for a new setup. JDK 11 also works with this project.
 2. Set `JAVA_HOME` to the JDK installation directory, not its `bin` directory.
 3. Add `%JAVA_HOME%\bin` to the Windows `Path` variable (on macOS, add `$JAVA_HOME/bin` to your shell profile).
 4. Open a new terminal and verify:
 
 ```powershell
 java -version
 javac -version
 ```
 
 ### 3.2 Install Node.js and Appium
 
 1. Install the current LTS version of Node.js from https://nodejs.org.
 2. Open a new terminal and verify Node.js and npm:
 
 ```powershell
 node -v
 npm -v
 ```
 
 3. Install Appium globally:
 
 ```powershell
 npm install -g appium
 appium -v
 ```
 
 4. Install the Device Farm plugin. It auto-detects connected devices/emulators/simulators and routes sessions to a free one — this works transparently whether you have one device or several, so it's worth installing now rather than only when you need parallel runs:
 
 ```powershell
 appium plugin install --source=npm appium-device-farm
 ```
 
 Start Appium with the plugin enabled (use this instead of plain `appium` from here on). `--plugin-device-farm-platform` is a **required** plugin argument — without it the plugin defaults to scanning no platform at all and reports "No devices found" even with emulators/simulators running; use `both` to support Android and iOS from the same server, or `android`/`ios` to restrict it to one:
 
 ```powershell
 appium --use-plugins=device-farm --plugin-device-farm-platform=both
 ```
 
 5. Optional diagnostic command (run `--android` on the Android machine, `--ios` on the Mac):
 
 ```powershell
 npm install -g @appium/doctor
 appium-doctor --android
 appium-doctor --ios
 ```
 
 Platform-specific automation drivers (`uiautomator2`, `xcuitest`) are installed in the Android-specific and iOS-specific sections below.
 
 ### 3.3 Install Maven
 
 1. Download the binary ZIP from https://maven.apache.org/download.cgi.
 2. Extract it, for example to `C:\Tools\apache-maven-<version>`.
 3. Set `MAVEN_HOME` to the Maven root folder, the folder containing `bin`, `conf`, and `lib`.
 4. Add `%MAVEN_HOME%\bin` to `Path`.
 5. Close and reopen your terminal and verify:
 
 ```powershell
 mvn -v
 ```
 
 Android Studio can also run Maven projects using its bundled Maven integration. A separate Maven installation is still useful for terminal and CI runs.
 
 ### 3.4 Create the Java Maven Project
 
 Use this structure. `FirstIOSTest.java` is only relevant once you also complete the iOS-specific setup below.
 
 ```text
 appium-framework/
 |-- apps/
 |-- pom.xml
 |-- testng.xml
 `-- src/
     `-- test/
         `-- java/
             `-- tests/
                 |-- FirstAndroidTest.java
                 `-- FirstIOSTest.java
 ```
 
 Use this `pom.xml`. Do not add an explicit Selenium dependency: Appium Java Client supplies compatible Selenium modules transitively. The same dependencies support both Android and iOS.
 
 ```xml
 <project xmlns="http://maven.apache.org/POM/4.0.0"
          xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
          xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
   <modelVersion>4.0.0</modelVersion>
 
   <groupId>com.example</groupId>
   <artifactId>appium-framework</artifactId>
   <version>1.0-SNAPSHOT</version>
 
   <properties>
     <maven.compiler.source>11</maven.compiler.source>
     <maven.compiler.target>11</maven.compiler.target>
     <project.build.sourceEncoding>UTF-8</project.build.sourceEncoding>
   </properties>
 
   <dependencies>
     <dependency>
       <groupId>io.appium</groupId>
       <artifactId>java-client</artifactId>
       <version>10.1.1</version>
     </dependency>
     <dependency>
       <groupId>org.testng</groupId>
       <artifactId>testng</artifactId>
       <version>7.10.2</version>
       <scope>test</scope>
     </dependency>
   </dependencies>
 
   <build>
     <plugins>
       <plugin>
         <groupId>org.apache.maven.plugins</groupId>
         <artifactId>maven-surefire-plugin</artifactId>
         <version>3.3.1</version>
         <configuration>
           <suiteXmlFiles>
             <suiteXmlFile>testng.xml</suiteXmlFile>
           </suiteXmlFiles>
         </configuration>
       </plugin>
     </plugins>
   </build>
 </project>
 ```

Create `testng.xml`. Add a second `<test>` block for iOS once `FirstIOSTest.java` exists:
 
 ```xml
 <!DOCTYPE suite SYSTEM "https://testng.org/testng-1.0.dtd">
 <suite name="AppiumSuite">
   <test name="AndroidTests">
     <classes>
       <class name="tests.FirstAndroidTest"/>
     </classes>
   </test>
 </suite>
 ```
 
 ## 4. Android-Specific Setup
 
 ### 4.1 Install Android Studio and the Android SDK
 
 1. Download the current stable Android Studio installer from https://developer.android.com/studio.
 2. Use the Standard installation option.
 3. In Android Studio, open **Tools > SDK Manager** and confirm these components are installed:
    - An Android SDK Platform/system image needed by your tests. API 31 (Android 12) is a good lightweight learning choice.
    - Android SDK Platform-Tools. This contains `adb`.
    - Android SDK Build-Tools.
    - Android Emulator.
    - Android SDK Command-line Tools (latest).
 4. Note the SDK location shown at the top of SDK Manager. A common Windows path is `C:\Users\<your-user>\AppData\Local\Android\Sdk`.
 
 ### 4.2 Create an Android Virtual Device
 
 1. Open **Tools > Device Manager** in Android Studio.
 2. Select **Create device** and choose a hardware profile, such as Pixel 6.
 3. Select a downloaded system image, such as API 31 x86_64.
 4. Finish and start the virtual device.
 5. Verify that Android can see it:
 
 ```powershell
 adb devices
 ```
 
 Expected example:
 
 ```text
 List of devices attached
 emulator-5554    device
 ```
 
 For a real device instead, enable **Developer options > USB debugging**, connect it over USB, approve the RSA dialog on the phone, and run `adb devices`.
 
 ### 4.3 Set Android Environment Variables
 
 Open **Edit environment variables for your account** in Windows. Add these user or system variables, replacing the example path with your real SDK location:
 
 | Variable | Example value |
 |---|---|
 | `ANDROID_HOME` | `C:\Users\<your-user>\AppData\Local\Android\Sdk` |
 | `ANDROID_SDK_ROOT` | Same value as `ANDROID_HOME` |
 
 Add these entries to `Path`:
 
 ```text
 %ANDROID_HOME%\platform-tools
 %ANDROID_HOME%\emulator
 %ANDROID_HOME%\cmdline-tools\latest\bin
 ```
 
 Close and reopen PowerShell and your IDE after changing environment variables. Verify:
 
 ```powershell
 echo $env:ANDROID_HOME
 adb version
 emulator -list-avds
 ```
 
 `appium-doctor` may report that the legacy `android` command is missing. Current Android SDK releases no longer include that deprecated command. If `ANDROID_HOME`, `adb`, and `emulator` are found, this warning does not block Android Appium testing.
 
 ### 4.4 Install the UiAutomator2 Driver
 
 ```powershell
 appium driver install uiautomator2
 appium driver list --installed
 ```
 
 Expected result includes `uiautomator2` as installed. Optional warnings from `appium-doctor` about `ffmpeg`, `opencv4nodejs`, `bundletool`, MJPEG, or GStreamer can be ignored for normal element-based tests. They are needed only for features such as image comparison, screen recording, App Bundles, or video streaming.
 
 ### 4.5 Get a Sample Android App
 
 For practice, download Appium's maintained ApiDemos test application:
 
 https://github.com/appium/android-apidemos/releases
 
 Download `ApiDemos-debug.apk` and save it in your project's `apps` directory, for example:
 
 ```text
 C:\Projects\appium-framework\apps\ApiDemos-debug.apk
 ```
 
 ### 4.6 Write the Android Test
 
 Update the device and APK paths to values that exist on your machine:
 
 ```java
 package tests;
 
 import io.appium.java_client.android.AndroidDriver;
 import io.appium.java_client.android.options.UiAutomator2Options;
 import org.openqa.selenium.By;
 import org.testng.Assert;
 import org.testng.annotations.AfterClass;
 import org.testng.annotations.BeforeClass;
 import org.testng.annotations.Test;
 
 import java.net.URL;
 import java.nio.file.Paths;
 
 public class FirstAndroidTest {
 
     private AndroidDriver driver;
 
     @BeforeClass
     public void setUp() throws Exception {
         UiAutomator2Options options = new UiAutomator2Options();
         options.setDeviceName("Pixel_6");
         options.setApp(Paths.get("C:\\Projects\\appium-framework\\apps\\ApiDemos-debug.apk").toString());
 
         driver = new AndroidDriver(new URL("http://127.0.0.1:4723"), options);
     }
 
     @Test
     public void appLaunchesAndShowsAccessibilityMenuItem() {
         Assert.assertTrue(
             driver.findElement(By.xpath("//*[@text='Accessibility']")).isDisplayed()
         );
     }
 
     @AfterClass
     public void tearDown() {
         if (driver != null) {
             driver.quit();
         }
     }
 }
 ```
 
 `deviceName` is an Appium label and can be a friendly name. To reliably select a particular device when multiple devices are connected, also set `options.setUdid("emulator-5554")`, using the ID printed by `adb devices`.
 
 ### 4.7 Run the Android Test Locally
 
 Use three terminals, in this order:
 
 1. Start the emulator from Android Studio Device Manager, then wait until this reports `device`:
 
 ```powershell
 adb devices
 ```
 
 2. Start Appium in a second terminal and leave it running (with the Device Farm plugin installed in section 3.2, this works the same for one device or several). Remember the required `--plugin-device-farm-platform` flag (see 3.2) — omitting it is why the plugin reports "No devices found" even when `adb devices` sees your emulators:
 
 ```powershell
 appium --use-plugins=device-farm --plugin-device-farm-platform=android
 ```
 
 Appium normally listens on `http://127.0.0.1:4723`. This is correct when tests and Appium run on the same machine.
 
 3. In the project folder, run the Maven test suite:
 
 ```powershell
 mvn clean test
 ```
 
 What happens during the run:
 
 1. Maven compiles the Java test and starts TestNG using `testng.xml`.
 2. TestNG runs `setUp()` because it has `@BeforeClass`.
 3. `new AndroidDriver(...)` sends a create-session HTTP request to Appium.
 4. Appium loads UiAutomator2, uses `adb`, installs the APK, and launches it on the emulator/device.
 5. TestNG runs the `@Test` method. `findElement(...)` asks Appium to search Android's UI hierarchy for `Accessibility`.
 6. The assertion passes when that visible element is found.
 7. TestNG runs `tearDown()`, and `driver.quit()` ends the Appium session.
 
 Expected final result:
 
 ```text
 Tests run: 1, Failures: 0, Errors: 0, Skipped: 0
 BUILD SUCCESS
 ```
 
 ## 5. iOS-Specific Setup (macOS Only)
 
 Everything in this section must run on a Mac. If you only have Windows, run the Appium server and Xcode tooling on a Mac elsewhere (a colleague's Mac, a cloud Mac such as MacStadium or GitHub Actions `macos-latest`, or a device-farm service like BrowserStack/Sauce Labs/LambdaTest) and point the Java test at that machine's Appium URL, the same way Section 6 describes for Android.
 
 ### 5.1 Install Xcode
 
 1. Install Xcode from the Mac App Store (latest stable release).
 2. Install the command-line tools:
 
 ```bash
 xcode-select --install
 sudo xcodebuild -license accept
 ```

 3. Open Xcode at least once so it can install additional components, and add any iOS platform runtimes you need under **Xcode > Settings > Platforms**.
 
 ### 5.2 Install the XCUITest Driver
 
 ```bash
 appium driver install xcuitest
 appium driver list --installed
 ```
 
 Expected result includes `xcuitest` as installed.
 
 ### 5.3 Simulators
 
 - List available simulators:
 
 ```bash
 xcrun simctl list devices
 ```
 
 - Simulators only run `.app` bundles (not `.ipa`), and require no Apple Developer account or code signing. This is the fastest way to get started with iOS.
 
 ### 5.4 Real Devices
 
 Real-device testing needs more setup than simulators:
 
 - An Apple Developer account (the free tier is enough for local/personal testing).
 - On the device: **Settings > Privacy & Security > Developer Mode** enabled (iOS 16+), and the "Trust This Computer" prompt accepted when connected over USB.
 - Code signing for WebDriverAgent: Appium builds and installs WDA on the device the first time it runs. Provide a signing team either as capabilities (`xcodeOrgId`, `xcodeSigningId`) or by opening WDA's Xcode project once and selecting your team manually.
 - Helper tools, installed via Homebrew:
 
 ```bash
 brew install libimobiledevice ios-deploy
 ```
 
 - Real apps must be built/signed for your device (`.ipa` or an installed `bundleId`); simulator `.app` builds will not install on hardware.
 
 ### 5.5 Get a Sample iOS App
 
 Use any `.app` bundle built for the Simulator, or an app you already build with Xcode. If you need a ready-made sample, Appium's own iOS `TestApp` project can be cloned and built with Xcode for the Simulator:
 
 https://github.com/appium/ios-test-app
 
 Save the resulting `.app` bundle in your project's `apps` directory, for example:
 
 ```text
 C:\Projects\appium-framework\apps\TestApp.app
 ```
 
 (this path only needs to exist on the Mac that runs the Appium server; it does not need to exist on Windows.)
 
 ### 5.6 Write the iOS Test
 
 ```java
 package tests;
 
 import io.appium.java_client.ios.IOSDriver;
 import io.appium.java_client.ios.options.XCUITestOptions;
 import org.openqa.selenium.By;
 import org.testng.Assert;
 import org.testng.annotations.AfterClass;
 import org.testng.annotations.BeforeClass;
 import org.testng.annotations.Test;
 
 import java.net.URL;
 import java.nio.file.Paths;
 
 public class FirstIOSTest {
 
     private IOSDriver driver;
 
     @BeforeClass
     public void setUp() throws Exception {
         XCUITestOptions options = new XCUITestOptions();
         options.setDeviceName("iPhone 15");
         options.setPlatformVersion("17.5");
         options.setApp(Paths.get("/Users/<mac-user>/appium-framework/apps/TestApp.app").toString());
 
         driver = new IOSDriver(new URL("http://127.0.0.1:4723"), options);
     }
 
     @Test
     public void appLaunches() {
         Assert.assertTrue(
             driver.findElement(By.accessibilityId("SomeElement")).isDisplayed()
         );
     }
 
     @AfterClass
     public void tearDown() {
         if (driver != null) {
             driver.quit();
         }
     }
 }
 ```
 
 Replace `"SomeElement"` with a real accessibility id from your app (use Appium Inspector to find it). Register the class in `testng.xml` in its own `<test>` block, mirroring the Android one.
 
 ### 5.7 Run the iOS Test Locally (on the Mac)
 
 1. Start (or let Appium auto-start) a simulator, or connect/trust a real device.
 2. Start Appium in a terminal and leave it running (the `--plugin-device-farm-platform` flag is required, see 3.2):
 
 ```bash
 appium --use-plugins=device-farm --plugin-device-farm-platform=ios
 ```
 
 3. From the project folder, run:
 
 ```bash
 mvn clean test
 ```
 
 Appium loads the XCUITest driver, builds/installs WebDriverAgent on the target, installs the app, and drives it via WDA — the iOS equivalent of steps 3-6 in the Android run described above.
 
 ## 6. Run Tests from a Different Machine
 
 The Appium server (and emulator/simulator/device) may run on Machine A while Maven tests run on Machine B.
 
 - Machine A needs Node.js and Appium, plus:
   - For Android: Android SDK, device/emulator, UiAutomator2 driver.
   - For iOS: macOS, Xcode, XCUITest driver — Machine A must be a Mac.
 - Machine B needs JDK, Maven, the Java test project, and its Maven dependencies. Machine B can be Windows for either platform, since it only compiles/runs the Java client code.
 - Start Appium on Machine A and allow incoming TCP traffic on port `4723` in its firewall.
 - Replace `127.0.0.1` in the Java test with Machine A's LAN IP, for example `http://10.24.27.248:4723`.
 - When using `options.setApp(...)`, the app path must exist on Machine A because Appium reads it there.
 
 ## 7. CI/CD Options
 
 For a self-hosted Android runner that launches an emulator, install the same components required on Machine A above. The CI job must start a headless emulator, wait for Android to boot, start Appium, run `mvn test`, and publish `target/surefire-reports` as a test artifact. The runner must support hardware/nested virtualization for acceptable emulator speed.
 
 For iOS CI, the runner must be macOS (e.g. GitHub Actions `macos-latest`, or a self-hosted Mac). Install Xcode, Node.js, Appium, and the XCUITest driver, then start Appium and run `mvn test` the same way.
 
 Alternatively, BrowserStack, Sauce Labs, or LambdaTest provide the emulator/simulator/device and Appium server for both platforms. With that approach, the CI machine generally needs only JDK, Maven, and the test project (it does not need to be a Mac even for iOS); the test connects to the device farm's remote Appium URL instead of `localhost`.
 
 ## 8. Running Tests in Parallel Across Multiple Devices (Local)
 
 The `appium-device-farm` plugin (installed in section 3.2) handles this — no manual device/environment pool config needed in the framework itself:
 
 - Start Appium with the plugin enabled: `appium --use-plugins=device-farm --plugin-device-farm-platform=both` (as shown throughout this guide; the `platform` flag is required or the plugin finds 0 devices).
 - It auto-detects all connected Android emulators/devices and iOS simulators/devices.
 - It automatically load-balances and queues incoming sessions across whichever are free.
 - It ships a web dashboard showing connected devices and running sessions/builds.
 - Point `appium.server.url` at this same Appium server; no other framework changes are required. Give each test class its own `<test>` block in `testng.xml` with `parallel="tests"`/`thread-count` on `<suite>` to actually run them concurrently (this also avoids a known TestNG `GraphOrchestrator` NullPointerException seen when grouping classes under `parallel="classes"`/`"methods"` instead).
 - To pin a run to one or more specific devices instead of letting the plugin auto-allocate, set `android.udids`/`ios.udids` in `runtimeConfig.jsonc` to a comma-separated list of device UDIDs (for Android emulators, the UDID is the serial shown by `adb devices`, e.g. `emulator-5554`), or override per run with `-Dudid=emulator-5554` (takes precedence over the config file). This maps to the device-farm plugin's `df:udids` capability.
 
 ## 9. Setup Checklist
 
 ### General
 
 - [ ] JDK installed; `java -version` and `javac -version` work.
 - [ ] Maven installed; `mvn -v` works.
 - [ ] Node.js and npm installed; `node -v` and `npm -v` work.
 - [ ] Appium installed; `appium -v` works.
 - [ ] Maven project has Appium Java Client and TestNG dependencies.
 - [ ] Appium server starts on port 4723.
 
 ### Android
 
 - [ ] Android Studio and required Android SDK tools installed.
 - [ ] `ANDROID_HOME` / `ANDROID_SDK_ROOT` and Android SDK `Path` entries configured.
 - [ ] Emulator started or USB-debuggable real device connected; `adb devices` lists it as `device`.
 - [ ] UiAutomator2 installed; `appium driver list --installed` lists it.
 - [ ] APK path and test device configuration are correct.
 - [ ] `mvn clean test` reports `BUILD SUCCESS`.
 
 ### iOS
 
 - [ ] Running on macOS with Xcode and command-line tools installed.
 - [ ] XCUITest driver installed; `appium driver list --installed` lists it.
 - [ ] Simulator available (`xcrun simctl list devices`), or a real device with Developer Mode enabled and trusted.
 - [ ] For real devices: signing team configured (`xcodeOrgId`/`xcodeSigningId` or manual Xcode signing for WDA), `libimobiledevice`/`ios-deploy` installed.
 - [ ] `.app`/`.ipa` path and test device configuration are correct.
 - [ ] `mvn clean test` reports `BUILD SUCCESS`.
 
 ## Reference Links
 
 - Appium: https://appium.io/docs/en/latest/
 - Appium Java Client: https://github.com/appium/java-client
 - Android Studio: https://developer.android.com/studio
 - ApiDemos sample app: https://github.com/appium/android-apidemos/releases
 - Appium iOS TestApp sample: https://github.com/appium/ios-test-app
 - WebDriverAgent: https://github.com/appium/WebDriverAgent
 - Appium Inspector: https://github.com/appium/appium-inspector