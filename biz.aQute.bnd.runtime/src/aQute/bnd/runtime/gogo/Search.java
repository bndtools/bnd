package aQute.bnd.runtime.gogo;

import java.util.Formatter;
import java.util.HashSet;
import java.util.Set;

import org.apache.felix.service.command.Converter;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.wiring.BundleRevision;

public class Search implements Converter {
	public String			serviceName;
	public BundleRevision	searcher;
	public Set<Long>		matched		= new HashSet<>();
	public Set<Long>		mismatched	= new HashSet<>();

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append(getState());

		sb.append("[")
			.append(searcher.getBundle()
				.getBundleId())
			.append("] ");
		sb.append(serviceName);

		sb.append(" ")
			.append(matched);
		if (!mismatched.isEmpty())
			sb.append(" !! ")
				.append(mismatched);
		return sb.toString();
	}

	private String getState() {
		if (!mismatched.isEmpty())
			return "! ";
		else if (matched.isEmpty())
			return "? ";
		else
			return "  ";
	}

	@Override
	public Object convert(Class<?> targetType, Object source) throws Exception {
		return null;
	}

	@Override
	public CharSequence format(Object source, int level, Converter next) throws Exception {
		switch (level) {
			case Converter.INSPECT :
				return inspect(this, next);

			case Converter.LINE :
				return line(this, next);

			case Converter.PART :
				return part(this, next);
		}
		return null;
	}

	private CharSequence part(Search search, Converter next) {
		return toString();
	}

	private CharSequence line(Search search, Converter next) throws Exception {
		try (Formatter f = new Formatter()) {
			f.format("%s %-60s %-50s %s %s", getState(), search.serviceName, search.searcher.getBundle(),
				search.matched.isEmpty() ? "" : search.matched,
				search.mismatched.isEmpty() ? "" : "!! " + search.mismatched);
			return f.toString();
		}
	}

	private CharSequence inspect(Search search, Converter next) throws Exception {
		BundleContext context = search.searcher.getBundle()
			.getBundleContext();

		try (Formatter f = new Formatter()) {
			f.format("Searching Bundle                %s\n", search.searcher.getBundle());
			f.format("Service Name                    %s\n", search.serviceName);
			if (!search.matched.isEmpty()) {
				f.format("Registrars in same class space \n");
				for (Long b : search.matched) {
					Bundle bundle = context.getBundle(b);
					f.format("  %s\n", next.format(bundle, Converter.LINE, next));
				}
			}

			if (!search.mismatched.isEmpty()) {
				f.format("!!! Registrars in different class space \n");
				for (Long b : search.mismatched) {
					Bundle bundle = context.getBundle(b);
					f.format("  %s\n", next.format(bundle, Converter.LINE, next));
				}
			}
			return f.toString();
		}
	}

}
