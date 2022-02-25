package aQute.bnd.osgi;

import java.util.function.Predicate;

import aQute.bnd.header.Parameters;

/**
 * Defines a number of attribute classes. Attributes are set on {@link Packages}
 * and {@link Parameters}. The primary purpose is to print these attributes in
 * the manifest. However, over time a number of use cases made the code use the
 * attributes to control bnd processing and/or are actually set by bnd. This
 * enum provides access to these classes. Each enum value is a predicate that
 * can test a key.
 */
public enum AttributeClasses implements Predicate<String> {

	/**
	 * Attributes that would show up in the manifest.
	 */
	MANIFEST {
		@Override
		public boolean test(String key) {
			return manifest.test(key);
		}
	},
	/**
	 * Attributes set and used by bnd code to maintain inernal correlations.
	 * These attributes are never set by users. For example,
	 * {@value Constants#INTERNAL_BUNDLESYMBOLICNAME_DIRECTIVE} These attributes
	 * must not end up in the manifest.
	 */
	INTERNAL {
		@Override
		public boolean test(String key) {
			return key.startsWith(Constants.INTERNAL_PREFIX);
		}
	},
	/**
	 * Attributes set by the user but solely with the purpose to control bnd
	 * processing. For example {@value Constants#SPLIT_PACKAGE_DIRECTIVE}. These
	 * attributes must not end up in the manifest.
	 */
	BND_USE {
		@Override
		public boolean test(String key) {
			return Constants.BND_USE_ATTRIBUTES.contains(key);
		}
	};

	private final static Predicate<String> manifest = INTERNAL.or(BND_USE)
		.negate();
}
