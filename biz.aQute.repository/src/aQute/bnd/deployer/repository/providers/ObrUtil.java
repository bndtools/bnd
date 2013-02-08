package aQute.bnd.deployer.repository.providers;

import java.util.regex.*;

import org.osgi.service.log.*;

public class ObrUtil {

	// Pattern to remove naked less-than operators and replace with not-greater-or-equal
	private static final Pattern REMOVE_LT = Pattern.compile("\\(([^<>=~()]*)<([^*=]([^\\\\\\*\\(\\)]|\\\\|\\*|\\(|\\))*)\\)");
	private static final String	NOT_GREATER_THAN_OR_EQUAL	= "(!($1>=$2))";

	// Pattern to remove naked greater-than operators and replace with not-less-than-or-equal
	private static final Pattern REMOVE_GT = Pattern.compile("\\(([^<>=~()]*)>([^*=]([^\\\\\\*\\(\\)]|\\\\|\\*|\\(|\\))*)\\)");
	private static final String	NOT_LESS_THAN_OR_EQUAL	= "(!($1<=$2))";
	
	// Patterns to search for OBR's extension "Set Arithmetic" operations
	private static final Pattern REMOVE_SUBSET = Pattern.compile("\\([^<>=~()]*<\\*[^)]*\\)");
	private static final Pattern REMOVE_SUPERSET = Pattern.compile("\\([^<>=~()]*\\*>[^)]*\\)");

	public static final String processFilter(String filter, LogService log) {
		filter = removeMatches(filter, REMOVE_SUBSET, log, "Removed unsupported subset clause: %s");
		filter = removeMatches(filter, REMOVE_SUPERSET, log, "Removed unsupported superset clause: %s");
		
		filter = REMOVE_LT.matcher(filter).replaceAll(NOT_GREATER_THAN_OR_EQUAL);
		filter = REMOVE_GT.matcher(filter).replaceAll(NOT_LESS_THAN_OR_EQUAL);
		
		return filter;
	}
	
	private static final String removeMatches(String string, Pattern pattern, LogService log, String messageTemplate) {
		Matcher matcher = pattern.matcher(string);
		while (matcher.find()) {
			String offending = matcher.group();
			String before = string.substring(0, matcher.start());
			String after = matcher.end() < string.length() ? string.substring(matcher.end()) : "";
			string = before + after;
			if (log != null)
				log.log(LogService.LOG_INFO, String.format(messageTemplate, offending), null);
			matcher.reset(string);
		}
		return string;
	}
}
