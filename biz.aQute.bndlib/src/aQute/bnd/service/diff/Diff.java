package aQute.bnd.service.diff;

import java.util.Collection;

public interface Diff {
	interface Ignore {
		boolean contains(Diff diff);
	}

	class Data {

		public Type		type;
		public Delta	delta;
		public String	name;
		public Data[]	children;
		public String	comment;
	}

	Data serialize();

	Delta getDelta();

	Delta getDelta(Ignore ignore);

	Type getType();

	String getName();

	Tree getOlder();

	Tree getNewer();

	Collection<? extends Diff> getChildren();

	Diff get(String name);

}
