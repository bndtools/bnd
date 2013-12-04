package biz.aQute.bndoc.lib;

import java.util.regex.*;

public class SinglePage extends Bndoc {
	final static Pattern CONTENT_P = Pattern.compile("\\$\\{content\\}");

	@Override
	public void generate() throws Exception {
		if (template == null)
			template = "${content}";

		int start = 0;
		Matcher matcher = CONTENT_P.matcher(template);
		while (matcher.find()) {
			String before = template.substring(start, matcher.start());
			out.write(replacer.process(before));
			content();
			start = matcher.end() + 1;
		}
		if (start == 0)
			reporter.error("No ${content} macro in the template");
		if (start < template.length() - 1) {
			String after = template.substring(start);
			out.write(replacer.process(after));
		}
		out.flush();
	}

}
