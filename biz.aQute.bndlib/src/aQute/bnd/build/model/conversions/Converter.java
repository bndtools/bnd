package aQute.bnd.build.model.conversions;

public interface Converter<R, T> {
	R convert(T input) throws IllegalArgumentException;

	R error(String msg);
}
