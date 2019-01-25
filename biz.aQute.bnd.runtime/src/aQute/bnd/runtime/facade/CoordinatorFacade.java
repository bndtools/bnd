package aQute.bnd.runtime.facade;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.osgi.dto.DTO;
import org.osgi.framework.BundleContext;
import org.osgi.service.coordinator.Coordination;
import org.osgi.service.coordinator.Coordinator;
import org.osgi.util.tracker.ServiceTracker;

import aQute.bnd.runtime.api.SnapshotProvider;

public class CoordinatorFacade implements SnapshotProvider {

	final ServiceTracker<Coordinator, Coordinator>	coordinatorTracker;
	final BundleContext								context;

	static class CoordinatorDTO extends DTO {
		public List<Map<String, Object>>	coordinations	= new ArrayList<>();
		public List<String>					errors			= new ArrayList<>();
	}

	public CoordinatorFacade(BundleContext context) {
		this.context = context;
		coordinatorTracker = new ServiceTracker<>(context, Coordinator.class, null);
		coordinatorTracker.open();
	}

	public CoordinatorDTO getCoordination() {
		CoordinatorDTO dto = new CoordinatorDTO();
		Coordinator coordinator = coordinatorTracker.getService();
		if (coordinator == null) {
			dto.errors.add("No Coordination service found");
		} else {
			if (coordinatorTracker.size() > 1) {
				dto.errors
					.add("Multiple Coordination services found, using first " + Arrays.toString(coordinatorTracker.getServices()));
			}
			Collection<Coordination> coordinations = coordinator.getCoordinations();
			for (Coordination c : coordinations) {
				// dto.coordinations.add(asBean(Coordination.class, c));
			}
		}
		return dto;
	}

	@Override
	public void close() throws IOException {
		coordinatorTracker.close();
	}

	@Override
	public Object getSnapshot() throws Exception {
		return getCoordination();
	}

}
