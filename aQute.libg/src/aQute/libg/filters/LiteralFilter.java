package aQute.libg.filters;

public class LiteralFilter extends Filter {

	private String filterString;

	public LiteralFilter(String filterString) {
		this.filterString = filterString;
	}

	@Override
	public void append(StringBuilder builder) {
		builder.append(filterString);
	}

}
