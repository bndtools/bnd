package aQute.lib.json;

import java.lang.reflect.Type;
import java.util.Map;

public class CharacterHandler extends Handler {

	@Override
	public void encode(Encoder app, Object object, Map<Object, Type> visited) throws Exception {
		Character c = (Character) object;
		int v = c.charValue();
		app.append(v + "");
	}

	@Override
	public Object decode(Decoder dec, boolean s) {
		return s ? 't' : 'f';
	}

	@Override
	public Object decode(Decoder dec, String s) {
		return (char) Integer.parseInt(s);
	}

	@Override
	public Object decode(Decoder dec, Number s) {
		return (char) s.shortValue();
	}

	@Override
	public Object decode(Decoder dec) {
		return 0;
	}

}
