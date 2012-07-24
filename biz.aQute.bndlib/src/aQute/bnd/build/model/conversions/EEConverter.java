package aQute.bnd.build.model.conversions;

import aQute.bnd.build.model.*;

public class EEConverter implements Converter<EE,String> {

	public EE convert(String input) throws IllegalArgumentException {
		return EE.parse(input);
	}

}
