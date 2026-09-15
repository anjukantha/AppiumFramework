package utils;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.openqa.selenium.OutputType;
import org.openqa.selenium.TakesScreenshot;

public final class ScreenshotUtils {

    private ScreenshotUtils() {
    }

    /**
     * Returns the absolute path of the saved screenshot, or null if the driver
     * can't capture one.
     */
    public static String capture(Object driver, String directory, String fileNamePrefix) {
        if (!(driver instanceof TakesScreenshot)) {
            return null;
        }
        try {
            File source = ((TakesScreenshot) driver).getScreenshotAs(OutputType.FILE);
            Path target = Path.of(directory, fileNamePrefix + "_" + System.currentTimeMillis() + ".png");
            Files.createDirectories(target.getParent());
            Files.copy(source.toPath(), target);
            return target.toAbsolutePath().toString();
        } catch (IOException e) {
            return null;
        }
    }
}