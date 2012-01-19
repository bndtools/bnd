package aQute.bnd.service.diff;

import java.util.*;

public interface Diff {
	interface Ignore {
		boolean contains(Diff diff);
	}
	
	Delta getDelta();
	Delta getDelta(Ignore ignore);

	Type getType();
	String getName();
	Tree getOlder();
	Tree getNewer();

	Collection<? extends Diff> getChildren();
	
	Diff get(String name);
	
	
}
