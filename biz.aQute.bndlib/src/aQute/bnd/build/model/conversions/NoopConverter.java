package aQute.bnd.build.model.conversions;

public class NoopConverter<T> implements Converter<T,T> {
	public T convert(T input) throws IllegalArgumentException {
		return input;
	}
}