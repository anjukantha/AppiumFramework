package utils;

import java.time.Duration;

import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import io.appium.java_client.AppiumDriver;

public final class WaitUtils {

    public static final Duration DEFAULT_TIMEOUT = Duration.ofSeconds(15);

    private WaitUtils() {
    }

    public static WebElement waitForVisible(AppiumDriver driver, WebElement element) {
        return waitForVisible(driver, element, DEFAULT_TIMEOUT);
    }

    public static WebElement waitForVisible(AppiumDriver driver, WebElement element, Duration timeout) {
        return new WebDriverWait(driver, timeout).until(ExpectedConditions.visibilityOf(element));
    }

    public static WebElement waitForClickable(AppiumDriver driver, WebElement element) {
        return new WebDriverWait(driver, DEFAULT_TIMEOUT).until(ExpectedConditions.elementToBeClickable(element));
    }

    public static boolean waitForInvisible(AppiumDriver driver, WebElement element) {
        return new WebDriverWait(driver, DEFAULT_TIMEOUT).until(ExpectedConditions.invisibilityOf(element));
    }
}