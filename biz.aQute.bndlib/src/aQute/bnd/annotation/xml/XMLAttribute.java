package aQute.bnd.annotation.xml;

import java.lang.annotation.*;

/**
 * Define an xml attribute extension annotation. In cooperation with an
 * XMLAttribute-aware annotation processor, such as the bnd plugins for DS and
 * (spec) metatype annotations, the values specified for the members of the
 * annotation will be added to the appropriate xml element in the document being
 * generated, using the namespace specified and attempting to use the prefix
 * specified. This will only occur if the generated document namespace matches
 * one of the embedIn strings. Supporting classes for this are in the
 * aQute.bnd.xmlattribute package. <p> For example: <pre>
 * &#064;XMLAttribute(namespace = &quot;org.foo.extensions.v1&quot;, prefix =
 * &quot;foo&quot;, embedIn = &quot;*&quot;)
 * &#064;Retention(RetentionPolicy.CLASS) &#064;Target(ElementType.TYPE)
 * &#064;interface OCDTestExtensions { boolean booleanAttr() default true; //
 * default provided, thus optional String stringAttr(); // no default, must be
 * specified Foo fooAttr(); } &#064;ObjectClassDefinition
 * &#064;OCDTestExtensions(stringAttr = &quot;ocd&quot;, fooAttr = Foo.A) public
 * static interface TestExtensions {} </pre> results in <pre> <metatype:MetaData
 * xmlns:metatype="http://www.osgi.org/xmlns/metatype/v1.3.0"
 * xmlns:foo="org.foo.extensions.v1"
 * localization="OSGI-INF/l10n/test.metatype.SpecMetatypeTest$TestExtensions">
 * <OCD id="test.metatype.SpecMetatypeTest$TestExtensions" name="Test metatype
 * spec metatype test test extensions" description="" foo:stringAttr="ocd"
 * foo:fooAttr="A"> ... </pre>
 */
@Retention(RetentionPolicy.CLASS)
@Target({
		ElementType.ANNOTATION_TYPE
})
public @interface XMLAttribute {

	String namespace();

	String prefix() default "ns";

	String[]embedIn() default {
			"*"
	};

}
