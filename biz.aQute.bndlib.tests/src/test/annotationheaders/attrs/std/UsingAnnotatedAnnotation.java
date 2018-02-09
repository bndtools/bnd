package test.annotationheaders.attrs.std;

@AnnotatedAnnotation(ignoredName = "sesame", usedName = {
	"sunflower", "marigold"
}, number = 42)
public class UsingAnnotatedAnnotation {

}
