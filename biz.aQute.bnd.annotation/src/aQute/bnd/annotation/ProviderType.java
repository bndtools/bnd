package aQute.bnd.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * <p>
 * A change in a provider type (that is all except Consumer types) can be
 * changed with only (at minimum) a minor update to the package API version
 * number. This interface is similar to the Eclipse @noextend and @noimplement
 * annotations. OSGi specifications use the @noimplement annotation, see below.
 * </p>
 * <h2>Elaborate and Simple Explanation</h2>
 * <p>
 * There is a distinction between "provider implemented" and "consumer
 * implemented" interfaces, which is reflected in the {@link ProviderType} and
 * {@link ConsumerType} annotations respectively.
 * </p>
 * <p>
 * These annotations can be placed on an interface to enable the tooling to
 * detect the role of the interface and generate the correct import range.
 * </p>
 * <p>
 * Usually - to a developer - the role of the interface is clear: interfaces
 * that are merely call-backs or listeners are generally "consumer implemented".
 * </p>
 * <h2>Thought Experiment with ConfigurationAdmin</h2>
 * <p>
 * In the case of the ConfigurationAdmin specification: implementing
 * ManagedService or ManagedServiceFactory does not make you a provider. You
 * would have to implement ConfigurationAdmin in order to be a provider.
 * </p>
 * <p>
 * <b>Rule of thumb:</b> suppose a method would be added to
 * ManagedServiceFactory. How many bundles would be broken as a result? The
 * answer is "lots" and therefore ManagedServiceFactory is a consumer type.
 * However, if a method would be added to ConfigurationAdmin then only a very
 * small number of bundles would be broken; these are the providers of the
 * ConfigurationAdmin API, e.g. org.apache.felix.configadmin and so on.
 * </p>
 * <h2>Relation to OSGi Specifications and Javadoc</h2>
 * <p>
 * At OSGi, the javadoc tag @noimplement is used to mark "provider implemented"
 * interfaces. Interfaces not marked @noimplement are "consumer implemented"
 * interfaces.
 * </p>
 * <p>
 * So, for example, the ConfigurationAdmin interface is marked @noimplement
 * while the ManagedService and ManagedServiceFactory interfaces are not.
 * </p>
 * <p>
 * In the specification, you will see "No Implement Consumers of this API must
 * not implement this interface" for @noimplement marked interfaces.
 * </p>
 * <p>
 * In the html javadoc, you will see "Consumers of this API must not implement
 * this interface" for @noimplement marked interfaces.
 * </p>
 */
@Documented
@Retention(RetentionPolicy.CLASS)
@Target(ElementType.TYPE)
public @interface ProviderType {

}
