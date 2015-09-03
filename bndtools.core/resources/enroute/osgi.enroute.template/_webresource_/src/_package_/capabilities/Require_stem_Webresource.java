package _package_.capabilities;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

import aQute.bnd.annotation.headers.RequireCapability;
import osgi.enroute.namespace.WebResourceNamespace;

/**
 * A sample web resource requirement 
 */

@RequireCapability(ns = WebResourceNamespace.NS, filter = "(&(" + WebResourceNamespace.NS + "="
		+ _stem_Constants._STEM_PATH + ")${frange;" + _stem_Constants._STEM_VERSION
		+ "})")
@Retention(RetentionPolicy.CLASS)
public @interface Require_stem_Webresource {

	String[] resource() default "_stem_.js";
	int priority() default 0;
}
