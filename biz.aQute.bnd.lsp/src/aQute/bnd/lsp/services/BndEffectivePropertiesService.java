package aQute.bnd.lsp.services;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import aQute.bnd.build.Workspace;
import aQute.bnd.build.Run;
import aQute.bnd.build.Project;
import aQute.bnd.build.model.BndEditModel;
import aQute.bnd.help.Syntax;
import aQute.bnd.osgi.Constants;
import aQute.bnd.osgi.Instructions;
import aQute.bnd.osgi.Processor;
import aQute.bnd.osgi.Processor.PropertyKey;
import aQute.lib.utf8properties.UTF8Properties;

public class BndEffectivePropertiesService {

	public record Provenance(String label, String uri) {}

	public record Row(String key, String value, String formattedValue, List<Provenance> provenances,
		List<String> errors) {}

	public record Result(int schemaVersion, String uri, Integer documentVersion, boolean expanded, boolean merged,
		List<Row> rows, String effectiveSource, List<String> dependencies, List<String> diagnostics) {}

	public Result evaluate(File file, String text, Integer version, boolean expanded, boolean merged) throws Exception {
		file = file.getCanonicalFile();
		Snapshot snapshot = new Snapshot(file, text);
		File root = file.getParentFile();
		while (root != null && !new File(root, "cnf/build.bnd").isFile()) {
			root = root.getParentFile();
		}
		Workspace workspace = null;
		try {
			if (root != null) {
				workspace = new Workspace(root, Workspace.CNFDIR) {
					@Override
					public Properties loadProperties(File source) throws IOException {
						return snapshot.load(source, this);
					}
				};
			}
			if (workspace != null && file.equals(workspace.getPropertiesFile().getCanonicalFile())) {
				return collect(workspace, file, version, expanded, merged);
			}
			if (workspace != null && file.getParentFile().getParentFile().equals(root)
				&& new File(file.getParentFile(), Project.BNDFILE).isFile()
				&& !file.getName().endsWith(".bndrun")) {
				try (Project project = new Project(workspace, file.getParentFile()) {
					@Override
					public Properties loadProperties(File source) throws IOException {
						return snapshot.load(source, this);
					}
				}) {
					if (file.getName().equals(Project.BNDFILE)) {
						return collect(project, file, version, expanded, merged);
					}
					if (new Instructions(project.getProperty(Constants.SUB)).matches(file.getName())
						&& !project.is(Constants.NOBUNDLES)) {
						try (Processor sub = new Processor(project) {
							@Override
							public Properties loadProperties(File source) throws IOException {
								return snapshot.load(source, this);
							}
						}) {
							sub.setProperties(file);
							sub.use(project);
							return collect(sub, file, version, expanded, merged);
						}
					}
				}
			}
			try (Processor input = new Processor() {
				@Override
				public Properties loadProperties(File source) throws IOException {
					return snapshot.load(source, this);
				}
			}) {
				input.setProperties(file);
				if (workspace == null || input.getProperty(Constants.STANDALONE) != null) {
					if (workspace != null) {
						workspace.close();
					}
					workspace = Workspace.createStandaloneWorkspace(input, file.toURI());
				}
			}
			try (Run owner = new Run(workspace, file) {
				@Override
				public Properties loadProperties(File source) throws IOException {
					return snapshot.load(source, this);
				}
			}) {
				return collect(owner, file, version, expanded, merged);
			}
		} finally {
			if (workspace != null) {
				workspace.close();
			}
		}
	}

	private Result collect(Processor owner, File file, Integer version, boolean expanded, boolean merge) {
		boolean merged = expanded && merge;
		Map<String, List<PropertyKey>> groups = new LinkedHashMap<>();
		for (PropertyKey property : PropertyKey.findVisible(owner.getPropertyKeys(key -> true))) {
			String stem = BndEditModel.getStem(property.key());
			String key = merged && (Syntax.isInstruction(stem) || Constants.MERGED_HEADERS.contains(stem))
				? stem : property.key();
			groups.computeIfAbsent(key, ignored -> new ArrayList<>()).add(property);
		}
		List<Row> rows = new ArrayList<>();
		StringBuilder source = new StringBuilder();
		List<String> diagnostics = new ArrayList<>(owner.getErrors());
		diagnostics.addAll(owner.getWarnings());
		for (Map.Entry<String, List<PropertyKey>> entry : groups.entrySet()) {
			String key = entry.getKey();
			List<String> errors = new ArrayList<>();
			String value = "";
			try (Processor evaluation = new Processor(owner)) {
				evaluation.setBase(owner.getBase());
				evaluation.setPropertiesFile(file);
				value = !expanded ? entry.getValue().get(0).getRawValue()
					: merged && (Syntax.isInstruction(key) || Constants.MERGED_HEADERS.contains(key))
						? evaluation.decorated(key).toString() : evaluation.getProperty(key);
				errors.addAll(evaluation.getErrors());
				errors.addAll(evaluation.getWarnings());
			} catch (Exception exception) {
				errors.add(String.valueOf(exception.getMessage()));
			}
			value = value == null ? "" : value;
			String formatted = value;
			try {
				formatted = BndEditModel.format(key, value);
			} catch (Exception exception) {
				errors.add(String.valueOf(exception.getMessage()));
			}
			List<Provenance> provenances = entry.getValue().stream()
				.map(property -> property.getProvenance().orElse("[unknown]"))
				.distinct().map(label -> new Provenance(label, new File(label).isFile()
					? new File(label).toURI().toString() : null)).toList();
			rows.add(new Row(key, value, formatted, provenances, errors));
			source.append(escape(key, true)).append(": ").append(escape(value, false)).append('\n');
		}
		LinkedHashSet<String> dependencies = new LinkedHashSet<>();
		owner.getSelfAndAncestors().forEach(dependency -> dependencies.add(dependency.toURI().toString()));
		return new Result(1, file.toURI().toString(), version, expanded, merged, rows, source.toString(),
			new ArrayList<>(dependencies), diagnostics);
	}

	private static String escape(String text, boolean key) {
		StringBuilder escaped = new StringBuilder();
		for (int index = 0; index < text.length(); index++) {
			char character = text.charAt(index);
			switch (character) {
				case '\\' -> escaped.append("\\\\");
				case '\n' -> escaped.append("\\n");
				case '\r' -> escaped.append("\\r");
				case '\t' -> escaped.append("\\t");
				case ' ', ':', '=', '#', '!' -> {
					if (key || index == 0) escaped.append('\\');
					escaped.append(character);
				}
				default -> escaped.append(character);
			}
		}
		return escaped.toString();
	}

	private record Snapshot(File file, String text) {
		Properties load(File source, Processor reporter) throws IOException {
			UTF8Properties properties = new UTF8Properties();
			if (text != null && file.equals(source.getCanonicalFile())) {
				properties.load(text, source, reporter, Constants.OSGI_SYNTAX_HEADERS);
			} else {
				try {
					properties.load(source, reporter, Constants.OSGI_SYNTAX_HEADERS);
				} catch (Exception exception) {
					throw new IOException("Cannot load " + source, exception);
				}
			}
			return properties.replaceHere(source.getParentFile());
		}
	}
}
