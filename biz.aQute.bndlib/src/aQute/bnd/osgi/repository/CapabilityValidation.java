package aQute.bnd.osgi.repository;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import org.osgi.framework.Version;
import org.osgi.framework.namespace.BundleNamespace;
import org.osgi.framework.namespace.ExecutionEnvironmentNamespace;
import org.osgi.framework.namespace.HostNamespace;
import org.osgi.framework.namespace.IdentityNamespace;
import org.osgi.framework.namespace.NativeNamespace;
import org.osgi.framework.namespace.PackageNamespace;
import org.osgi.resource.Capability;

import aQute.bnd.osgi.resource.CapReqBuilder;

class CapabilityValidation {

	private static class Attribute<T> {
		final String	namespace;
		final String	name;
		final Function<Object, T>	check;

		Attribute(String namespace, String name, Function<Object, T> check) {
			this.namespace = namespace;
			this.name = name;
			this.check = check;
		}

		boolean validate(Map<String, ? super T> map) throws IllegalArgumentException {
			final boolean modified;
			Object value = map.get(name);

			final T checked;
			try {
				checked = check.apply(value);
			} catch (Exception e) {
				throw new IllegalArgumentException(
					MessageFormat.format("Capability in namespace {0} has invalid value for {1} \"{2}\": {3}",
						namespace, getClass().getSimpleName(), name, e.getMessage()));
			}

			if (checked != value) {
				map.put(name, checked);
				modified = true;
			} else {
				modified = false;
			}
			return modified;
		}
	}

	private static class Directive extends Attribute<String> {
		Directive(String namespace, String name, Function<Object, String> check) {
			super(namespace, name, check);
		}
	}

	private static final Map<String, List<Attribute<?>>>	VALIDATION_ATTRIBUTES	= new HashMap<>();
	private static final Map<String, List<Directive>>		VALIDATION_DIRECTIVES	= new HashMap<>();

	static {
		final Function<Object, String> mandatoryString = cast(String.class).andThen(CapabilityValidation::mandatory);

		// Identity Namespace
		{
			List<Attribute<?>> a = new LinkedList<>();
			a.add(new Attribute<>(IdentityNamespace.IDENTITY_NAMESPACE, IdentityNamespace.IDENTITY_NAMESPACE,
				mandatoryString));
			a.add(new Attribute<>(IdentityNamespace.IDENTITY_NAMESPACE, IdentityNamespace.CAPABILITY_TYPE_ATTRIBUTE,
				cast(String.class).andThen(s -> s == null ? "osgi.bundle" : s)));
			a.add(new Attribute<>(IdentityNamespace.IDENTITY_NAMESPACE, IdentityNamespace.CAPABILITY_VERSION_ATTRIBUTE,
				CapabilityValidation::defaultingVersion));

			VALIDATION_ATTRIBUTES.put(IdentityNamespace.IDENTITY_NAMESPACE, a);
		}

		// EE Namespace
		{
			List<Attribute<?>> a = new LinkedList<>();
			a.add(new Attribute<>(ExecutionEnvironmentNamespace.EXECUTION_ENVIRONMENT_NAMESPACE,
				ExecutionEnvironmentNamespace.EXECUTION_ENVIRONMENT_NAMESPACE, mandatoryString));
			a.add(new Attribute<>(ExecutionEnvironmentNamespace.EXECUTION_ENVIRONMENT_NAMESPACE,
				ExecutionEnvironmentNamespace.CAPABILITY_VERSION_ATTRIBUTE, CapabilityValidation::defaultingVersion));

			VALIDATION_ATTRIBUTES.put(ExecutionEnvironmentNamespace.EXECUTION_ENVIRONMENT_NAMESPACE, a);
		}

		// Package Namespace
		{
			List<Attribute<?>> a = new LinkedList<>();
			a.add(new Attribute<>(PackageNamespace.PACKAGE_NAMESPACE, PackageNamespace.PACKAGE_NAMESPACE,
				mandatoryString));
			a.add(new Attribute<>(PackageNamespace.PACKAGE_NAMESPACE, PackageNamespace.CAPABILITY_VERSION_ATTRIBUTE,
				CapabilityValidation::defaultingVersion));
			a.add(new Attribute<>(PackageNamespace.PACKAGE_NAMESPACE,
				PackageNamespace.CAPABILITY_BUNDLE_SYMBOLICNAME_ATTRIBUTE, mandatoryString));
			a.add(new Attribute<>(PackageNamespace.PACKAGE_NAMESPACE,
				PackageNamespace.CAPABILITY_BUNDLE_VERSION_ATTRIBUTE, CapabilityValidation::defaultingVersion));

			VALIDATION_ATTRIBUTES.put(PackageNamespace.PACKAGE_NAMESPACE, a);
		}

		// Bundle Namespace
		{
			List<Attribute<?>> a = new LinkedList<>();
			a.add(new Attribute<>(BundleNamespace.BUNDLE_NAMESPACE, BundleNamespace.BUNDLE_NAMESPACE,
				mandatoryString));
			a.add(new Attribute<>(BundleNamespace.BUNDLE_NAMESPACE, BundleNamespace.CAPABILITY_BUNDLE_VERSION_ATTRIBUTE,
				CapabilityValidation::defaultingVersion));

			VALIDATION_ATTRIBUTES.put(BundleNamespace.BUNDLE_NAMESPACE, a);
		}

		// Bundle Namespace
		{
			List<Attribute<?>> a = new LinkedList<>();
			a.add(new Attribute<>(HostNamespace.HOST_NAMESPACE, HostNamespace.HOST_NAMESPACE, mandatoryString));
			a.add(new Attribute<>(HostNamespace.HOST_NAMESPACE, HostNamespace.CAPABILITY_BUNDLE_VERSION_ATTRIBUTE,
				CapabilityValidation::defaultingVersion));

			VALIDATION_ATTRIBUTES.put(HostNamespace.HOST_NAMESPACE, a);
		}

		// Native Namespace
		{
			List<Attribute<?>> a = new LinkedList<>();
			a.add(new Attribute<>(NativeNamespace.NATIVE_NAMESPACE, NativeNamespace.CAPABILITY_OSNAME_ATTRIBUTE,
				collectionLike(String.class).andThen(CapabilityValidation::mandatory)));
			a.add(new Attribute<>(NativeNamespace.NATIVE_NAMESPACE, NativeNamespace.CAPABILITY_OSVERSION_ATTRIBUTE,
				CapabilityValidation::defaultingVersion));
			a.add(new Attribute<>(NativeNamespace.NATIVE_NAMESPACE, NativeNamespace.CAPABILITY_PROCESSOR_ATTRIBUTE,
				collectionLike(String.class).andThen(CapabilityValidation::mandatory)));
			a.add(new Attribute<>(NativeNamespace.NATIVE_NAMESPACE, NativeNamespace.CAPABILITY_LANGUAGE_ATTRIBUTE,
				mandatoryString));

			VALIDATION_ATTRIBUTES.put(NativeNamespace.NATIVE_NAMESPACE, a);
		}
	}

	Capability validate(Capability original) throws Exception {
		final Map<String, Object> attribs = new HashMap<>(original.getAttributes());
		final Map<String, String> directives = new HashMap<>(original.getDirectives());
		boolean modified = false;

		// Check attribs
		List<Attribute<?>> validationAttribList = VALIDATION_ATTRIBUTES.get(original.getNamespace());
		if (validationAttribList != null) {
			for (Attribute<?> validationAttrib : validationAttribList) {
				modified |= validationAttrib.validate(attribs);
			}
		}
		// Check directives
		List<Directive> validationDirectiveList = VALIDATION_DIRECTIVES.get(original.getNamespace());
		if (validationDirectiveList != null) {
			for (Directive validationDirective : validationDirectiveList) {
				modified |= validationDirective.validate(directives);
			}
		}

		final Capability result;
		if (modified) {
			CapReqBuilder builder = new CapReqBuilder(original.getNamespace());
			builder.addAttributes(attribs);
			builder.addDirectives(directives);

			result = original.getResource() != null ? builder.setResource(original.getResource())
				.buildCapability() : builder.buildSyntheticCapability();
		} else {
			result = original;
		}
		return result;
	}

	private static final <T> Function<Object, T> cast(Class<T> type) {
		return type::cast;
	}

	private static final <T> T mandatory(T t) {
		if (t == null)
			throw new IllegalArgumentException("Missing mandatory value");
		return t;
	}

	private static final Version defaultingVersion(Object o) {
		final Version result;
		if (o == null) {
			result = Version.emptyVersion;
		} else if (o instanceof Version) {
			result = (Version) o;
		} else if (o instanceof aQute.bnd.version.Version) {
			result = new Version(o.toString());
		} else if (o instanceof String) {
			result = new Version((String) o);
		} else {
			throw new IllegalArgumentException("Cannot convert to Version from type " + o.getClass()
				.getName());
		}
		return result;
	}

	private static final <T> Function<Object, Collection<T>> collectionLike(Class<T> componentType) {
		return o -> {
			final Collection<T> coll;
			if (o == null) {
				coll = null;
			} else if (o.getClass()
				.isArray()) {
				if (componentType.isAssignableFrom(o.getClass()
					.getComponentType())) {
					@SuppressWarnings("unchecked")
					T[] array = (T[]) o;
					coll = Arrays.asList(array);
				} else {
					throw new IllegalArgumentException("Array contains incorrect type: " + o.getClass()
						.getComponentType() + ", expected: " + componentType.getName());
				}
			} else if (o instanceof Collection) {
				Collection<?> unknownColl = (Collection<?>) o;
				if (unknownColl.isEmpty()) {
					coll = Collections.emptyList();
				} else {
					coll = new ArrayList<>(unknownColl.size());
					for (Object member : unknownColl) {
						if (componentType.isInstance(member)) {
							@SuppressWarnings("unchecked")
							T t = (T) member;
							coll.add(t);
						} else {
							String memberLabel = member != null ? member.getClass()
								.getName() : "<null>";
							// Automatic code formatting SUCKS
							throw new IllegalArgumentException(
								"Collection contains object of incorrect type: " + memberLabel + ", expected: "
									+ componentType.getClass()
										.getName());
						}
					}
				}
			} else if (componentType.isInstance(o)) {
				@SuppressWarnings("unchecked")
				T singleton = (T) o;
				coll = Collections.singleton(singleton);
			} else {
				throw new IllegalArgumentException("Could not convert object of type " + o.getClass()
					.getName() + " to a collection of " + componentType.getName());
			}
			return coll;
		};
	}
}
