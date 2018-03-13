package aQute.libg.filters;

public final class SimpleFilter extends Filter {

	private final String	name;
	private final Operator	operator;
	private final String	value;

	/**
	 * Construct a simple filter with the default "equals" operator, i.e. {@code
	 * (name=value)}.
	 */
	public SimpleFilter(String name, String value) {
		this(name, Operator.Equals, value);
	}

	/**
	 * Construct a simple filter with any of the comparison operators.
	 */
	public SimpleFilter(String name, Operator operator, String value) {
		this.name = name;
		this.operator = operator;
		this.value = value;
	}

	@Override
	public void append(StringBuilder builder) {
		builder.append('(');
		builder.append(name)
			.append(operator.getSymbol())
			.append(value);
		builder.append(')');
	}

}
