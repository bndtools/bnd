package biz.aQute.bnd.reporter.plugins.transformer;

import java.util.Map;
import java.util.Map.Entry;

import org.jtwig.functions.FunctionRequest;
import org.jtwig.functions.JtwigFunction;
import org.jtwig.functions.SimpleJtwigFunction;

public class JTwigFunctions {
	public static final String BND_REPORTER_SHOW_PREFIX = "bnd.reporter.show.";

	public static final JtwigFunction newfunction_showSection() {
		return new SimpleJtwigFunction() {

			@Override
			public String name() {
				return "showSection";
			}

			@Override
			public Object execute(FunctionRequest functionRequest) {

				try {
					String name = (String) functionRequest.get(0);
					Boolean defaultSection = (Boolean) functionRequest.get(1);
					Object allParametersObj = functionRequest.get(2);

					boolean check = defaultSection;
					if (allParametersObj != null && allParametersObj instanceof Map) {
						for (Entry<String, String> entry : ((Map<String, String>) allParametersObj).entrySet()) {
							String parameterKey = entry.getKey();
							String parameterValue = entry.getValue();

							if (parameterKey != null && parameterKey.startsWith(BND_REPORTER_SHOW_PREFIX)) {
								String testParam = parameterKey.substring(BND_REPORTER_SHOW_PREFIX.length())
									.replace(".", "\\.")
									.replace("*", "(.*)");
								if (name.matches(testParam)) {
									check = Boolean.valueOf(parameterValue);
								}
							}
						}
					}
					return check;

				} catch (Exception e) {
					e.printStackTrace();

					return true;
				}
			}
		};
	}
}
