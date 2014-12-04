package test.annotationheaders.attrs;

import test.annotationheaders.attrs.AnnotationWithAttrs.E;

@AnnotationWithAttrs(bar=10, foo={"abc","def"}, en=E.A)
@License(foo="abc")
@ExtendedProvide(foo=3)
public class UsingAttrs {

}
