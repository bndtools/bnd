package aQute.bnd.build.model.conversions;

import aQute.bnd.build.model.EE;

public final class EEFormatter implements Converter<String, EE> {
	@Override
	public String convert(EE input) throws IllegalArgumentException {
		return input != null ? input.getEEName() : null;
	}

	@Override
	public String error(String msg) {
		return msg;
	}
}
