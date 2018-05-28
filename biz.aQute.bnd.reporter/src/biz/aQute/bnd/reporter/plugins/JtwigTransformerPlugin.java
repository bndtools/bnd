package biz.aQute.bnd.reporter.plugins;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.Map;

import org.jtwig.JtwigModel;
import org.jtwig.JtwigTemplate;
import org.jtwig.environment.EnvironmentConfigurationBuilder;

import aQute.bnd.service.reporter.ReportTransformerPlugin;
import aQute.lib.io.IO;
import aQute.lib.json.JSONCodec;

public class JtwigTransformerPlugin implements ReportTransformerPlugin {

	static private final String[] _extT = { "twig" };
	static private final String[] _extI = { "json" };

	@Override
	public void transform(final InputStream data, final InputStream templateInputStream, final OutputStream output,
			final Map<String, String> parameters) throws Exception {
		final Object modelDto = new JSONCodec().dec().from(data).get();

		final EnvironmentConfigurationBuilder eb = EnvironmentConfigurationBuilder.configuration();

		final JtwigTemplate template = JtwigTemplate.inlineTemplate(IO.collect(templateInputStream), eb.build());
		final JtwigModel model = JtwigModel.newModel().with("report", modelDto);
		parameters.forEach((k, v) -> model.with(k, v));

		template.render(model, output);
	}

	@Override
	public String[] getHandledTemplateExtensions() {
		return _extT;
	}

	@Override
	public String[] getHandledModelExtensions() {
		return _extI;
	}
}
