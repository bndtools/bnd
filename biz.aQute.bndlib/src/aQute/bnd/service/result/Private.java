package aQute.bnd.service.result;

import aQute.lib.exceptions.Exceptions;

public class Private {

	static RuntimeException duck(Exception e) {
		return Exceptions.duck(e);
	}

}
