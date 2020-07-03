package bndtools.core.test.utils;

import static bndtools.core.test.utils.ResourceLock.TEST_WORKSPACE;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.parallel.ResourceLock;

/**
 * Used to mark a test that uses the WorkspaceExtension.
 * <p>
 * Usage:
 *
 * <pre>
 * <code>
 * package bndtools.core.test.my.pkg;
 * // with no parameters, it will look for the template workspace in
 * // resources/workspaces/my/pkg/mytest
 * &#64;WorkbenchTest
 * class MyTest {
 * }</code>
 * </pre>
 *
 * <pre>
 * <code>
 * package bndtools.core.test.my.pkg;
 * // with no parameters, it will look for the template workspace in
 * // resources/workspaces/my/custom/path
 * &#64;WorkbenchTest("my/custom/path")
 * class MyTest {
 * }</code>
 * </pre>
 *
 * @see WorkbenchExtension
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@ResourceLock(TEST_WORKSPACE)
@ExtendWith(WorkbenchExtension.class)
public @interface WorkbenchTest {

	String value() default "";

}
