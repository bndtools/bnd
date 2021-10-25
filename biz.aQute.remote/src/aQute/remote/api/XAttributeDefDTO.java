package aQute.remote.api;

import java.util.List;

import org.osgi.dto.DTO;

public class XAttributeDefDTO extends DTO {

	public String		id;
	public String		name;
	public String		description;
	public int			type;
	public List<String>	optionValues;
	public List<String>	defaultValue;

}
