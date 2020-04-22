package aQute.bnd.service.generate;

import java.util.List;
import java.util.Map;

public interface Options {
	List<String> _arguments();

	Map<String, String> _properties();
}
