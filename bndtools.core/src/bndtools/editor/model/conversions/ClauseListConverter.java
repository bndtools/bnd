package bndtools.editor.model.conversions;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import aQute.libg.header.OSGiHeader;


public class ClauseListConverter<R> implements Converter<List<R>, String> {

    private final Converter<? extends R, ? super Entry<String, Map<String, String>>> itemConverter;

    public ClauseListConverter(Converter<? extends R, ? super Entry<String, Map<String, String>>> itemConverter) {
        this.itemConverter = itemConverter;
    }

    public List<R> convert(String input) throws IllegalArgumentException {
        List<R> result = new ArrayList<R>();

        Map<String, Map<String, String>> header = OSGiHeader.parseHeader(input);
        for (Entry<String, Map<String, String>> entry : header.entrySet()) {
            result.add(itemConverter.convert(entry));
        }

        return result;
    }

}
