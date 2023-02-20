package test.annotationheaders.div;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

import org.osgi.annotation.bundle.Attribute;
import org.osgi.annotation.bundle.Header;

@Header(name = "Foo", value = "${#a} ${#b}")
@Retention(RetentionPolicy.CLASS)
@interface HeaderWithValues {
	@Attribute
	String a();

	@Attribute
	Class<?> b();
}

