package aQute.bnd.annotation.headers;

import java.lang.annotation.*;

/**
 * The Bundle-License header provides an optional machine readable form of
 * license information. The purpose of this header is to automate some of the
 * license processing required by many organizations like for example license
 * acceptance before a bundle is used. The header is structured to provide the
 * use of unique license naming to merge acceptance requests, as well as links
 * to human readable infor- mation about the included licenses. This header is
 * purely informational for management agents and must not be processed by the
 * OSGi Framework.
 * <p>
 * The syntax for this header is as follows:
 * 
 * <pre>
 *    Bundle-License ::= ’<<EXTERNAL>>’ |
 *                         ( license ( ’,’ license ) * )
 *    license        ::= name ( ’;’ license-attr ) *
 *    license-attr   ::= description | link
 *    description    ::= ’description’ ’=’ string
 *    link           ::= ’link’ ’=’ <url>
 * </pre>
 * 
 * This header has the following attributes:
 * <ul>
 * <li>name – Provides a globally unique name for this license, preferably world
 * wide, but it should at least be unique with respect to the other clauses. The
 * magic name <<EXTERNAL>> is used to indicate that this artifact does not
 * contain any license information but that licensing information is provided in
 * some other way. This is also the default contents of this header.
 * <li>Clients of this bundle can assume that licenses with the same name refer
 * to the same license. This can for example be used to minimize the click
 * through licenses. This name should be the canonical URL of the license, it
 * must not be localized by the translator. This URL does not have to exist but
 * must not be used for later versions of the license. It is recommended to use
 * URLs from <a href="http://opensource.org/">Open Source Initiative</a>. Other
 * licenses should use the following structure, but this is not mandated:
 * 
 * <pre>
 *      http://<domain-name>/licenses/
 *            <license-name>-<version>.<extension>
 * </pre>
 * <li>description – (optional) Provide the description of the license. This is
 * a short description that is usable in a list box on a UI to select more
 * information about the license.
 * <li>link – (optional) Provide a URL to a page that defines or explains the
 * license. If this link is absent, the name field is used for this purpose. The
 * URL is relative to the root of the bundle. That is, it is possible to refer
 * to a file inside the bundle.
 * </ul>
 * If the Bundle-License statement is absent, then this does not mean that the
 * bundle is not licensed. Licensing could be handled outside the bundle and the
 * {@code <<EXTERNAL>>} form should be assumed. This header is informational and
 * may not have any legal bearing. Consult a lawyer before using this header to
 * automate licensing processing.
 * <p>
 * A number of licenses have been predefined, {@link ASL_2_0},
 * {@link BSD_2_Clause}, {@link BSD_3_Clause}, {@link CDDL_1_0}, {@link EPL_1_0}, {@link GPL_2_0}, {@link GPL_3_0}, {@link LGPL_2_1}, {@link MIT_1_0},
 * {@link MPL_2_0}.
 */
@Retention(RetentionPolicy.CLASS)
@Target({
		ElementType.ANNOTATION_TYPE, ElementType.TYPE
})
public @interface BundleLicense {
	/**
	 * The name of the license, should refer to <a
	 * href="http://opensource.org/">Open Source Initiative</a>
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
