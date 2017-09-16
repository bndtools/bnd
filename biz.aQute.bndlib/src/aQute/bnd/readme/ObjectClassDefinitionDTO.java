package aQute.bnd.readme;

import java.util.List;
import java.util.Map;

import org.osgi.dto.DTO;

class ObjectClassDefinitionDTO extends DTO {

	public List<String>	pid;
	public List<String>	factoryPid;
	public Map<String,AttributeDefinitionDTO>	attributes;
}
