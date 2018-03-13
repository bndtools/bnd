package aQute.remote.api;

import org.osgi.framework.dto.BundleDTO;
import org.osgi.framework.dto.ServiceReferenceDTO;

import aQute.bnd.util.dto.DTO;

/**
 * An event sent from the agent to the supervisor.
 */
public class Event extends DTO {
	public enum Type {
		exit,
		framework
	}

	public Type					type;
	public int					code;
	public BundleDTO			bundle;
	public ServiceReferenceDTO	reference;
}
