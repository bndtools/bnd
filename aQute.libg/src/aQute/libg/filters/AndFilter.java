package aQute.libg.filters;

import java.util.LinkedList;
import java.util.List;

public final class AndFilter extends Filter {

	private final List<Filter> children = new LinkedList<>();

	public AndFilter addChild(Filter child) {
		if (child instanceof AndFilter)
			children.addAll(((AndFilter) child).children);
		else
			children.add(child);
		return this;
	}

	@Override
	public void append(StringBuilder builder) {
		if (children.isEmpty())
			return;

		builder.append("(&");
		for (Filter child : children) {
			child.append(builder);
		}
		builder.append(")");
	}

}
