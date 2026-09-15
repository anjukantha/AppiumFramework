package pages;

import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.PageFactory;

import io.appium.java_client.AppiumDriver;
import io.appium.java_client.pagefactory.AppiumFieldDecorator;
import utils.WaitUtils;

public abstract class BasePage {

    protected final AppiumDriver driver;

    protected BasePage(AppiumDriver driver) {
        this.driver = driver;
        PageFactory.initElements(new AppiumFieldDecorator(driver), this);
    }

    protected WebElement waitForVisible(WebElement element) {
        return WaitUtils.waitForVisible(driver, element);
    }
}