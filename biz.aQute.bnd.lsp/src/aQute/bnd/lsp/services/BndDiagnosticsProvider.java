package aQute.bnd.lsp.services;

import java.io.File;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.eclipse.lsp4j.Diagnostic;
import org.eclipse.lsp4j.DiagnosticSeverity;
import org.eclipse.lsp4j.Position;
import org.eclipse.lsp4j.Range;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.JsonObject;

import aQute.bnd.build.Project;
import aQute.bnd.build.ProjectBuilder;
import aQute.bnd.build.Run;
import aQute.bnd.differ.Baseline.BundleInfo;
import aQute.bnd.lsp.Constants;
import aQute.bnd.lsp.models.BndDocumentModel;
import aQute.bnd.lsp.models.BndHeaderClause;
import aQute.bnd.lsp.models.BndHeaderEntry;
import aQute.bnd.osgi.Processor;
import aQute.bnd.osgi.PropertyConflict;
import aQute.bnd.osgi.Verifier.BundleActivatorError;
import aQute.service.reporter.Report.Location;

public class BndDiagnosticsProvider {
	private static final Logger			logger	= LoggerFactory.getLogger(BndDiagnosticsProvider.class);

	private final BndWorkspaceManager	workspaceManager;

	public BndDiagnosticsProvider(BndWorkspaceManager workspaceManager) {
		this.workspaceManager = workspaceManager;
	}

	public List<Diagnostic> computeDiagnostics(BndDocumentModel model) {
		File file = getFileFromUri(model.getUri());
		if (file == null || !file.exists()) {
			return Collections.emptyList();
		}

		List<Diagnostic> diagnostics = new ArrayList<>();
		Processor processor = null;

		try {
			processor = workspaceManager.getProcessorForFile(file);

			// If it's a project file, build/analyze
			if (processor instanceof Project project) {
				try (ProjectBuilder pb = project.getBuilder(null)) {
					pb.build();
					collectDiagnosticsFromProcessor(pb, model, diagnostics);
				}
			} else if (processor instanceof Run run) {
				run.getRunbundles();
				collectDiagnosticsFromProcessor(run, model, diagnostics);
			} else if (processor != null) {
				collectDiagnosticsFromProcessor(processor, model, diagnostics);
			}
		} catch (Exception e) {
			logger.debug("Diagnostics computation exception for {}", file, e);
		}

		diagnostics.addAll(computePropertyConflicts(model, file, processor));
		return diagnostics;
	}

	/**
	 * Validates the editor text through bnd's property-conflict analysis. The
	 * owning processor supplies inherited settings such as -propertyconflicts;
	 * when the file is that processor's own properties file, its parent is used
	 * so the unsaved text replaces the on-disk definitions.
	 */
	public List<Diagnostic> computePropertyConflicts(BndDocumentModel model, File file, Processor owner) {
		Processor parent = owner;
		if (parent != null && file.getAbsoluteFile()
			.equals(parent.getPropertiesFile())) {
			parent = parent.getParent();
		}
		List<Diagnostic> diagnostics = new ArrayList<>();
		try (Processor snapshot = PropertyConflict.analyze(parent, file, model.getText())) {
			collectPropertyConflicts(snapshot, snapshot.getErrors(), DiagnosticSeverity.Error, model, file, diagnostics);
			collectPropertyConflicts(snapshot, snapshot.getWarnings(), DiagnosticSeverity.Warning, model, file,
				diagnostics);
		} catch (Exception e) {
			logger.debug("Property conflict analysis failed for {}", file, e);
		}
		return diagnostics;
	}

	private void collectPropertyConflicts(Processor snapshot, List<String> messages, DiagnosticSeverity severity,
		BndDocumentModel model, File file, List<Diagnostic> diagnostics) {
		File source = file.getAbsoluteFile();
		for (String message : messages) {
			Location loc = snapshot.getLocation(message);
			if (loc == null || !(loc.details instanceof PropertyConflict conflict))
				continue;
			List<PropertyConflict.Occurrence> occurrences = conflict.occurrences();
			// duplicates: every definition after the first is a lost or
			// overriding one; includes: the shadowed root definition
			int first = conflict.kind() == PropertyConflict.Kind.DUPLICATE ? 1 : 0;
			for (int index = first; index < occurrences.size(); index++) {
				PropertyConflict.Occurrence occurrence = occurrences.get(index);
				if (occurrence.source() == null || !source.equals(new File(occurrence.source()).getAbsoluteFile()))
					continue;
				if (conflict.kind() == PropertyConflict.Kind.INCLUDE && occurrence.equals(conflict.winner()))
					continue;
				Diagnostic d = new Diagnostic();
				d.setMessage(message);
				d.setSeverity(severity);
				d.setSource(Constants.DIAGNOSTIC_SOURCE_BND);
				d.setCode(Constants.DIAGNOSTIC_CODE_PROPERTY_CONFLICT);
				d.setRange(occurrenceRange(occurrence, model));
				JsonObject data = new JsonObject();
				data.addProperty("key", conflict.key());
				data.addProperty("kind", conflict.kind()
					.name());
				data.addProperty("mergeable", conflict.mergeable());
				data.addProperty("line", occurrence.line());
				data.addProperty("start", occurrence.start());
				data.addProperty("end", occurrence.end());
				d.setData(data);
				diagnostics.add(d);
			}
		}
	}

	private static Range occurrenceRange(PropertyConflict.Occurrence occurrence, BndDocumentModel model) {
		if (occurrence.start() >= 0 && occurrence.end() >= occurrence.start()) {
			return new Range(model.offsetToPosition(occurrence.start()), model.offsetToPosition(occurrence.end()));
		}
		int line = Math.max(0, occurrence.line());
		int length = 0;
		try {
			length = model.getLineInformation(line)
				.getLength();
		} catch (Exception e) {
			// keep zero-length range
		}
		return new Range(new Position(line, 0), new Position(line, length));
	}

	private void collectDiagnosticsFromProcessor(Processor p, BndDocumentModel model, List<Diagnostic> diagnostics) {
		for (String error : p.getErrors()) {
			Location loc = p.getLocation(error);
			if (isPropertyConflict(loc))
				continue;
			diagnostics.add(createDiagnostic(error, loc, DiagnosticSeverity.Error, model));
		}

		for (String warning : p.getWarnings()) {
			Location loc = p.getLocation(warning);
			if (isPropertyConflict(loc))
				continue;
			diagnostics.add(createDiagnostic(warning, loc, DiagnosticSeverity.Warning, model));
		}
	}

	/** Conflicts are reported from the editor snapshot with exact offsets instead. */
	private static boolean isPropertyConflict(Location loc) {
		return loc != null && loc.details instanceof PropertyConflict;
	}

	private Diagnostic createDiagnostic(String message, Location loc, DiagnosticSeverity severity,
		BndDocumentModel model) {
		Diagnostic d = new Diagnostic();
		d.setMessage(message);
		d.setSeverity(severity);
		d.setSource(Constants.DIAGNOSTIC_SOURCE_BND);

		Range range = computeRange(loc, model);
		d.setRange(range);

		if (loc != null && loc.details instanceof BundleActivatorError activator) {
			d.setCode(Constants.DIAGNOSTIC_CODE_BUNDLE_ACTIVATOR);
			JsonObject data = new JsonObject();
			data.addProperty("activatorClassName", activator.activatorClassName);
			data.addProperty("errorType", String.valueOf(activator.errorType));
			d.setData(data);
		} else if (loc != null && loc.details instanceof BundleInfo info && info.suggestedVersion != null) {
			d.setCode(Constants.DIAGNOSTIC_CODE_BUNDLE_VERSION);
			JsonObject data = new JsonObject();
			data.addProperty("suggestedVersion", info.suggestedVersion.toString());
			d.setData(data);
		} else if (loc != null && loc.details != null) {
			d.setData(loc.details.toString());
		}

		return d;
	}

	private Range computeRange(Location loc, BndDocumentModel model) {
		if (loc != null) {
			if (loc.line > 0) {
				int lspLine = loc.line - 1;
				int charStart = 0;
				int charEnd = 100;
				try {
					int len = model.getLineInformation(lspLine)
						.getLength();
					charEnd = len;
				} catch (Exception e) {
					// ignore
				}
				if (loc.length > 0) {
					charEnd = Math.min(charEnd, charStart + loc.length);
				}
				return new Range(new Position(lspLine, charStart), new Position(lspLine, charEnd));
			}

			if (loc.header != null) {
				for (BndHeaderEntry entry : model.getHeaderEntries()) {
					if (entry.getKey()
						.equalsIgnoreCase(loc.header)) {
						if (loc.context != null) {
							for (BndHeaderClause clause : entry.getClauses()) {
								if (clause.getName()
									.contains(loc.context)) {
									return new Range(new Position(clause.getStartLine(), 0),
										new Position(clause.getEndLine(), clause.getLength()));
								}
							}
						}
						return new Range(new Position(entry.getStartLine(), 0),
							new Position(entry.getStartLine(), entry.getKeyLength()));
					}
				}
			}
		}

		// Default range: line 0
		return new Range(new Position(0, 0), new Position(0, 0));
	}

	private static File getFileFromUri(String uriStr) {
		try {
			URI uri = URI.create(uriStr);
			if ("file".equalsIgnoreCase(uri.getScheme())) {
				return new File(uri.getPath());
			}
		} catch (Exception e) {
			// ignore
		}
		return null;
	}
}
