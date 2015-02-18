package aQute.bnd.annotation;

import java.lang.annotation.*;

/**
 * This is a type that will be proxied. Any additions and deletions are ok. They
 * do not cause a version incompatibility.
 */
@Documented
@Retention(RetentionPolicy.CLASS)
@Target(ElementType.TYPE)
public @interface ProxyType {

}
