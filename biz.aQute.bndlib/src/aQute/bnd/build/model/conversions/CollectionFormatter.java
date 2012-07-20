package aQute.bnd.build.model.conversions;

import java.util.*;

public class CollectionFormatter<T> implements Converter<String,Collection< ? extends T>> {

	private final String						separator;
	private final Converter<String, ? super T>	itemFormatter;
	private final String						emptyOutput;

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
		this.separator = separator;
		this.itemFormatter = itemFormatter;
		this.emptyOutput = emptyOutput;
	}

	public String convert(Collection< ? extends T> input) throws IllegalArgumentException {
		String result = null;
		if (input != null) {
			if (input.isEmpty()) {
				result = emptyOutput;
			} else {
				StringBuilder buffer = new StringBuilder();
				for (Iterator< ? extends T> iter = input.iterator(); iter.hasNext();) {
					T item = iter.next();
					buffer.append(itemFormatter.convert(item));
					if (iter.hasNext())
						buffer.append(separator);
				}
				result = buffer.toString();
			}
		}
		return result;
	}
}