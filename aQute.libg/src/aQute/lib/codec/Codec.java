package aQute.lib.codec;

import java.io.*;
import java.lang.reflect.*;

public interface Codec {
	Object decode(Reader in, Type type) throws Exception;	
	void encode(Type t, Object o, Appendable out) throws Exception;
}
