package aQute.bnd.shade;

import java.util.AbstractMap;
import java.util.Set;

import aQute.bnd.shade.ShadeA.ShadeB;
import aQute.bnd.shade.ShadeA.ShadeC;

public class ShadeA<T extends ShadeB<ShadeC>> extends AbstractMap<ShadeC, ShadeB<ShadeC>> {
	static class ShadeB<B extends ShadeC> {
		void foo(Class<B> b) {

		}
	}

	static class ShadeC {

	}

	@Override
	public Set<Entry<ShadeC, ShadeB<ShadeC>>> entrySet() {
		return null;
	}
}
