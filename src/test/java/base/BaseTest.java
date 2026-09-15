package base;

import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;

import config.ConfigManager;
import drivers.DriverFactory;
import drivers.DriverManager;
import io.appium.java_client.AppiumDriver;

public abstract class BaseTest {

    protected AppiumDriver driver;

    @BeforeClass(alwaysRun = true)
    public void setUp() throws Exception {
        ConfigManager config = new ConfigManager();
        driver = DriverFactory.createDriver(config);
        DriverManager.setDriver(driver);
    }

    @AfterClass(alwaysRun = true)
    public void tearDown() {
        DriverManager.quitDriver();
        driver = null;
        ConfigManager.clearActive();
    }
}