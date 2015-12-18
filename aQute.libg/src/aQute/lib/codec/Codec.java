package aQute.lib.codec;

import java.io.Reader;
import java.lang.reflect.Type;

public interface Codec {
	Object decode(Reader in, Type type) throws Exception;

	void encode(Type t, Object o, Appendable out) throws Exception;
}
