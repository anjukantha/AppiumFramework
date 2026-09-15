package pages;

import org.openqa.selenium.WebElement;

import io.appium.java_client.AppiumDriver;
import io.appium.java_client.pagefactory.AndroidBy;
import io.appium.java_client.pagefactory.iOSXCUITFindBy;

public class ApiDemosHomePage extends BasePage {
    @AndroidBy(accessibility = "Accessibility")
    @iOSXCUITFindBy(accessibility = "Accessibility")
    private WebElement accessibilityMenuItem;

    public ApiDemosHomePage(AppiumDriver driver) {
        super(driver);
    }

    public boolean isAccessibilityMenuItemDisplayed() {
        return waitForVisible(accessibilityMenuItem).isDisplayed();
    }

    public void tapAccessibilityMenuItem() {
        waitForVisible(accessibilityMenuItem).click();
    }
}
