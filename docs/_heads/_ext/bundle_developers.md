---
layout: default
class: Header
title: Bundle-Developers ... 
summary: Lists the bundle developers according to the Maven bundle-developers pom entry
---
	
	/*
	 * Bundle-Developers header
	 */
	private void doBundleDevelopers(BundleDevelopers annotation) throws IOException {
		StringBuilder sb = new StringBuilder(annotation.value());
		if (annotation.name() != null) {
			sb.append(";name='");
			escape(sb, annotation.name());
			sb.append("'");
		}
		if (annotation.roles() != null) {
			sb.append(";roles='");
			escape(sb,annotation.roles());
			sb.append("'");
		}
		if (annotation.organizationUrl() != null) {
			sb.append(";organizationUrl='");
			escape(sb,annotation.organizationUrl());
			sb.append("'");
		}
		if (annotation.organization() != null) {
			sb.append(";organization='");
			escape(sb,annotation.organization());
			sb.append("'");
		}
		if (annotation.timezone() != 0)
			sb.append(";timezone=").append(annotation.timezone());

		add(Constants.BUNDLE_DEVELOPERS, sb.toString());
	}

	
			/**
		 * Maven defines developers and developers in the POM. This annotation will
		 * generate a (not standardized by OSGi) Bundle-Developers header.
		 * <p>
		 * A deve
		 * <p>
		 * This annotation can be used directly on a type or it can 'color' an
		 * annotation. This coloring allows custom annotations that define a specific
		 * developer. For example:
		 * 
		 * <pre>
		 *   @BundleContributor("Peter.Kriens@aQute.biz")
		 *   @interface pkriens {}
		 *   
		 *   @pkriens
		 *   public class MyFoo {
		 *     ...
		 *   }
		 * </pre>
		 * 
		 * Duplicates are removed before the header is generated and the coloring does
		 * not create an entry in the header, only an annotation on an actual type is
		 * counted. This makes it possible to make a library of developers without
		 * then adding them all to the header.
		 * <p>
		 * {@see https://maven.apache.org/pom.html#Developers}
		 */
		@Retention(RetentionPolicy.CLASS)
		@Target({
				ElementType.ANNOTATION_TYPE, ElementType.TYPE
		})
		public @interface BundleDevelopers {
		
			/**
			 * The email address of the developer.
			 */
			String value();
		
			/**
			 * The display name of the developer. If not specified, the {@link #value()}
			 * is used.
			 */
			String name() default "";
		
			/**
			 * The roles this developer plays in the development.
			 */
			String[] roles() default {};
		
			/**
			 * The name of the organization where the developer works for.
			 */
			String organization() default "";
		
			/**
			 * The url of the organization where the developer works for.
			 */
			String organizationUrl() default "";
		
			/**
			 * Time offset in hours from UTC without Daylight savings
			 */
			int timezone() default 0;
		}
			