package aQute.bnd.build.model.conversions;

import java.util.*;

import aQute.bnd.osgi.*;
import aQute.libg.qtokens.*;

public class SimpleListConverter<R> implements Converter<List<R>,String> {

	private Converter< ? extends R, ? super String>	itemConverter;

	public static <R> Converter<List<R>,String> create(Converter<R, ? super String> itemConverter) {
		return new SimpleListConverter<R>(itemConverter);
	}

	public static Converter<List<String>,String> create() {
		return new SimpleListConverter<String>(new NoopConverter<String>());
	}

	private SimpleListConverter(Converter< ? extends R, ? super String> itemConverter) {
		this.itemConverter = itemConverter;
	}

	public List<R> convert(String input) throws IllegalArgumentException {
		List<R> result = new ArrayList<R>();

		if (Constants.EMPTY_HEADER.equalsIgnoreCase(input.trim()))
			return result;

		QuotedTokenizer qt = new QuotedTokenizer(input, ",");
		String token = qt.nextToken();

		while (token != null) {
			result.add(itemConverter.convert(token.trim()));
			token = qt.nextToken();
		}

		return result;
	}

}
