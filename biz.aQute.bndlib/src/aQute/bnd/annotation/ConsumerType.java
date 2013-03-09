package aQute.bnd.annotation;

import java.lang.annotation.*;

/**
 * <p>
 * Adding this annotation to a type in an API package indicates that the owner of
 * that package will not change this interface in a minor update. Any backward
 * incompatible change to this interface requires a major update of the version
 * of this package. This annotation is provided for convenience because it is the default. 
 * </p>
 * <p>
 * For an elaborate and simple explanation, see {@link ProviderType}.
 * </p>
 */
@Documented
@Retention(RetentionPolicy.CLASS)
@Target(ElementType.TYPE)
public @interface ConsumerType {

}
