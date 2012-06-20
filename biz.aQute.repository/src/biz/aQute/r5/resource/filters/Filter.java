package biz.aQute.r5.resource.filters;

public abstract class Filter {

	public abstract void append(StringBuilder builder);

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		append(builder);
		return builder.toString();
	}

}
