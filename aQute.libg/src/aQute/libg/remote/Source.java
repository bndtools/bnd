package aQute.libg.remote;

import java.io.IOException;

public interface Source {
	byte[] getData(String id) throws Exception;

	void event(Event e, Area area) throws Exception;

	void output(String areaId, CharSequence text, boolean err) throws IOException;
}
