package aQute.bnd.annotation;

import java.lang.annotation.*;

@Retention(RetentionPolicy.CLASS)
@Target(ElementType.PACKAGE)
public @interface Export {
    String RNAME = "LaQute/bnd/annotation/Export;";
    String VERSION   = "version";
    String MANDATORY = "mandatory";
    String OPTIONAL  = "optional";
    String USES      = "uses";
    String EXCLUDE   = "exclude";
    String INCLUDE   = "include";

    String version() default "";

    String[] mandatory() default "";

    String[] optional() default "";

    Class<?>[] exclude() default Object.class;

    Class<?>[] include() default Object.class;
}
