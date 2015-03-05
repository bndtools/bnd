package aQute.remote.api;

import org.osgi.dto.DTO;
import org.osgi.framework.dto.BundleDTO;
import org.osgi.framework.dto.ServiceReferenceDTO;

public class Event extends DTO {
	public enum Type {
		exit, framework
	}

	public Type type;
	public int code;
	public BundleDTO bundle;
	public ServiceReferenceDTO reference;
}
