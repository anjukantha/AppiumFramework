package tests;

import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import base.BaseTest;
import pages.ApiDemosHomePage;
import utils.Log;

public class FirstAndroidTest extends BaseTest {

    private ApiDemosHomePage apiDemosHomePage;

    @BeforeClass(dependsOnMethods = "setUp", alwaysRun = true)
    public void initPage() {
        apiDemosHomePage = new ApiDemosHomePage(driver);
    }

    @Test
    public void testAccessibilityMenuItem() {
        Log.info("Checking the Accessibility menu item");
        Assert.assertTrue(apiDemosHomePage.isAccessibilityMenuItemDisplayed(),
                "Accessibility menu item is not displayed");
        apiDemosHomePage.tapAccessibilityMenuItem();
        Log.info("Tapped on the Accessibility menu item");
    }
}