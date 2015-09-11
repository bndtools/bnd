package test.packageinfo.annotated;

import java.lang.annotation.*;

@Target(ElementType.PACKAGE)
@Retention(RetentionPolicy.CLASS)
public @interface BlahAnnotation {}
