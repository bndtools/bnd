package aQute.bnd.runtime.facade;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.osgi.dto.DTO;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.service.coordinator.Coordination;
import org.osgi.service.coordinator.Coordinator;

import aQute.bnd.runtime.api.SnapshotProvider;

public class CoordinatorFacade implements SnapshotProvider {

	final BundleContext context;

	public static class CoordinationDTO extends DTO {

		public long						id;
		public String					name;
		public String					failure;
		public boolean					isTerminated;
		public List<String>				participants	= new ArrayList<>();
		public Map<Class<?>, Object>	variables;
		public String					thread;
		public long						bundle;
		public CoordinationDTO			enclosing;
	}

	static class CoordinatorDTO extends DTO {
		public List<CoordinationDTO>	coordinations	= new ArrayList<>();
		public List<String>				errors			= new ArrayList<>();
	}

	public CoordinatorFacade(BundleContext context) {
		this.context = context;
	}

	public CoordinatorDTO getCoordination() {
		CoordinatorDTO dto = new CoordinatorDTO();
		Coordinator coordinator = getCoordinator();
		if (coordinator == null) {
			dto.errors.add("No Coordination service found");
		} else {
			Collection<Coordination> coordinations = coordinator.getCoordinations();
			for (Coordination c : coordinations) {
				CoordinationDTO coordination = getCoordinationDTO(c);
				dto.coordinations.add(coordination);
				// dto.coordinations.add(asBean(Coordination.class, c));
			}
		}
		return dto;
	}

	private CoordinationDTO getCoordinationDTO(Coordination c) {

		CoordinationDTO coordination = new CoordinationDTO();

		coordination.bundle = c.getBundle()
			.getBundleId();
		if (c.getEnclosingCoordination() != null)
			coordination.enclosing = getCoordinationDTO(c.getEnclosingCoordination());
		if (c.getFailure() != null)
			coordination.failure = c.getFailure()
				.toString();

		if (c.getFailure() != null)
			coordination.failure = c.getFailure()
				.toString();

		coordination.id = c.getId();
		coordination.isTerminated = c.isTerminated();
		coordination.name = c.getName();
		coordination.participants = c.getParticipants()
			.stream()
			.map(Object::toString)
			.collect(Collectors.toList());
		coordination.thread = c.getThread()
			.toString();
		coordination.variables = new HashMap<>(c.getVariables());
		return coordination;
	}

	private Coordinator getCoordinator() {
		ServiceReference<Coordinator> reference = context.getServiceReference(Coordinator.class);
		if (reference == null)
			return null;
		return context.getService(reference);
	}

	@Override
	public void close() throws IOException {}

	@Override
	public Object getSnapshot() throws Exception {
		return getCoordination();
	}

}
