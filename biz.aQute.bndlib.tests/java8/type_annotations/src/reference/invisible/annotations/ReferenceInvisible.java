package reference.invisible.annotations;

import invisible.annotations.InvisibleParameterAnnotation;
import invisible.annotations.InvisibleRepeatedAnnotation;
import invisible.annotations.InvisibleTypeAnnotation;

@InvisibleRepeatedAnnotation
@InvisibleRepeatedAnnotation
@InvisibleRepeatedAnnotation
@InvisibleRepeatedAnnotation
public class ReferenceInvisible {
	
	void foo(@InvisibleParameterAnnotation int p) {
		new @InvisibleTypeAnnotation Object();
	}
}
