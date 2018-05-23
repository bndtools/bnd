package test.annotationheaders.attrs.std;

@AnnotatedAnnotation(ignoredName = "sesame", usedName = {
	"sunflower", "marigold"
}, number = 42, anotherIgnoredName = "seed", x_anotherUsedName = {
	"foo", "bar"
}, x_anotherNumber = 17)
public class UsingAnnotatedAnnotation {

}
