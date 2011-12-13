package aQute.bnd.service.diff;

import java.util.*;

public interface Diff {
	interface Ignore {
		boolean contains(Diff diff);
	}
	
	Delta getDelta();
	Delta getDelta(Ignore ignore);

	Type getType();

	String getOlderValue();

	String getNewerValue();

	Collection<? extends Diff> getChildren();
	
	Diff get(String name);
	
	String getName();
	
}
