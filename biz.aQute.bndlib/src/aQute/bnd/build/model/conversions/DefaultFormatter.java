package aQute.bnd.build.model.conversions;

public class DefaultFormatter implements Converter<String, Object> {

	public String convert(Object input) throws IllegalArgumentException {
		return input == null ? null : input.toString();
	}

	@Override
	public String error(String msg) {
		return msg;
	}

}
