package bndtools.utils;

import java.util.Iterator;
import java.util.List;

public class CircularDependencyException extends Exception {

	private static final long	serialVersionUID	= 1L;
	@SuppressWarnings("rawtypes")
	private final List			circle;

	public CircularDependencyException(@SuppressWarnings("rawtypes") List circle) {
		this.circle = circle;
	}

	@Override
	public String getLocalizedMessage() {
		StringBuilder builder = new StringBuilder();
		builder.append("Artifacts in cycle: ");
		for (Iterator<?> iter = circle.iterator(); iter.hasNext();) {
			builder.append(iter.next()
				.toString());
			if (iter.hasNext())
				builder.append(" -> ");
		}
		return builder.toString();
	}

}
