package biz.aQute.bnd.reporter.plugins.entries.bndproject;

import java.io.File;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

import aQute.bnd.annotation.plugin.BndPlugin;
import aQute.bnd.build.Project;
import aQute.bnd.service.Plugin;
import aQute.bnd.service.reporter.ReportEntryPlugin;
import aQute.service.reporter.Reporter;
import biz.aQute.bnd.reporter.codesnippet.CodeSnippetExtractor;
import biz.aQute.bnd.reporter.codesnippet.dto.CodeSnippetDTO;
import biz.aQute.bnd.reporter.generator.EntryNamesReference;

/**
 * This plugins allows to extract code snippets from a project. The user can set
 * the {@link CodeSnippetPlugin#PATH_PROPERTY} to specify the folder from which
 * snippets are looked up. By default, snippets are read from
 * {testClasspath}/examples directory.
 */
@BndPlugin(name = "entry." + EntryNamesReference.CODE_SNIPPETS)
public class CodeSnippetPlugin implements ReportEntryPlugin<Project>, Plugin {

	final public static String			PATH_PROPERTY	= "path";

	private final Map<String, String>	_properties		= new HashMap<>();
	private final CodeSnippetExtractor	_extractor		= new CodeSnippetExtractor();

	public CodeSnippetPlugin() {
		_properties.put(ReportEntryPlugin.ENTRY_NAME_PROPERTY, EntryNamesReference.CODE_SNIPPETS);
		_properties.put(ReportEntryPlugin.SOURCE_CLASS_PROPERTY, Project.class.getCanonicalName());
	}

	@Override
	public void setReporter(final Reporter processor) {
		// Not needed
	}

	@Override
	public void setProperties(final Map<String, String> map) throws Exception {
		_properties.putAll(map);
	}

	@Override
	public Map<String, String> getProperties() {
		return Collections.unmodifiableMap(_properties);
	}

	private File getDirectory(final Project p) {
		File dir = null;
		if (_properties.containsKey(PATH_PROPERTY)) {
			dir = new File(_properties.get(PATH_PROPERTY));
		} else {
			dir = new File(p.getTestSrc(), "examples");
		}
		return (dir.exists() && dir.isDirectory()) ? dir : null;
	}

	@Override
	public List<CodeSnippetDTO> extract(final Project project, final Locale locale) throws Exception {
		Objects.requireNonNull(project, "project");

		final File dir = getDirectory(project);

		if (dir != null) {
			final List<CodeSnippetDTO> result = _extractor.extract(dir.getPath());
			return !result.isEmpty() ? result : null;
		} else {
			return null;
		}
	}
}
