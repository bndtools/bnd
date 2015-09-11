package aQute.libg.filters;

public final class NotFilter extends Filter {

	private final Filter child;

	public NotFilter(Filter child) {
		this.child = child;
	}

	@Override
	public void append(StringBuilder builder) {
		builder.append("(!");
		child.append(builder);
		builder.append(")");
	}

}
