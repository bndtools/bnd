package aQute.bnd.annotation.headers;


/**
 * This package contains a number of annotations that create or append headers
 * in the manifest. These annotations simplify maintaining these headers:
 * <ul>
 * <li>The annotations can be applied on the types that are generating their
 * need instead of maintaining them in the manifest
 * <li>Less errors because of the use of annotations
 * <li>Use of IDE features to track them or navigate
 * <ul>
 * The annotations provide the following features:
 * <ul>
 * <li>macros - use the bnd macro preprocessor for DNRY
 * <li>coloring - define custom annotations that encapsulate some headers
 * <li>No runtime dependencies since all annotations are build time.
 * <li>No duplicates
 * </ul>
 * <h2>Macros</h2> Any strings in the annotations are run through the bnd
 * preprocessor and can therefore use any of the myriad of bnd macros (except
 * system commands, for obvious reason they are excluded). As a convenience, a
 * number of local macros are set from the context:
 * <ul>
 * <li> {@code ${@package} – The package name
 * <li> {@code ${@class} – The class name to which this macro is applied to
 * <li> {@code ${@class-short} – The short class name to which this macro is
 * applied to
 * <li> {@code ${@version} – The package version if set
 * <li> {@code ${@frange;version[;isProvider]} – A
 * macro to create a filter expression on a version based on the semantic
 * versioning rules. Default is consumer, specify true for the isProvider to get
 * provider semantics.
 * <h2>Coloring</h2> Annotations can only be applied once, making it impossible
 * to add for example two Provide-Capability headers on the same type. It also
 * would become unreadable quickly. The advised way to use most of these
 * annotation headers is therefore through 'annotation coloring'. These header
 * annotations should be applied to custom annotations that represents the
 * 'thing'. This is clearly represented in the BundleLicense custom annotations
 * like for example the {@link ASL_2_0} annotation. This annotation can be
 * applied to any type and will automatically then create the appropriate
 * clauses.
 * <p>
 * For example:
 * 
 * <pre>
 *   public class Webserver {
 *   	@RequireCapability(ns="osgi.extender", name="webserver", version="${@version}")
 *      @interface Require {}
 *      
 *   	@ProvideCapability(ns="osgi.extender", filter="(&(osgi.extender=webserver)${frange;${@version}))")
 *      @interface Provide {}
 * </pre>
 * 
 * This resource can now be stored in a library to be used by others. If a
 * component now wants to depend this resource, it can declare its component as
 * follows:
 * 
 * <pre>
 *   @Webserver.Require
 *   public class MyResource {
 *   ...
 *   }
 * </pre>
 */
public class About {

}
