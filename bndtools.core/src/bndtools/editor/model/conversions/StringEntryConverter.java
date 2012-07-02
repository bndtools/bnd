package bndtools.editor.model.conversions;

import java.util.Map.Entry;

public class StringEntryConverter implements Converter<String,Entry<String, ? >> {

    public String convert(Entry<String, ? > input) throws IllegalArgumentException {
        return input.getKey();
    }

}
