---
layout: default
class: Header
title: Require-Capability ::= requirement ( ',' requirement )* 
summary: Specifies that a bundle requires other bundles to provide a capability 
---
	
				Parameters requirements = new Parameters(annotationHeaders.getHeader(REQUIRE_CAPABILITY));
			Parameters capabilities = new Parameters(annotationHeaders.getHeader(PROVIDE_CAPABILITY));

			//
			// Do any contracts contracts
			//
			contracts.addToRequirements(requirements);

			//
			// We want to add the minimum EE as a requirement
			// based on the class version
			//

			if (!isTrue(getProperty(NOEE)) //
					&& !ees.isEmpty() // no use otherwise
					&& since(About._2_3) // we want people to not have to
											// automatically add it
					&& !requirements.containsKey(ExecutionEnvironmentNamespace.EXECUTION_ENVIRONMENT_NAMESPACE) // and
																												// it
																												// should
																												// not
																												// be
																												// there
																												// already
			) {

				JAVA highest = ees.last();
				Attrs attrs = new Attrs();

				String filter = doEEProfiles(highest);

				attrs.put(Constants.FILTER_DIRECTIVE, filter);

				//
				// Java 1.8 introduced profiles.
				// If -eeprofile= auto | (<profile>="...")+ is set then
				// we add a

				requirements.add(ExecutionEnvironmentNamespace.EXECUTION_ENVIRONMENT_NAMESPACE, attrs);
			}

			if (!requirements.isEmpty())
				main.putValue(REQUIRE_CAPABILITY, requirements.toString());
	
	
		/*
	 * Require-Capability header
	 */
	private void doRequireCapability(RequireCapability annotation) {
		StringBuilder sb = new StringBuilder(annotation.ns());
		if (annotation.filter() != null)
			sb.append(";filter:='").append(annotation.filter()).append("'");
		if (annotation.effective() != null)
			sb.append(";effective:='").append(annotation.effective()).append("'");
		if (annotation.resolution() != null)
			sb.append(";resolution:='").append(annotation.resolution()).append("'");

		if (annotation.value() != null)
			sb.append(";").append(annotation.value());

		add(Constants.REQUIRE_CAPABILITY, sb.toString());
	}
	
	
	package aQute.bnd.annotation.headers;

		import java.lang.annotation.*;
		
		/**
		 * The Bundle’s Require-Capability header
		 * 
		 * {@link About}
		 */
		@Retention(RetentionPolicy.CLASS)
		@Target({
				ElementType.ANNOTATION_TYPE, ElementType.TYPE
		})
		public @interface RequireCapability {
			String value() default "";
		
			/**
			 * The capability namespace. For example: {@code osgi.contract}.
			 */
			String ns();
		
			/**
			 * Specifies the time a Requirement is considered, either 'resolve'
			 * (default) or another name. The OSGi framework resolver only considers
			 * Requirements without an effective directive or effective:=resolve. Other
			 * Requirements can be considered by an external agent. Additional names for
			 * the effective directive should be registered with the OSGi Alliance. See
			 * <a href="https://www.osgi.org/developer/specifications/reference/">OSGi Reference
			 * Page</a>
			 */
			String effective() default "resolve";
		
			/**
			 * A filter expression that is asserted on the Capabilities belonging to the
			 * given namespace. The matching of the filter against the Capability is
			 * done on one Capability at a time. A filter like {@code (&(a=1)(b=2))}
			 * matches only a Capability that specifies both attributes at the required
			 * value, not two capabilties that each specify one of the attributes
			 * correctly. A filter is optional, if no filter directive is specified the
			 * Requirement always matches.
			 */
			String filter();
		
			/**
			 * A mandatory Requirement forbids the bundle to resolve when the
			 * Requirement is not satisfied; an optional Requirement allows a bundle to
			 * resolve even if the Requirement is not satisfied. No wirings are created
			 * when this Requirement cannot be resolved, this can result in Class Not
			 * Found Exceptions when the bundle attempts to use a package that was not
			 * resolved because it was optional.
			 */
			Resolution resolution() default Resolution.mandatory;
		
		}
		
		
			private void verifyRequirements() {
		Parameters map = parseHeader(manifest.getMainAttributes().getValue(Constants.REQUIRE_CAPABILITY));
		for (String key : map.keySet()) {
			Attrs attrs = map.get(key);
			verify(attrs, "filter:", FILTERPATTERN, false, "Requirement %s filter not correct", key);

			String filter = attrs.get("filter:");
			if (filter != null) {
				String verify = new Filter(filter).verify();
				if (verify != null)
					error("Invalid filter syntax in requirement %s=%s. Reason %s", key, attrs, verify);
			}
			verify(attrs, "cardinality:", CARDINALITY_PATTERN, false, "Requirement %s cardinality not correct", key);
			verify(attrs, "resolution:", RESOLUTION_PATTERN, false, "Requirement %s resolution not correct", key);

			if (key.equals("osgi.extender")) {
				// No requirements on extender
			} else if (key.equals("osgi.serviceloader")) {
				verify(attrs, "register:", PACKAGEPATTERN, false,
						"Service Loader extender register: directive not a fully qualified Java name");
			} else if (key.equals("osgi.contract")) {

			} else if (key.equals("osgi.service")) {

			} else if (key.equals("osgi.ee")) {

			} else if (key.startsWith("osgi.wiring.") || key.startsWith("osgi.identity")) {
				error("osgi.wiring.* namespaces must not be specified with generic requirements/capabilities");
			}

			verifyAttrs(attrs);

			if (attrs.containsKey("mandatory:"))
				error("mandatory: directive is intended for Capabilities, not Requirement %s", key);

			if (attrs.containsKey("uses:"))
				error("uses: directive is intended for Capabilities, not Requirement %s", key);
		}
	}

		
	
