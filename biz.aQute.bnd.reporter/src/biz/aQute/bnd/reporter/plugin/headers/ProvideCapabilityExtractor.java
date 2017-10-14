package biz.aQute.bnd.reporter.plugin.headers;

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Map.Entry;

import aQute.bnd.header.Attrs;
import aQute.bnd.header.Attrs.Type;
import aQute.bnd.header.Parameters;
import aQute.bnd.osgi.Constants;
import aQute.bnd.osgi.Jar;
import aQute.lib.tag.Tag;
import aQute.service.reporter.Reporter;
import biz.aQute.bnd.reporter.generator.ManifestHelper;

public class ProvideCapabilityExtractor extends HeaderExtractor {

	final private static String HEADER_TAG = "provide-capability";
	final private static String NAMESPACE_TAG = "namespace";
	final private static String USE_TAG = "use";
	final private static String EFFECTIVE_TAG = "effective";
	final private static String ARBITRARY_DIRECTIVE_TAG = "arbitrary-directive";
	final private static String TYPED_ATTRIBUTE_TAG = "typed-attribute";
	final private static String NAME_TAG = "name";
	final private static String VALUE_TAG = "value";
	final private static String TYPE_TAG = "type";
	final private static String MULTI_VALUE_TAG = "multi-value";

	@Override
	public List<Tag> extract(final ManifestHelper manifest, final Jar jar, final Reporter reporter) {
		final List<Tag> result = new LinkedList<>();
		final Parameters header = manifest.getHeader(Constants.PROVIDE_CAPABILITY, false);
		for (final Entry<String, Attrs> entry : header.entrySet()) {
			final Tag tag = new Tag(HEADER_TAG);

			tag.addContent(new Tag(NAMESPACE_TAG, cleanKey(entry.getKey())));

			if (entry.getValue().containsKey("uses:")) {
				for (final String c : entry.getValue().get("uses:").split(",")) {
					tag.addContent(new Tag(USE_TAG, c.trim()));
				}
			}

			if (entry.getValue().containsKey("effective:")) {
				tag.addContent(new Tag(EFFECTIVE_TAG, entry.getValue().get("effective:")));
			} else {
				tag.addContent(new Tag(EFFECTIVE_TAG, "resolve"));
			}

			final Attrs attribute = new Attrs(entry.getValue());
			attribute.remove("uses:");
			attribute.remove("effective:");

			for (final Entry<String, String> a : attribute.entrySet()) {
				if (a.getKey().endsWith(":")) {
					final Tag aTag = new Tag(ARBITRARY_DIRECTIVE_TAG);
					aTag.addContent(new Tag(NAME_TAG, a.getKey().substring(0, a.getKey().length() - 1)));
					aTag.addContent(new Tag(VALUE_TAG, a.getValue()));
					tag.addContent(aTag);
				}
			}

			for (final Entry<String, String> a : attribute.entrySet()) {
				if (!a.getKey().endsWith(":")) {
					final Object val = attribute.getTyped(a.getKey());
					final Tag pTag = new Tag(TYPED_ATTRIBUTE_TAG);

					pTag.addContent(new Tag(NAME_TAG, a.getKey()));

					if (val != null) {
						if (attribute.getType(a.getKey()) == Type.DOUBLE) {
							pTag.addContent(new Tag(VALUE_TAG, val.toString()));
							pTag.addContent(new Tag(TYPE_TAG, "Double"));
							pTag.addContent(new Tag(MULTI_VALUE_TAG, false));
						} else if (attribute.getType(a.getKey()) == Type.LONG) {
							pTag.addContent(new Tag(VALUE_TAG, val.toString()));
							pTag.addContent(new Tag(TYPE_TAG, "Long"));
							pTag.addContent(new Tag(MULTI_VALUE_TAG, false));
						} else if (attribute.getType(a.getKey()) == Type.STRING) {
							pTag.addContent(new Tag(VALUE_TAG, val.toString()));
							pTag.addContent(new Tag(TYPE_TAG, "String"));
							pTag.addContent(new Tag(MULTI_VALUE_TAG, false));
						} else if (attribute.getType(a.getKey()) == Type.VERSION) {
							pTag.addContent(new Tag(VALUE_TAG, val.toString()));
							pTag.addContent(new Tag(TYPE_TAG, "Version"));
							pTag.addContent(new Tag(MULTI_VALUE_TAG, false));
						} else if (attribute.getType(a.getKey()) == Type.DOUBLES) {
							for (final Object v : (Collection<?>) val) {
								pTag.addContent(new Tag(VALUE_TAG, v.toString()));
							}
							pTag.addContent(new Tag(TYPE_TAG, "Double"));
							pTag.addContent(new Tag(MULTI_VALUE_TAG, true));
						} else if (attribute.getType(a.getKey()) == Type.LONGS) {
							for (final Object v : (Collection<?>) val) {
								pTag.addContent(new Tag(VALUE_TAG, v.toString()));
							}
							pTag.addContent(new Tag(TYPE_TAG, "Long"));
							pTag.addContent(new Tag(MULTI_VALUE_TAG, true));
						} else if (attribute.getType(a.getKey()) == Type.STRINGS) {
							for (final Object v : (Collection<?>) val) {
								pTag.addContent(new Tag(VALUE_TAG, v.toString()));
							}
							pTag.addContent(new Tag(TYPE_TAG, "String"));
							pTag.addContent(new Tag(MULTI_VALUE_TAG, true));
						} else if (attribute.getType(a.getKey()) == Type.VERSIONS) {
							for (final Object v : (Collection<?>) val) {
								pTag.addContent(new Tag(VALUE_TAG, v.toString()));
							}
							pTag.addContent(new Tag(TYPE_TAG, "Version"));
							pTag.addContent(new Tag(MULTI_VALUE_TAG, true));
						} else {
							pTag.addContent(new Tag(VALUE_TAG, val.toString()));
							pTag.addContent(new Tag(TYPE_TAG, "String"));
							pTag.addContent(new Tag(MULTI_VALUE_TAG, false));
						}

						tag.addContent(pTag);
					}
				}
			}
			result.add(tag);
		}
		return result;
	}
}
