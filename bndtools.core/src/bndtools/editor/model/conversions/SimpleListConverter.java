package bndtools.editor.model.conversions;

import java.util.ArrayList;
import java.util.List;

import aQute.libg.qtokens.QuotedTokenizer;

public class SimpleListConverter<R> implements Converter<List<R>, String> {

    private Converter<? extends R, ? super String> itemConverter;

    public SimpleListConverter(Converter<? extends R, ? super String> itemConverter) {
        this.itemConverter = itemConverter;
    }

    public List<R> convert(String input) throws IllegalArgumentException {
        List<R> result = new ArrayList<R>();

        QuotedTokenizer qt = new QuotedTokenizer(input, ",");
        String token = qt.nextToken();

        while (token != null) {
            result.add(itemConverter.convert(token.trim()));
            token = qt.nextToken();
        }

        return result;
    }

}
