package aQute.bnd.unmodifiable;

import java.util.function.Consumer;

class Holder<E> implements Consumer<E> {
	boolean	set	= false;
	E value;

	@Override
	public void accept(E t) {
		this.value = t;
		set = true;
	}
}
