package utils;

import java.time.Duration;

import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import config.ConfigManager;
import io.appium.java_client.AppiumDriver;

public final class WaitUtils {

    private static final Duration FALLBACK_TIMEOUT = Duration.ofSeconds(15);

    private WaitUtils() {
    }

    public static Duration getDefaultTimeout() {
        ConfigManager config = ConfigManager.getActive();
        return config != null ? Duration.ofSeconds(config.getWaitTimeoutSeconds()) : FALLBACK_TIMEOUT;
    }

    public static WebElement waitForVisible(AppiumDriver driver, WebElement element) {
        return waitForVisible(driver, element, getDefaultTimeout());
    }

    public static WebElement waitForVisible(AppiumDriver driver, WebElement element, Duration timeout) {
        return new WebDriverWait(driver, timeout).until(ExpectedConditions.visibilityOf(element));
    }

    public static WebElement waitForClickable(AppiumDriver driver, WebElement element) {
        return new WebDriverWait(driver, getDefaultTimeout()).until(ExpectedConditions.elementToBeClickable(element));
    }

    public static boolean waitForInvisible(AppiumDriver driver, WebElement element) {
        return new WebDriverWait(driver, getDefaultTimeout()).until(ExpectedConditions.invisibilityOf(element));
    }
}