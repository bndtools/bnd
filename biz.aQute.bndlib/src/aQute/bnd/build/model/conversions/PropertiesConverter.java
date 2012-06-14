package aQute.bnd.build.model.conversions;

import java.util.*;

import aQute.libg.header.*;

public class PropertiesConverter implements Converter<Map<String,String>,String> {

	public Map<String,String> convert(String input) throws IllegalArgumentException {
		return OSGiHeader.parseProperties(input);
	}

}
