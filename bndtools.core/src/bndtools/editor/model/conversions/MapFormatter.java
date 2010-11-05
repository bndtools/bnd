package bndtools.editor.model.conversions;

import java.util.Map;
import java.util.Map.Entry;

public class MapFormatter implements Converter<String, Map<String, String>> {

    private CollectionFormatter<Entry<String, String>> entrySetFormatter;

    public MapFormatter(Converter<String, ? super Entry<String, String>> entryFormatter) {
        entrySetFormatter = new CollectionFormatter<Entry<String, String>>(entryFormatter);
    }

    public String convert(Map<String, String> input) throws IllegalArgumentException {
        return entrySetFormatter.convert(input.entrySet());
    }
}
