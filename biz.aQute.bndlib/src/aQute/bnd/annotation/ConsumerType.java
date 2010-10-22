package aQute.bnd.annotation;

import java.lang.annotation.*;

/**
 * Adding this annotation to a type in an API package indicates the the owner of
 * that package will not change this interface in a minor update. Any backward
 * compatible change to this interface requires a major update of the version of
 * this package.
 * 
 */
@Retention(RetentionPolicy.CLASS) @Target(ElementType.TYPE) public @interface ConsumerType {

}
