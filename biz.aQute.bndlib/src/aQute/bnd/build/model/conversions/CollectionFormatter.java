package aQute.bnd.build.model.conversions;

import java.util.*;

public class CollectionFormatter<T> implements Converter<String,Collection< ? extends T>> {

	private final String						separator;
	private final Converter<String, ? super T>	itemFormatter;
	private final String						emptyOutput;
	private final String						initial;
	private final String						suffix;

	public CollectionFormatter(String separator) {
		this(separator, (String) null);
	}

	public CollectionFormatter(String separator, String emptyOutput) {
		this(separator, new DefaultFormatter(), emptyOutput);
	}

	public CollectionFormatter(String separator, Converter<String, ? super T> itemFormatter) {
		this(separator, itemFormatter, null);
	}

	public CollectionFormatter(String separator, Converter<String, ? super T> itemFormatter, String emptyOutput) {
		this(separator, itemFormatter, emptyOutput, " \\\n\t", "");
	}

	public CollectionFormatter(String separator, Converter<String, ? super T> itemFormatter, String emptyOutput,
			String prefix, String suffix) {
		this.separator = separator;
		this.itemFormatter = itemFormatter;
		this.emptyOutput = emptyOutput;
		this.initial = prefix;
		this.suffix = suffix;
	}

	public String convert(Collection< ? extends T> input) throws IllegalArgumentException {
		String result = null;
		if (input != null) {
			if (input.isEmpty()) {
				result = emptyOutput;
			} else {
				StringBuilder buffer = new StringBuilder();
				String del  = initial == null ? "" : initial;
				for (T item : input) {
					buffer.append(del);
					buffer.append(itemFormatter.convert(item));
					del = separator;
				}
				if ( suffix != null)
					buffer.append(suffix);
				result = buffer.toString();
			}
		}
		return result;
	}
}