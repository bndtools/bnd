package bndtools.editor.model.conversions;

import java.util.Map;

import aQute.libg.header.OSGiHeader;

public class PropertiesConverter implements Converter<Map<String, String>, String> {

    public Map<String, String> convert(String input) throws IllegalArgumentException {
        return OSGiHeader.parseProperties(input);
    }

}
