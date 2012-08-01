package aQute.lib.json;

import java.lang.reflect.*;
import java.util.*;

public class CharacterHandler extends Handler {

	@Override
	void encode(Encoder app, Object object, Map<Object,Type> visited) throws Exception {
		Character c = (Character) object;
		int v = c.charValue();
		app.append(v + "");
	}

	@Override
	Object decode(Decoder dec, boolean s) {
		return s ? 't' : 'f';
	}

	@Override
	Object decode(Decoder dec, String s) {
		return (char) Integer.parseInt(s);
	}

	@Override
	Object decode(Decoder dec, Number s) {
		return (char) s.shortValue();
	}

	@Override
	Object decode(Decoder dec) {
		return 0;
	}

}
