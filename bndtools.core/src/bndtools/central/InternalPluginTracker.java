package bndtools.central;

import java.lang.reflect.Method;
import java.util.Formatter;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.TreeMap;
import java.util.stream.Collectors;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleEvent;
import org.osgi.framework.wiring.BundleRevision;
import org.osgi.resource.Capability;
import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;
import org.osgi.util.tracker.BundleTracker;

import aQute.bnd.annotation.plugin.InternalPluginDefinition;
import aQute.bnd.annotation.plugin.InternalPluginNamespace;
import aQute.bnd.osgi.Processor;

public class InternalPluginTracker extends BundleTracker<List<InternalPluginDefinition>> {

	public InternalPluginTracker(BundleContext context) {
		super(context, Bundle.ACTIVE + Bundle.STARTING + Bundle.RESOLVED, null);
	}

	@Override
	public List<InternalPluginDefinition> addingBundle(Bundle bundle, BundleEvent event) {
		BundleRevision revision = bundle.adapt(BundleRevision.class);
		List<Capability> capabilities = revision.getCapabilities(InternalPluginNamespace.NAMESPACE);
		if (capabilities.isEmpty())
			return null;

		return capabilities.stream()
			.map(cap -> capToDef(bundle, cap))
			.filter(Objects::nonNull)
			.collect(Collectors.toList());
	}

	private InternalPluginDefinition capToDef(Bundle bundle, Capability cap) {

		try {
			String name = (String) cap.getAttributes()
				.get(InternalPluginNamespace.NAME_A);
			String key = (String) cap.getAttributes()
				.get(InternalPluginNamespace.IMPLEMENTATION_A);

			Class<?> implementation = name == null ? null : bundle.loadClass(key);
			key = (String) cap.getAttributes()
				.get(InternalPluginNamespace.PARAMETERS_A);
			Class<?> configuration = key == null ? null : bundle.loadClass(key);

			Object value = cap.getAttributes()
				.get("hide");
			boolean hide = value != null && Processor.isTrue(value.toString());

			return new InternalPluginDefinition() {

				@Override
				public String getTemplate() {
					return InternalPluginTracker.getTemplate(name, implementation, configuration);
				}

				@Override
				public String getName() {
					return name;
				}

				@Override
				public Class<?> getImplementation() {
					return implementation;
				}

				@Override
				public Optional<Class<?>> getParameters() {
					return Optional.ofNullable(configuration);
				}

				@Override
				public boolean isHidden() {
					return hide;
				}
			};
		} catch (ClassNotFoundException e) {
			return null;
		}
	}

	static String getTemplate(String name, Class<?> implementation, Class<?> configuration) {
		try (Formatter sb = new Formatter()) {
			ObjectClassDefinition od = configuration != null ? configuration.getAnnotation(ObjectClassDefinition.class)
				: null;

			Map<String, Method> used = new TreeMap<>();
			if (configuration != null) {
				for (Method m : configuration.getMethods()) {

					if (m.getDeclaringClass() != configuration)
						continue;

					Deprecated deprecated = m.getAnnotation(Deprecated.class);
					if (deprecated != null)
						continue;

					if (used.containsKey(m.getName()))
						continue;
					used.put(m.getName(), m);
				}
			}

			sb.format("%s", implementation.getName());

			for (Method m : used.values()) {
				String description = "";
				String deflt = "";
				boolean required = true;

				AttributeDefinition ad = m.getAnnotation(AttributeDefinition.class);
				if (ad != null) {
					description = ad.description();
					deflt = ad.defaultValue()[0];
					required = ad.required();
				}

				sb.format(";%s = \"%s%s %s %s\"", m.getName(), required ? "!" : "?", deflt, m.getReturnType()
					.getSimpleName(), description);
			}
			return sb.toString();
		}
	}
}
