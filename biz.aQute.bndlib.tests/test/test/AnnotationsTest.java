package test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.lang.annotation.RetentionPolicy;
import java.util.Collections;
import java.util.Map;

import org.junit.jupiter.api.Test;

import aQute.bnd.annotation.metatype.Configurable;
import aQute.bnd.osgi.Analyzer;
import aQute.bnd.osgi.Annotation;
import aQute.bnd.osgi.Annotation.ElementType;
import aQute.bnd.osgi.Descriptors.TypeRef;

public class AnnotationsTest {

	@SuppressWarnings({
		"rawtypes", "unchecked"
	})
	@Test
	public void testNestedAnnotations() throws Exception {
		try (Analyzer analyzer = new Analyzer();) {
			TypeRef typeref = analyzer.getTypeRefFromFQN(ActualAnnotation.class.getName());
			Map<String, Object> annMap = Collections.singletonMap("a", 5);
			Annotation annotation = new Annotation(typeref, annMap, ElementType.FIELD, RetentionPolicy.RUNTIME);

			Map<String, Object> properties = Collections.singletonMap("ann", annotation);
			ConfigurableInterface a = Configurable.createConfigurable(ConfigurableInterface.class, properties);

			assertNotNull(a);
			assertNotNull(a.ann());
			assertEquals(5, a.ann()
				.a());
		}
	}
}

@interface ActualAnnotation {
	int a() default 1;
}

interface ConfigurableInterface {
	ActualAnnotation ann();
}
