package aQute.bnd.mcp;

import java.util.ArrayList;
import java.util.List;

/**
 * Errors and warnings of an operation. Operations report problems here instead
 * of throwing, so a caller always gets a complete, inspectable result.
 */
public record Diagnostics(List<String> errors, List<String> warnings) {

	public Diagnostics {
		errors = List.copyOf(errors);
		warnings = List.copyOf(warnings);
	}

	public static Diagnostics none() {
		return new Diagnostics(List.of(), List.of());
	}

	public static Diagnostics error(String format, Object... args) {
		return new Diagnostics(List.of(String.format(format, args)), List.of());
	}

	public boolean ok() {
		return errors.isEmpty();
	}

	public Diagnostics plus(Diagnostics other) {
		return new Diagnostics(concat(errors, other.errors), concat(warnings, other.warnings));
	}

	public Diagnostics plus(List<String> moreErrors, List<String> moreWarnings) {
		return plus(new Diagnostics(moreErrors, moreWarnings));
	}

	private static List<String> concat(List<String> a, List<String> b) {
		List<String> result = new ArrayList<>(a);
		b.stream()
			.filter(s -> !result.contains(s))
			.forEach(result::add);
		return result;
	}

	/**
	 * Implemented by results that carry diagnostics; a result with errors is
	 * reported to the MCP client as a failed tool call.
	 */
	public interface Carrier {
		Diagnostics diagnostics();
	}
}
