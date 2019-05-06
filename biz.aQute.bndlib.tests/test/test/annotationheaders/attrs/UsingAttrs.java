package test.annotationheaders.attrs;

import test.annotationheaders.attrs.AnnotationWithAttrs.E;

@AnnotationWithAttrs(bar = 10, foo = {
	"abc", "def"
}, en = E.A)
@License(foo = "abc")
@ExtendedProvide(foo = 3, bar = 3)
@AnnotationWithValue("hello")
@SuppressWarnings("deprecation")
@aQute.bnd.annotation.headers.RequireCapability(ns = "nsz", filter = "(nsz=*)", extra = "hello=world")
@ParameterisedAnnotation(param1 = "hello", param2 = "goodbye")
public class UsingAttrs {

}
