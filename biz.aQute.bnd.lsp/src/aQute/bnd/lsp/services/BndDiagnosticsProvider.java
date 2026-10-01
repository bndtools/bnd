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

import aQute.bnd.build.Project;
import aQute.bnd.build.ProjectBuilder;
import aQute.bnd.build.Run;
import aQute.bnd.build.Workspace;
import aQute.bnd.lsp.Constants;
import aQute.bnd.lsp.models.BndDocumentModel;
import aQute.bnd.lsp.models.BndHeaderClause;
import aQute.bnd.lsp.models.BndHeaderEntry;
import aQute.bnd.osgi.Builder;
import aQute.bnd.osgi.Processor;
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

		try {
			Processor processor = workspaceManager.getProcessorForFile(file);
			if (processor == null) {
				return Collections.emptyList();
			}

			// If it's a project file, build/analyze
			if (processor instanceof Project project) {
				try (ProjectBuilder pb = project.getBuilder(null)) {
					pb.build();
					collectDiagnosticsFromProcessor(pb, model, diagnostics);
				}
			} else if (processor instanceof Run run) {
				run.getRunbundles();
				collectDiagnosticsFromProcessor(run, model, diagnostics);
			} else {
				collectDiagnosticsFromProcessor(processor, model, diagnostics);
			}
		} catch (Exception e) {
			logger.debug("Diagnostics computation exception for {}", file, e);
		}

		return diagnostics;
	}

	private void collectDiagnosticsFromProcessor(Processor p, BndDocumentModel model, List<Diagnostic> diagnostics) {
		for (String error : p.getErrors()) {
			Location loc = p.getLocation(error);
			diagnostics.add(createDiagnostic(error, loc, DiagnosticSeverity.Error, model));
		}

		for (String warning : p.getWarnings()) {
			Location loc = p.getLocation(warning);
			diagnostics.add(createDiagnostic(warning, loc, DiagnosticSeverity.Warning, model));
		}
	}

	private Diagnostic createDiagnostic(String message, Location loc, DiagnosticSeverity severity,
		BndDocumentModel model) {
		Diagnostic d = new Diagnostic();
		d.setMessage(message);
		d.setSeverity(severity);
		d.setSource(Constants.DIAGNOSTIC_SOURCE_BND);

		Range range = computeRange(loc, model);
		d.setRange(range);

		if (loc != null && loc.details != null) {
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
