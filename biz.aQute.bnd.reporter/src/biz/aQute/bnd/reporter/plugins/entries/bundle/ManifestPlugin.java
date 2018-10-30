package biz.aQute.bnd.reporter.plugins.entries.bundle;

import aQute.bnd.annotation.plugin.BndPlugin;
import aQute.bnd.osgi.Jar;
import aQute.bnd.service.Plugin;
import aQute.bnd.service.reporter.ReportEntryPlugin;
import aQute.service.reporter.Reporter;
import biz.aQute.bnd.reporter.generator.EntryNamesReference;
import biz.aQute.bnd.reporter.helpers.HeadersHelper;
import java.util.Collections;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

/**
 * This plugin allows to add some of the bundle manifest headers to the report.
 */
@BndPlugin(name = "entry." + EntryNamesReference.MANIFEST)
public class ManifestPlugin implements ReportEntryPlugin<Jar>, Plugin {

  private Reporter _reporter;
  private final Map<String, String> _properties = new HashMap<>();


  public ManifestPlugin() {
    _properties.put(ReportEntryPlugin.ENTRY_NAME_PROPERTY, EntryNamesReference.MANIFEST);
    _properties.put(ReportEntryPlugin.SOURCE_CLASS_PROPERTY, Jar.class.getCanonicalName());
  }

  @Override
  public Object extract(final Jar jar, final Locale locale) {
    Objects.requireNonNull(jar, "jar");
    Objects.requireNonNull(locale, "locale");

    final Map<String, Object> result = HeadersHelper.extract(jar, locale, _reporter);

    return !result.isEmpty() ? result : null;
  }

  @Override
  public Map<String, String> getProperties() {
    return Collections.unmodifiableMap(_properties);
  }

  @Override
  public void setProperties(final Map<String, String> map) throws Exception {
    _properties.putAll(map);
  }

  @Override
  public void setReporter(final Reporter processor) {
    _reporter = processor;
  }
}
