package test.annotationheaders.multiple;

import aQute.bnd.annotation.headers.ProvideCapability;
import aQute.bnd.annotation.headers.RequireCapability;

@ProvideCapability(ns = "provide")
@RequireCapability(ns = "require", filter = "(a=b)")
@interface WebApplication {
	String name();
}

@WebApplication(name = "Petstore")
public class PetstoreAppComponent {
	// ..
}
