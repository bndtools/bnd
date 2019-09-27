package aQute.bnd.build.model.conversions;

public class NoopConverter<T> implements Converter<T, T> {
	@Override
	public T convert(T input) throws IllegalArgumentException {
		return input;
	}

	@Override
	public T error(String msg) {
		return null;
	}
}
