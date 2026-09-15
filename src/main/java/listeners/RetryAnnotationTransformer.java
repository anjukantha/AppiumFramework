package listeners;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;

import org.testng.IAnnotationTransformer;
import org.testng.annotations.ITestAnnotation;

/**
 * Auto-attaches RetryAnalyzer to every @Test method so retries are
 * config-driven without per-test annotations.
 */
public class RetryAnnotationTransformer implements IAnnotationTransformer {

    @Override
    @SuppressWarnings("rawtypes") // raw Class/Constructor required to match IAnnotationTransformer's own
                                  // signature
    public void transform(ITestAnnotation annotation, Class testClass, Constructor testConstructor, Method testMethod) {
        annotation.setRetryAnalyzer(RetryAnalyzer.class);
    }
}