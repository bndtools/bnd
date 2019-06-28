package test.annotationheaders.custom.uses;

import static aQute.bnd.annotation.Constants.USES_MACRO;

import java.io.Serializable;

import org.osgi.annotation.bundle.Capability;

@Meta(uses = {
	Serializable.class, Capability.class
})
public class UsesCheck {}

@Capability(attribute = {
	USES_MACRO
}, namespace = "type")
@interface Meta {
	Class<?>[] uses() default {};
}
