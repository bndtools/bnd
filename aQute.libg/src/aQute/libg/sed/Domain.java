package aQute.libg.sed;

import java.util.*;

public interface Domain {
	Map<String,String> getMap();

	Domain getParent();
}
