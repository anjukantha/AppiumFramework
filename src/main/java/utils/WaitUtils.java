package utils;
 
 import config.ConfigManager;
 import io.appium.java_client.AppiumDriver;
import io.appium.java_client.android.StartsActivity;
import org.openqa.selenium.By;
 import org.openqa.selenium.WebElement;
import org.openqa.selenium.StaleElementReferenceException;
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

    /** Finds the element again on every poll, which allows recovery after a screen transition. */
    public static WebElement waitForVisible(AppiumDriver driver, By locator) {
        return waitForVisible(driver, locator, defaultTimeout());
    }

    public static WebElement waitForVisible(AppiumDriver driver, By locator, Duration timeout) {
        return createWait(driver, timeout).until(
                ExpectedConditions.refreshed(ExpectedConditions.visibilityOfElementLocated(requiredLocator(locator))));
    }

    public static WebElement waitForPresent(AppiumDriver driver, By locator) {
        return waitForPresent(driver, locator, defaultTimeout());
    }

    public static WebElement waitForPresent(AppiumDriver driver, By locator, Duration timeout) {
        return createWait(driver, timeout).until(
                ExpectedConditions.presenceOfElementLocated(requiredLocator(locator)));
    }
 
     public static WebElement waitForClickable(AppiumDriver driver, WebElement element) {
         return waitForClickable(driver, element, defaultTimeout());
     }
 
     public static WebElement waitForClickable(AppiumDriver driver, WebElement element, Duration timeout) {
         return createWait(driver, timeout).until(ExpectedConditions.elementToBeClickable(requiredElement(element)));
     }

    public static WebElement waitForClickable(AppiumDriver driver, By locator) {
        return waitForClickable(driver, locator, defaultTimeout());
    }

    public static WebElement waitForClickable(AppiumDriver driver, By locator, Duration timeout) {
        return createWait(driver, timeout).until(
                ExpectedConditions.refreshed(ExpectedConditions.elementToBeClickable(requiredLocator(locator))));
    }

    public static boolean waitForText(AppiumDriver driver, By locator, String text) {
        return waitForText(driver, locator, text, defaultTimeout());
    }

    public static boolean waitForText(AppiumDriver driver, By locator, String text, Duration timeout) {
        Objects.requireNonNull(text, "text must not be null");
        return createWait(driver, timeout).until(
                ExpectedConditions.textToBePresentInElementLocated(requiredLocator(locator), text));
    }

    public static boolean waitForAttribute(AppiumDriver driver, By locator, String attribute, String value) {
        return waitForAttribute(driver, locator, attribute, value, defaultTimeout());
    }

    public static boolean waitForAttribute(
            AppiumDriver driver, By locator, String attribute, String value, Duration timeout) {
        Objects.requireNonNull(attribute, "attribute must not be null");
        Objects.requireNonNull(value, "value must not be null");
        return createWait(driver, timeout).until(
                ExpectedConditions.attributeToBe(requiredLocator(locator), attribute, value));
    }

    public static boolean waitForPageSourceContains(AppiumDriver driver, String text) {
        return waitForPageSourceContains(driver, text, defaultTimeout());
    }

    public static boolean waitForPageSourceContains(AppiumDriver driver, String text, Duration timeout) {
        Objects.requireNonNull(text, "text must not be null");
        return createWait(driver, timeout).until(currentDriver -> {
            String pageSource = currentDriver.getPageSource();
            return pageSource != null && pageSource.contains(text);
        });
    }

    public static String waitForActivity(AppiumDriver driver, String activity) {
        return waitForActivity(driver, activity, defaultTimeout());
    }

    public static String waitForActivity(AppiumDriver driver, String activity, Duration timeout) {
        Objects.requireNonNull(activity, "activity must not be null");
        if (!(driver instanceof StartsActivity)) {
            throw new IllegalArgumentException("The active driver does not expose Android activity information.");
        }
        StartsActivity androidDriver = (StartsActivity) driver;
        return createWait(driver, timeout).until(currentDriver -> {
            String currentActivity = androidDriver.currentActivity();
            return activity.equals(currentActivity) ? currentActivity : null;
        });
    }

    public static String waitForPackage(AppiumDriver driver, String packageName) {
        return waitForPackage(driver, packageName, defaultTimeout());
    }

    public static String waitForPackage(AppiumDriver driver, String packageName, Duration timeout) {
        Objects.requireNonNull(packageName, "packageName must not be null");
        if (!(driver instanceof StartsActivity)) {
            throw new IllegalArgumentException("The active driver does not expose Android package information.");
        }
        StartsActivity androidDriver = (StartsActivity) driver;
        return createWait(driver, timeout).until(currentDriver -> {
            String currentPackage = androidDriver.getCurrentPackage();
            return packageName.equals(currentPackage) ? currentPackage : null;
        });
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
        WebDriverWait wait = new WebDriverWait(driver, timeout);
        wait.ignoring(StaleElementReferenceException.class);
        return wait;
     }
 
     private static WebElement requiredElement(WebElement element) {
         return Objects.requireNonNull(element, "element must not be null");
     }

    private static By requiredLocator(By locator) {
        return Objects.requireNonNull(locator, "locator must not be null");
    }
 }