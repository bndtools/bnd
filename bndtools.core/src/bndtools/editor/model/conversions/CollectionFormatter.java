package bndtools.editor.model.conversions;

import java.util.Collection;
import java.util.Iterator;

import bndtools.editor.model.BndEditModel;

public class CollectionFormatter<T> implements Converter<String, Collection<? extends T>> {

    private final Converter<String, ? super T> itemFormatter;

    public CollectionFormatter() {
        this(new DefaultFormatter());
    }

    public CollectionFormatter(Converter<String, ? super T> itemFormatter) {
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
                    buffer.append(BndEditModel.LIST_SEPARATOR);
            }
            result = buffer.toString();
        }
        return result;
    }
}