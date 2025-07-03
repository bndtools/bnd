---
layout: default
class: Header
title: Provide-Capability  ::= capability (',' capability )* 
summary: Specifies that a bundle provides a set of Capabilities, 
---
	
			Parameters capabilities = new Parameters(annotationHeaders.getHeader(PROVIDE_CAPABILITY));
	
	
		/*
	 * Provide-Capability header
	 */
	private void doProvideCapability(ProvideCapability annotation) {
		StringBuilder sb = new StringBuilder(annotation.ns());
		if (annotation.name() != null)
			sb.append(";").append(annotation.ns()).append("='").append(annotation.name()).append("'");
		if (annotation.uses() != null)
			sb.append(";").append("uses:='").append(Strings.join(",", annotation.uses())).append("'");
		if (annotation.mandatory() != null)
			sb.append(";").append("mandatory:='").append(Strings.join(",", annotation.mandatory())).append("'");
		if (annotation.version() != null)
			sb.append(";").append("version:Version='").append(annotation.version()).append("'");
		if (annotation.value() != null)
			sb.append(";").append(annotation.value());
		if (annotation.effective() != null)
			sb.append(";effective:='").append(annotation.effective()).append("'");

		add(Constants.PROVIDE_CAPABILITY, sb.toString());
	}

	
	
	
			package aQute.bnd.annotation.headers;
		
		import java.lang.annotation.*;

		/**
		 * Define a Provide Capability clause in the manifest.
		 * <p>
		 * Since this annotation can only be applied once, it is possible to create an annotation
		 * that models a specific capability. For example:
		 * <pre>
		 * interface Webserver {
		 * 		@ProvideCapability(ns="osgi.extender", name="aQute.webserver", version = "${@version}")
		 * 	 	@interface Provide {}
		 * 
		 * 		@RequireCapability(ns="osgi.extender", filter="(&(osgi.extender=aQute.webserver)${frange;${@version}})")
		 * 	 	@interface Require {}
		 * }
		 * 
		 * Webserver.@Provide
		 * public class MyWebserver {
		 * }
		 * </pre>
		 * 
		 */
		@Retention(RetentionPolicy.CLASS)
		@Target({
				ElementType.ANNOTATION_TYPE, ElementType.TYPE
		})
		public @interface ProvideCapability {
			/**
			 * Appended at the end of the clause (after a ';'). Can be used to add
			 * additional attributes and directives.
			 */
			String value() default "";
		
			/**
			 * The capability namespace. For example: {@code osgi.contract}.
			 */
			String ns();
		
			/**
			 * The name of the capability. If this is set, a property will be added as
			 * {ns}={name}. This is the custom pattern for OSGi namespaces. Leaving this
			 * unfilled, requires the {@link #value()} to be used to specify the name of
			 * the capability, if needed. For example {@code aQute.sse}.
			 */
			String name() default "";
		
			/**
			 * The version of the capability. This must be a valid OSGi version.
			 */
			String version() default "";
		
			/**
			 * Effective time. Specifies the time a capability is available, either
			 * resolve (default) or another name. The OSGi framework resolver only
			 * considers Capabilities without an effective directive or
			 * effective:=resolve. Capabilities with other values for the effective
			 * directive can be considered by an external agent.
			 */
			String effective() default "resolve";
		
			/**
			 * The uses directive lists package names that are used by this Capability.
			 * This information is intended to be used for <em>uses constraints</em>,
			 */
			String[] uses() default {};
		
			/**
			 * Mandatory attributes. Forces the resolver to only satisfy filters that
			 * refer to all listed attributes.
			 */
			String[] mandatory() default {};
		}
			
			
			
					verifyDirectives(Constants.PROVIDE_CAPABILITY, "effective:|uses:", null, null);
					
					
					
					
			private void verifyCapabilities() {
		Parameters map = parseHeader(manifest.getMainAttributes().getValue(Constants.PROVIDE_CAPABILITY));
		for (String key : map.keySet()) {
			Attrs attrs = map.get(key);
			verify(attrs, "cardinality:", CARDINALITY_PATTERN, false, "Requirement %s cardinality not correct", key);
			verify(attrs, "resolution:", RESOLUTION_PATTERN, false, "Requirement %s resolution not correct", key);

			if (key.equals("osgi.extender")) {
				verify(attrs, "osgi.extender", SYMBOLICNAME, true,
						"Extender %s must always have the osgi.extender attribute set", key);
				verify(attrs, "version", VERSION, true, "Extender %s must always have a version", key);
			} else if (key.equals("osgi.serviceloader")) {
				verify(attrs, "register:", PACKAGEPATTERN, false,
						"Service Loader extender register: directive not a fully qualified Java name");
			} else if (key.equals("osgi.contract")) {
				verify(attrs, "osgi.contract", SYMBOLICNAME, true,
						"Contracts %s must always have the osgi.contract attribute set", key);

			} else if (key.equals("osgi.service")) {
				verify(attrs, "objectClass", MULTIPACKAGEPATTERN, true,
						"osgi.service %s must have the objectClass attribute set", key);

			} else if (key.equals("osgi.ee")) {
				// TODO
			} else if (key.startsWith("osgi.wiring.") || key.startsWith("osgi.identity")) {
				error("osgi.wiring.* namespaces must not be specified with generic requirements/capabilities");
			}

			verifyAttrs(attrs);

			if (attrs.containsKey("filter:"))
				error("filter: directive is intended for Requirements, not Capability %s", key);
			if (attrs.containsKey("cardinality:"))
				error("cardinality: directive is intended for Requirements, not Capability %s", key);
			if (attrs.containsKey("resolution:"))
				error("resolution: directive is intended for Requirements, not Capability %s", key);
		}
	}
		
			