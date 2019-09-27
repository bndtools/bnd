package bndtools.types;

public class ComparablePair<A extends Comparable<A>, B> extends Pair<A, B> implements Comparable<Pair<A, ?>> {

	private static final long serialVersionUID = 1L;

	public ComparablePair(A first, B second) {
		super(first, second);
	}

	@Override
	public int compareTo(Pair<A, ?> o) {
		return getFirst().compareTo(o.getFirst());
	}
}
