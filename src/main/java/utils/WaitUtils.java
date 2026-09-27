package utils;
 
 import config.ConfigManager;
 import io.appium.java_client.AppiumDriver;
 import org.openqa.selenium.WebElement;
 import org.openqa.selenium.support.ui.ExpectedConditions;
 import org.openqa.selenium.support.ui.WebDriverWait;
 
 import java.time.Duration;
 import java.util.Objects;
 
 /** Shared explicit-wait operations for Appium page objects. */
 public final class WaitUtils {
 
     private static final Duration FALLBACK_TIMEOUT = Duration.ofSeconds(15);
 
     private WaitUtils() {
     }
 
     /** Returns the configured default timeout, or 15 seconds when no test configuration is active. */
     public static Duration defaultTimeout() {
         ConfigManager config = ConfigManager.getActive();
         return config != null ? Duration.ofSeconds(config.getWaitTimeoutSeconds()) : FALLBACK_TIMEOUT;
     }
 
     public static WebElement waitForVisible(AppiumDriver driver, WebElement element) {
         return waitForVisible(driver, element, defaultTimeout());
     }
 
     public static WebElement waitForVisible(AppiumDriver driver, WebElement element, Duration timeout) {
         return createWait(driver, timeout).until(ExpectedConditions.visibilityOf(requiredElement(element)));
     }
 
     public static WebElement waitForClickable(AppiumDriver driver, WebElement element) {
         return waitForClickable(driver, element, defaultTimeout());
     }
 
     public static WebElement waitForClickable(AppiumDriver driver, WebElement element, Duration timeout) {
         return createWait(driver, timeout).until(ExpectedConditions.elementToBeClickable(requiredElement(element)));
     }
 
     public static boolean waitForInvisible(AppiumDriver driver, WebElement element) {
         return waitForInvisible(driver, element, defaultTimeout());
     }
 
     public static boolean waitForInvisible(AppiumDriver driver, WebElement element, Duration timeout) {
         return createWait(driver, timeout).until(ExpectedConditions.invisibilityOf(requiredElement(element)));
     }
 
     private static WebDriverWait createWait(AppiumDriver driver, Duration timeout) {
         Objects.requireNonNull(driver, "driver must not be null");
         Objects.requireNonNull(timeout, "timeout must not be null");
         if (timeout.isNegative()) {
             throw new IllegalArgumentException("timeout must not be negative");
         }
         return new WebDriverWait(driver, timeout);
     }
 
     private static WebElement requiredElement(WebElement element) {
         return Objects.requireNonNull(element, "element must not be null");
     }
 }