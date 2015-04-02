package aQute.remote.api;

import org.osgi.dto.*;
import org.osgi.framework.dto.*;

public class Event extends DTO {
	public enum Type {
		exit, framework
	}

	public Type					type;
	public int					code;
	public BundleDTO			bundle;
	public ServiceReferenceDTO	reference;
}
