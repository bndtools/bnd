package test.annotationheaders.multiple;

@SuppressWarnings("deprecation")
@aQute.bnd.annotation.headers.ProvideCapability(ns = "provide")
@aQute.bnd.annotation.headers.RequireCapability(ns = "require", filter = "(a=b)")
@interface WebApplication {
	String name();
}

@WebApplication(name = "Petstore")
public class PetstoreAppComponent {
	// ..
}
