package aQute.remote.api;

import org.osgi.dto.DTO;
import org.osgi.framework.dto.ServiceReferenceDTO;

public class XSatisfiedReferenceDTO extends DTO {

	public String					name;
	public String					target;
	public String					objectClass;
	public ServiceReferenceDTO[]	serviceReferences;

}
