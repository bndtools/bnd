package req;

import org.osgi.service.configurator.annotations.RequireConfigurator;

import static java.lang.annotation.ElementType.PACKAGE;
import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.CLASS;


import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;


@Documented
@Retention(CLASS)
@Target({ TYPE, PACKAGE })

@RequireConfigurator
public @interface RequireAnnotationOne {

}
