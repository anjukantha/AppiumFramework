package listeners;

import org.testng.IRetryAnalyzer;
import org.testng.ITestResult;

import config.ConfigManager;
import drivers.DriverManager;

/**
 * Retries a failed @Test up to config's "retry.count" times (default 0 = no
 * retries).
 */
public class RetryAnalyzer implements IRetryAnalyzer {

    private int attempts = 0;

    @Override
    public boolean retry(ITestResult result) {
        ConfigManager config = ConfigManager.getActive();
        int maxRetries = config != null ? config.getRetryCount() : 0;
        if (attempts < maxRetries) {
            attempts++;
            DriverManager.restartApp(config.getAppId());
            return true;
        }
        return false;
    }
}