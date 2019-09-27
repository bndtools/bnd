package aQute.bnd.annotation.headers;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * The {@code Bundle-License} header provides an optional machine readable form
 * of license information. The purpose of this header is to automate some of the
 * license processing required by many organizations like for example license
 * acceptance before a bundle is used. The header is structured to provide the
 * use of unique license naming to merge acceptance requests, as well as links
 * to human readable information about the included licenses. This header is
 * purely informational for management agents and must not be processed by the
 * OSGi Framework.
 * <p>
 * The syntax for this header is as follows:
 *
 * <pre>
 * Bundle-License ::= ’&lt;&lt;EXTERNAL&gt;&gt;’ | ( license ( ’,’ license ) * )
 * license ::= name ( ’;’ license-attr )
 * license-attr ::= description | link
 * description ::= ’description’ ’=’ string
 * link ::= ’link’ ’=’ &lt;url&gt;
 * </pre>
 *
 * This header has the following attributes:
 * <ul>
 * <li>{@code name} – Provides a globally unique name for this license,
 * preferably world wide, but it should at least be unique with respect to the
 * other clauses. The magic name {@code <<EXTERNAL>>} is used to indicate that
 * this artifact does not contain any license information but that licensing
 * information is provided in some other way. This is also the default contents
 * of this header. Clients of this bundle can assume that licenses with the same
 * name refer to the same license. This can for example be used to minimize the
 * click through licenses. This name should be one of the identifiers defined by
 * <a href="https://spdx.org/licenses/">Software Package Data Exchange (SPDX)
 * License List</a>. Clients of this bundle can assume that licenses with the
 * same identifier refer to the same license. This can for example be used to
 * minimize the click through licenses. Alternatively, this name should be the
 * canonical URL of the license, it must not be localized by the translator.
 * This URL does not have to exist but must not be used for later versions of
 * the license. It is recommended to use URLs from
 * <a href="http://opensource.org/">Open Source Initiative</a>. Other licenses
 * should use the following structure, but this is not mandated:
 *
 * <pre>
 * http://&lt;domain-name&gt;/licenses/&lt;license-name&gt;-&lt;version&gt;.&lt;extension&gt;
 * </pre>
 *
 * <li>{@code description} – (optional) Provide the description of the license.
 * This is a short description that is usable in a list box on a UI to select
 * more information about the license.
 * <li>{@code link} – (optional) Provides a URL to a page that defines or
 * explains the license. If this link is absent, the name field is used for this
 * purpose. The URL is relative to the root of the bundle. That is, it is
 * possible to refer to a file inside the bundle.
 * </ul>
 * If the {@code Bundle-License} statement is absent, then this does not mean
 * that the bundle is not licensed. Licensing could be handled outside the
 * bundle and the {@code <<EXTERNAL>>} form should be assumed. This header is
 * informational and may not have any legal bearing. Consult a lawyer before
 * using this header to automate licensing processing.
 * <p>
 * A number of license annotations have been predefined. For example:
 * {@link aQute.bnd.annotation.licenses.Apache_2_0 Apache_2_0},
 * {@link aQute.bnd.annotation.licenses.BSD_2_Clause BSD_2_Clause},
 * {@link aQute.bnd.annotation.licenses.BSD_3_Clause BSD_3_Clause},
 * {@link aQute.bnd.annotation.licenses.CDDL_1_0 CDDL_1_0},
 * {@link aQute.bnd.annotation.licenses.EPL_1_0 EPL_1_0} ,
 * {@link aQute.bnd.annotation.licenses.EPL_2_0 EPL_2_0} ,
 * {@link aQute.bnd.annotation.licenses.GPL_2_0 GPL_2_0},
 * {@link aQute.bnd.annotation.licenses.GPL_3_0 GPL_3_0},
 * {@link aQute.bnd.annotation.licenses.LGPL_2_1 LGPL_2_1},
 * {@link aQute.bnd.annotation.licenses.MIT MIT}, and
 * {@link aQute.bnd.annotation.licenses.MPL_2_0 MPL_2_0}.
 */
@Documented
@Retention(RetentionPolicy.CLASS)
@Target({
	ElementType.PACKAGE, ElementType.TYPE
})
public @interface BundleLicense {
	/**
	 * The name of the license, should refer to
	 * <a href="https://spdx.org/licenses/">Software Package Data Exchange
	 * (SPDX) License List</a>
	 */
	String name();

	/**
	 * A short description of the license
	 */
	String description() default "";

	/**
	 * A URI to the license text. This maybe relative, in that case it is from
	 * the corresponding bundle.
	 */
	String link() default "";
}
