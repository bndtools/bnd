package bndtools.editor.model.conversions;

import java.util.Collection;
import java.util.Iterator;

public class CollectionFormatter<T> implements Converter<String, Collection<? extends T>> {

    private final String separator;
    private final Converter<String, ? super T> itemFormatter;

    public CollectionFormatter(String separator) {
        this(separator, new DefaultFormatter());
    }

    public CollectionFormatter(String separator, Converter<String, ? super T> itemFormatter) {
        this.separator = separator;
        this.itemFormatter = itemFormatter;
    }

    public String convert(Collection<? extends T> input) throws IllegalArgumentException {
        String result = null;
        if (input != null && !input.isEmpty()) {
            StringBuilder buffer = new StringBuilder();
            for(Iterator<? extends T> iter = input.iterator(); iter.hasNext(); ) {
                T item = iter.next();
                buffer.append(itemFormatter.convert(item));
                if(iter.hasNext())
                    buffer.append(separator);
            }
            result = buffer.toString();
        }
        return result;
    }
}