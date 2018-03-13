package aQute.bnd.build.model.conversions;

import java.util.ArrayList;
import java.util.List;

import aQute.bnd.osgi.Constants;
import aQute.libg.qtokens.QuotedTokenizer;

public class SimpleListConverter<R> implements Converter<List<R>, String> {

	private Converter<? extends R, ? super String> itemConverter;

	public static <R> Converter<List<R>, String> create(Converter<R, ? super String> itemConverter) {
		return new SimpleListConverter<>(itemConverter);
	}

	public static Converter<List<String>, String> create() {
		return new SimpleListConverter<>(new NoopConverter<>());
	}

	private SimpleListConverter(Converter<? extends R, ? super String> itemConverter) {
		this.itemConverter = itemConverter;
	}

	@Override
	public List<R> convert(String input) throws IllegalArgumentException {
		if (input == null)
			return null;

		List<R> result = new ArrayList<>();

		if (input == null || Constants.EMPTY_HEADER.equalsIgnoreCase(input.trim()))
			return result;

		QuotedTokenizer qt = new QuotedTokenizer(input, ",");
		String token = qt.nextToken();

		while (token != null) {
			result.add(itemConverter.convert(token.trim()));
			token = qt.nextToken();
		}

		return result;
	}

	@Override
	public List<R> error(String msg) {
		List<R> l = new ArrayList<>();
		l.add(itemConverter.error(msg));
		return l;
	}

}
