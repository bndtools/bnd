package biz.aQute.bnd.reporter.plugin;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import aQute.bnd.header.Attrs;
import aQute.bnd.header.Parameters;
import aQute.bnd.osgi.Builder;
import aQute.bnd.osgi.Constants;
import aQute.bnd.service.buildevents.PostBuildPlugin;
import biz.aQute.bnd.reporter.generator.ReportConfig;
import biz.aQute.bnd.reporter.generator.ReportGenerator;

public class ReporterPlugin implements PostBuildPlugin {

	private static final String LOCALE_ATTR = "locale";
	private static final String INCLUDES_ATTR = "includes";
	private static final String TEMPLATES_ATTR = "templates";

	@Override
	public void postBuild(final Builder builder) throws Exception {
		if (builder.getJar() != null) {
			final Parameters parameters = new Parameters(builder.getProperty(Constants.REPORT));
			final Map<Attrs, ReportConfig.Builder> stored = new HashMap<>();
			final List<ReportConfig.Builder> configs = new LinkedList<>();

			for (final Map.Entry<String, Attrs> e : parameters.entrySet()) {
				if (stored.containsKey(e.getValue())) {
					stored.get(e.getValue()).setOutput(e.getKey());
				} else {
					ReportConfig.Builder config;

					if (builder.getJar() != null) {
						config = ReportConfig.builder(builder.getJar());
					} else {
						config = null;
					}

					if (config != null) {
						stored.put(e.getValue(), config);
						configs.add(config);

						config.setOutput(e.getKey());

						if (e.getValue().containsKey(INCLUDES_ATTR)) {
							for (final String include : e.getValue().get(INCLUDES_ATTR).split(",")) {
								final String[] parts = include.trim().split(":");
								if (parts.length == 1) {
									config.addIncludePath(parts[0], null, null);
								} else if (parts.length == 2) {
									if (parts[1].isEmpty()) {
										parts[1] = null;
									}
									config.addIncludePath(parts[0], parts[1], null);
								} else if (parts.length == 3) {
									if (parts[1].isEmpty()) {
										parts[1] = null;
									}
									if (parts[2].isEmpty()) {
										parts[2] = null;
									}
									config.addIncludePath(parts[0], parts[1], parts[2]);
								}
							}
						}

						if (e.getValue().containsKey(LOCALE_ATTR)) {
							config.setLocale(e.getValue().get(LOCALE_ATTR));
						}

						if (e.getValue().containsKey(TEMPLATES_ATTR)) {
							for (final String template : e.getValue().get(TEMPLATES_ATTR).split(",")) {
								config.addTemplates(template.trim());
							}
						}
					}
				}
			}

			try (ReportGenerator rg = new ReportGenerator(builder)) {
				for (final ReportConfig.Builder config : configs) {
					rg.generate(config.build());
				}
				builder.getInfo(rg);
			}
		}
	}
}
