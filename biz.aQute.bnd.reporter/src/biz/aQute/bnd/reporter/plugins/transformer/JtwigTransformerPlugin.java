package biz.aQute.bnd.reporter.plugins.transformer;

import aQute.bnd.osgi.Resource;
import aQute.bnd.service.reporter.ReportTransformerPlugin;
import aQute.lib.io.IO;
import aQute.lib.json.JSONCodec;
import com.google.common.base.Optional;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.Map;
import org.jtwig.JtwigModel;
import org.jtwig.JtwigTemplate;
import org.jtwig.environment.EnvironmentConfigurationBuilder;
import org.jtwig.resource.exceptions.ResourceNotFoundException;
import org.jtwig.resource.loader.ResourceLoader;
import org.jtwig.resource.loader.TypedResourceLoader;

public class JtwigTransformerPlugin implements ReportTransformerPlugin {

  static private final String[] _extT = {"twig", "jtwig"};
  static private final String[] _extI = {"json"};

  @Override
  public void transform(final InputStream data, final InputStream templateInputStream,
      final OutputStream output, final Map<String, String> parameters) throws Exception {
    final Object modelDto = new JSONCodec().dec().from(data).get();

    final EnvironmentConfigurationBuilder eb = EnvironmentConfigurationBuilder.configuration();

    eb.resources().resourceLoaders().add(new TypedResourceLoader("http", new HttpResourceLoader()));
    eb.resources().resourceLoaders()
        .add(new TypedResourceLoader("https", new HttpsResourceLoader()));

    final JtwigTemplate template =
        JtwigTemplate.inlineTemplate(IO.collect(templateInputStream), eb.build());
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

  class HttpResourceLoader implements ResourceLoader {

    @SuppressWarnings("unused")
    @Override
    public boolean exists(final String path) {
      try {
        new URL("http:" + path);
        return true;
      } catch (final MalformedURLException exception) {
        return false;
      }
    }

    @Override
    public Optional<Charset> getCharset(final String path) {
      return Optional.absent();
    }

    @Override
    public InputStream load(final String path) {
      try {
        return Resource.fromURL(new URL("http:" + path)).openInputStream();
      } catch (final Exception exception) {
        throw new ResourceNotFoundException(exception);
      }
    }

    @Override
    public Optional<URL> toUrl(final String path) {
      try {
        return Optional.of(new URL("http:" + path));
      } catch (@SuppressWarnings("unused") final MalformedURLException exception) {
        return Optional.absent();
      }
    }
  }

  class HttpsResourceLoader implements ResourceLoader {

    @SuppressWarnings("unused")
    @Override
    public boolean exists(final String path) {
      try {
        new URL("https:" + path);
        return true;
      } catch (final MalformedURLException exception) {
        return false;
      }
    }

    @Override
    public Optional<Charset> getCharset(final String path) {
      return Optional.absent();
    }

    @Override
    public InputStream load(final String path) {
      try {
        return Resource.fromURL(new URL("https:" + path)).openInputStream();
      } catch (final Exception exception) {
        throw new ResourceNotFoundException(exception);
      }
    }

    @Override
    public Optional<URL> toUrl(final String path) {
      try {
        return Optional.of(new URL("https:" + path));
      } catch (@SuppressWarnings("unused") final MalformedURLException exception) {
        return Optional.absent();
      }
    }
  }
}
