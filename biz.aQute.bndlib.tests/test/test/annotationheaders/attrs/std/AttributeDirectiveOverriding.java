package test.annotationheaders.attrs.std;

import org.osgi.annotation.bundle.Attribute;
import org.osgi.annotation.bundle.Capability;
import org.osgi.annotation.bundle.Directive;
import org.osgi.annotation.bundle.Requirement;

/**
 * Should have
 * <ul>
 * <li>foo:List<String>=foobar</li>
 * <li>x-top-name:=fizzbuzz</li>
 * <li>name=Steve</li>
 * <li>x-name:=Dave</li>
 * <li>overriding=foo</li>
 * </ul>
 */
@FinalAnnotation(name = "Chris")
public class AttributeDirectiveOverriding {}

@BottomAnnotation(foo = "foobar")
@interface FinalAnnotation {

	// This does *not* override the name from the req/cap of Top
	// as that part of the spec was changed (it caused breakages
	// in DS and metatype)
	String name();

}

@Capability(namespace = "overriding", name = "foo", version = "1")
@Requirement(namespace = "overriding", name = "foo", version = "1")
@interface TopAnnotation {

	// Not an override as this is an attribute
	@Attribute
	String name();

	@Attribute
	String foo();

	@Directive("x-top-name")
	String fizz();
}

@TopAnnotation(name = "Steve", foo = "bar", fizz = "buzz")
@interface MiddleAnnotation {
	// Not an override as this is a directive
	@Directive("x-name")
	String name();

	// Override, even though the member name is different
	@Directive("x-top-name")
	String middle();
}

@MiddleAnnotation(name = "Dave", middle = "fizzbuzz")
@interface BottomAnnotation {
	// Overrides and changes type
	@Attribute
	String[] foo();
}
