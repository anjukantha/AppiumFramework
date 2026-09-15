# Copilot Instructions for Appium-Framework
 
 This is a Java + Maven + TestNG mobile UI automation framework using Appium, targeting **both Android (UiAutomator2 driver) and iOS (XCUITest driver)** as first-class, equally supported platforms.
 
 ## Project context
 - Build tool: Maven (`pom.xml`). Java 11 source/target.
 - Test runner: TestNG, driven by `testng.xml`.
 - Framework code (config, driver management, page objects) lives under `src/main/java`; tests live under `src/test/java`.
 - Sample/test apps (`.apk` for Android, `.app`/`.ipa` for iOS) live under `apps/`.
 - Appium server must be started separately (`appium`) before running tests; tests connect to `http://127.0.0.1:4723`.
 - See [APPIUM_SETUP_GUIDE.md](../APPIUM_SETUP_GUIDE.md) for full environment setup (general, Android-specific, and iOS-specific sections; JDK, Android SDK, Node/Appium, Maven).
 - Appium's XCUITest driver only runs on macOS, so running iOS tests requires a Mac (or cloud Mac/device farm) to host the Appium server, even though Java test code can be authored and compiled on Windows.
 
 ## Framework structure
 - `enums.Platform` — ANDROID/IOS enum driving all platform switches.
 - `config.ConfigManager` — loads `runtimeConfig.jsonc` (from the project root, for easy editing — Appium server URL, active platform, per-platform device/app details under `android.*`/`ios.*` e.g. `android.appPath`, screenshot settings, report identity, retry count, driver capabilities); active platform is resolved with precedence: `-Dplatform=<value>` system property, then `PLATFORM` env var, then runtimeConfig.jsonc's `platform` key, then default `android` (e.g. `mvn test -Dplatform=ios`). Nested JSON objects are flattened into dot-separated keys (e.g. `"screenshot": { "mode": "all" }` → `screenshot.mode`). Also exposes `getScreenshotMode()` (`enums.ScreenshotMode`: NONE/FAILURE/PASS/ALL, config key `screenshot.mode`, default FAILURE), `isScreenshotOnStep()` (config key `screenshot.onStep`, default false), `getProductName()`/`getTeamName()`/`getRunType()` (config keys under `report.*`, used in the report header), `getRetryCount()` (config key `retry.count`, default 0), and `getDeviceUdids()` (`android.udids`/`ios.udids`, comma-separated, overridable via `-Dudid=<value>`/`UDID` env var, same precedence pattern as platform) used to pin the appium-device-farm plugin to specific device(s) instead of auto-allocating. There is no `deviceName` config/capability — appium-device-farm doesn't use it for device selection (only `platformName`/`platformVersion`/`df:udids`/`df:minSDK`/`df:maxSDK`/`df:tags`/`df:filterByHost` matter there), so it was dropped; the report's "environment" label uses `getPlatform().name()` instead. Each instance registers itself in a static `ThreadLocal` (`ConfigManager.getActive()`) so non-test classes (`ReportListener`, `Log`, `RetryAnalyzer`) can read config without a direct reference; `BaseTest.tearDown()` calls `ConfigManager.clearActive()` to avoid leaking across threads.
 - `drivers.DriverFactory` — builds `AndroidDriver`/`UiAutomator2Options` or `IOSDriver`/`XCUITestOptions` from a `ConfigManager`; when `getDeviceUdids()` is non-null it also sets the `df:udids` capability so appium-device-farm restricts allocation to those device(s).
 - `drivers.DriverManager` — holds the active `AppiumDriver` in a `ThreadLocal` for parallel-safe execution.
 - `base.BaseTest` — `@BeforeClass`/`@AfterClass` wiring that creates the driver via `DriverFactory` and tears it down via `DriverManager`; test classes extend this instead of managing their own driver.
 - `pages.BasePage` — common `PageFactory`/`AppiumFieldDecorator` init and wait helpers; concrete pages extend it.
 - Page objects are unified by default (one class per screen using `@AndroidFindBy`/`@iOSXCUITFindBy` on the same fields); only split into separate per-platform classes behind a common interface when a screen's structure genuinely diverges between platforms (hybrid/Option C approach).
 - `utils.WaitUtils` — shared explicit-wait helpers (visible/clickable/invisible); `pages.BasePage` delegates to it instead of building its own `WebDriverWait`.
 - `utils.ScreenshotUtils` — captures a screenshot from any `TakesScreenshot` driver to a given directory.
 - `utils.Log` — single logging entry point (`info`/`pass`/`warn`/`error`): writes to Log4j2 (console + `logs/`) and, if a test is running, appends a `reporting.model.StepLog` to the current `reporting.model.TestAttempt` (via `reporting.ReportManager`). When `screenshot.onStep` is enabled, each log call also attaches a screenshot. Use this instead of `System.out`/raw loggers in tests and page objects.
 - Reporting is a fully custom, dependency-free HTML report (no ExtentReports) built from `reporting.model.*` (`SuiteRecord` → `TestRecord` → `TestAttempt` → `StepLog`): `reporting.ReportManager` is the static, thread-safe in-memory store for the whole run (per-thread "current test/attempt", keyed by `className#methodName` so retries share one `TestRecord`), and `reporting.HtmlReportRenderer` renders it to `test-output/<yyyy-MM-dd_HH-mm-ss>/` as `index.html` (header `"<productName> - <teamName> - <runType> - <environment>"`, a run summary table, and a tests table with a dynamic `Retry #N` column per extra attempt) plus one `tests/<testName>.html` detail page per test with every attempt's steps/screenshots.
 - `listeners.ReportListener` — registered in `testng.xml`'s `<listeners>`; implements both `ISuiteListener` (populates suite identity/timing on start, renders the HTML report on finish) and `ITestListener` (starts/finishes `TestAttempt`s per test, attaches a screenshot per `ConfigManager.getScreenshotMode()`).
 - `listeners.RetryAnalyzer`/`listeners.RetryAnnotationTransformer` — config-driven retry: the transformer auto-attaches `RetryAnalyzer` to every `@Test` method (no per-test annotation needed), and `RetryAnalyzer` retries up to `ConfigManager.getRetryCount()` (`retry.count` in runtimeConfig.jsonc, default 0 = disabled).
 - Logging config lives in `src/test/resources/log4j2.xml` (console + rolling file under `logs/`, rotated on every run).
 
 ## Conventions
 - Use `UiAutomator2Options` (Android) or `XCUITestOptions` (iOS) to configure driver capabilities (device name, app path, reset behavior) rather than raw `DesiredCapabilities`.
 - Prefer `@BeforeClass`/`@AfterClass` for driver setup/teardown so a single driver session is reused across the test class's `@Test` methods.
 - Always quit the driver in an `@AfterClass`/`@AfterMethod` guarded by a null check to avoid leaking Appium sessions.
 - Locate elements with stable, cross-platform-friendly locators (accessibility id, resource-id) over fragile XPath text matches where possible.
 - Keep device/app-specific values (device name, platform version, app path) under `android`/`ios` in `runtimeConfig.jsonc` (project root), not hardcoded in test classes; `ConfigManager` reads them from the active platform's block (`getPlatform()` selects which).
 - Do not commit real device identifiers or credentials; the current hardcoded emulator/device name in `runtimeConfig.jsonc` is a placeholder and expected to be overridden locally.
 - Use `utils.Log` for step logging in tests/pages so steps show up in both the log file and the HTML report; avoid `System.out.println`.
 - TestNG (`org.testng`) is a regular (compile-scope) Maven dependency, so framework code in `src/main/java` (e.g. `listeners.ReportListener`) may use TestNG APIs directly; test classes still belong in `src/test/java`.
 
 ## When adding new code
 - New test classes go under `src/test/java/tests`, must extend `base.BaseTest`, and must be registered in `testng.xml`.
 - New screens get a page object under `src/main/java/pages` extending `BasePage`; follow the hybrid approach described above instead of duplicating setup code per test class.
 - Write new tests and abstractions so they work for both Android and iOS where practical; avoid Android-only assumptions (e.g. XPath tied to Android widget classes) in shared code.
 
 ## Build/test commands
 - Run all tests: `mvn test`
 - Reports are generated under `target/surefire-reports/`.