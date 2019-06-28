package aQute.bnd.runtime.facade;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.osgi.dto.DTO;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.osgi.service.component.runtime.ServiceComponentRuntime;
import org.osgi.service.component.runtime.dto.ComponentConfigurationDTO;
import org.osgi.service.component.runtime.dto.ComponentDescriptionDTO;
import org.osgi.service.component.runtime.dto.ReferenceDTO;
import org.osgi.service.component.runtime.dto.SatisfiedReferenceDTO;
import org.osgi.service.component.runtime.dto.UnsatisfiedReferenceDTO;

import aQute.bnd.runtime.api.SnapshotProvider;

public class ServiceComponentRuntimeFacade implements SnapshotProvider {
	static AtomicLong		templateCounter	= new AtomicLong();
	static Map<ID, Long>	templateMap		= new HashMap<>();

	final BundleContext		context;

	public static class TemplateDTO extends DTO {
		public long									id;
		public String								name;
		public long									bundleId;
		public String								factory;
		public String								scope;
		public String								implementationClass;
		public boolean								defaultEnabled;
		public boolean								immediate;
		public String[]								serviceInterfaces;
		public Map<String, Object>					properties;
		public Map<String, TemplateReferenceDTO>	references	= new LinkedHashMap<>();
		public String								activate;
		public String								deactivate;
		public String								modified;
		public String								configurationPolicy;
		public String[]								configurationPid;
		public boolean								isEnabled;
	}

	public static class TemplateReferenceDTO extends DTO {
		public String	name;
		public String	service;
		public String	cardinality;
		public String	policy;
		public String	policyOption;
		public String	target;
		public String	bind;
		public String	unbind;
		public String	updated;
		public String	field;
		public String	fieldOption;
		public String	scope;

	}

	public static class InstanceDTO extends DTO {
		public long									id;
		public long									templateId;
		public long									serviceId;
		public int									state;
		public Map<String, Object>					properties;
		public Map<String, InstanceReferenceDTO>	references	= new LinkedHashMap<>();
	}

	public static class InstanceReferenceDTO extends DTO {
		public boolean		satisfied;
		public String		name;
		public String		target;
		public List<Long>	boundedServiceIds	= new ArrayList<>();
		public List<Long>	candidateServiceIds	= new ArrayList<>();
		public List<Long>	hiddenServiceIds	= new ArrayList<>();
	}

	public static class ScrDTO extends DTO {
		public Map<Long, TemplateDTO>	templates	= new TreeMap<>();
		public Map<Long, InstanceDTO>	instances	= new TreeMap<>();
		public List<String>				errors		= new ArrayList<>();
	}

	public ServiceComponentRuntimeFacade(BundleContext context) {
		this.context = context;
	}

	public ScrDTO getDTO() throws InvalidSyntaxException {
		ScrDTO scrdto = new ScrDTO();
		ServiceComponentRuntime service = getSCR(scrdto);
		if (service == null)
			return scrdto;

		for (ComponentDescriptionDTO description : service.getComponentDescriptionDTOs()) {
			TemplateDTO template = new TemplateDTO();
			template.name = description.name;
			template.bundleId = description.bundle.id;

			template.id = toId(template.name, template.bundleId);
			template.factory = description.factory;
			template.scope = description.scope;
			template.immediate = description.immediate;
			template.implementationClass = description.implementationClass;
			template.defaultEnabled = description.defaultEnabled;
			template.serviceInterfaces = description.serviceInterfaces;
			template.properties = description.properties;
			template.references = Stream.of(description.references)
				.map(ServiceComponentRuntimeFacade::convert)
				.collect(Collectors.toMap(r -> r.name, r -> r));
			template.activate = description.activate;
			template.deactivate = description.deactivate;
			template.modified = description.modified;
			template.configurationPolicy = description.configurationPolicy;
			template.configurationPid = description.configurationPid;
			template.isEnabled = service.isComponentEnabled(description);

			for (ComponentConfigurationDTO configuration : service.getComponentConfigurationDTOs(description)) {
				InstanceDTO instance = new InstanceDTO();

				instance.templateId = template.id;
				instance.id = configuration.id;
				instance.properties = configuration.properties;
				instance.state = configuration.state;

				ServiceReference<?>[] allServiceReferences = context.getAllServiceReferences((String) null,
					String.format("(component.id=%s)", instance.id));
				if (allServiceReferences != null && allServiceReferences.length == 1) {
					instance.serviceId = (Long) allServiceReferences[0].getProperty(Constants.SERVICE_ID);
				} else {
					instance.serviceId = -1;
				}

				instance.references = Stream.of(configuration.satisfiedReferences)
					.map((SatisfiedReferenceDTO sr) -> {
						InstanceReferenceDTO ird = new InstanceReferenceDTO();
						ird.name = sr.name;
						ird.satisfied = true;
						ird.target = sr.target;
						ird.boundedServiceIds = Stream.of(sr.boundServices)
							.map(b -> b.id)
							.collect(Collectors.toList());

						// TODO candidates and hidden
						return ird;
					})
					.collect(Collectors.toMap(x -> x.name, x -> x));

				instance.references.putAll(Stream.of(configuration.unsatisfiedReferences)
					.map((UnsatisfiedReferenceDTO sr) -> {
						InstanceReferenceDTO ird = new InstanceReferenceDTO();
						ird.name = sr.name;
						ird.satisfied = false;
						ird.target = sr.target;
						ird.boundedServiceIds = Stream.of(sr.targetServices)
							.map(b -> b.id)
							.collect(Collectors.toList());

						// TODO candidates and hidden
						return ird;
					})
					.collect(Collectors.toMap(x -> x.name, x -> x)));

				scrdto.instances.put(instance.id, instance);
			}
			scrdto.templates.put(template.id, template);
		}

		return scrdto;
	}

	private ServiceComponentRuntime getSCR(ScrDTO scrdto) {
		ServiceReference<ServiceComponentRuntime> ref = context.getServiceReference(ServiceComponentRuntime.class);
		if (ref == null) {
			scrdto.errors.add("No ServiceComponentRuntime service visible (getting ref)");
			return null;
		}

		ServiceComponentRuntime service = context.getService(ref);
		if (service == null) {
			scrdto.errors.add("No ServiceComponentRuntime service visible (get service) " + ref);
			return null;
		}
		return service;
	}

	public static long toId(String name, long bundle) {
		return templateMap.computeIfAbsent(new ID(name, bundle), x -> templateCounter.incrementAndGet());
	}

	static TemplateReferenceDTO convert(ReferenceDTO dto) {
		TemplateReferenceDTO ref = new TemplateReferenceDTO();
		ref.bind = dto.bind;
		ref.unbind = dto.unbind;
		ref.cardinality = dto.cardinality;
		ref.field = dto.field;
		ref.fieldOption = dto.fieldOption;
		ref.name = dto.name;
		ref.scope = dto.scope;
		ref.service = dto.interfaceName;
		ref.target = dto.target;
		ref.updated = dto.updated;
		return ref;
	}

	static class ID {
		final String	name;
		final long		bundle;

		ID(String name, long bundle) {
			this.name = name;
			this.bundle = bundle;
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + (int) (bundle ^ (bundle >>> 32));
			result = prime * result + ((name == null) ? 0 : name.hashCode());
			return result;
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			ID other = (ID) obj;
			if (bundle != other.bundle)
				return false;
			if (name == null) {
				if (other.name != null)
					return false;
			} else if (!name.equals(other.name))
				return false;
			return true;
		}
	}

	@Override
	public void close() throws IOException {}

	@Override
	public Object getSnapshot() throws Exception {
		return getDTO();
	}

}
