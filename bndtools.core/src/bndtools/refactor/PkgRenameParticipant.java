package bndtools.refactor;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.bndtools.api.ILogger;
import org.bndtools.api.Logger;
import org.bndtools.utils.workspace.FileUtils;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceProxyVisitor;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.internal.corext.refactoring.rename.RenamePackageProcessor;
import org.eclipse.ltk.core.refactoring.Change;
import org.eclipse.ltk.core.refactoring.CompositeChange;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;
import org.eclipse.ltk.core.refactoring.TextChange;
import org.eclipse.ltk.core.refactoring.TextFileChange;
import org.eclipse.ltk.core.refactoring.participants.CheckConditionsContext;
import org.eclipse.ltk.core.refactoring.participants.ISharableParticipant;
import org.eclipse.ltk.core.refactoring.participants.RefactoringArguments;
import org.eclipse.ltk.core.refactoring.participants.RenameArguments;
import org.eclipse.ltk.core.refactoring.participants.RenameParticipant;
import org.eclipse.text.edits.MultiTextEdit;
import org.eclipse.text.edits.ReplaceEdit;
import org.eclipse.text.edits.TextEdit;

import aQute.bnd.build.model.BndEditModel;
import aQute.bnd.build.model.clauses.ExportedPackage;
import aQute.bnd.build.model.clauses.HeaderClause;
import aQute.bnd.build.model.clauses.ImportPattern;
import aQute.bnd.properties.Document;
import aQute.bnd.properties.IDocument;
import aQute.bnd.properties.IRegion;
import aQute.bnd.properties.LineType;
import aQute.bnd.properties.PropertiesLineReader;

public class PkgRenameParticipant extends RenameParticipant implements ISharableParticipant {
	private static final ILogger							logger			= Logger
		.getLogger(PkgRenameParticipant.class);

	private final Map<IPackageFragment, RenameArguments>	pkgFragments	= new HashMap<>();
	private String											changeTitle		= null;

	@Override
	protected boolean initialize(Object element) {
		IPackageFragment pkgFragment = (IPackageFragment) element;
		RenameArguments args = getArguments();
		pkgFragments.put(pkgFragment, args);

		StringBuilder sb = new StringBuilder(256);
		sb.append("Bndtools: rename package '");
		sb.append(pkgFragment.getElementName());
		sb.append("' ");
		if (((RenamePackageProcessor) this.getProcessor()).getRenameSubpackages())
			sb.append("and subpackages ");
		sb.append("to '");
		sb.append(args.getNewName());
		sb.append("'");
		changeTitle = sb.toString();

		return true;
	}

	@Override
	public void addElement(Object element, RefactoringArguments arguments) {
		this.pkgFragments.put((IPackageFragment) element, (RenameArguments) arguments);
	}

	@Override
	public String getName() {
		return "Bndtools Package Rename Participant";
	}

	@Override
	public RefactoringStatus checkConditions(IProgressMonitor pm, CheckConditionsContext context)
		throws OperationCanceledException {
		return new RefactoringStatus();
	}

	private static final String grammarSeparator = "[\\s,\"';]";

	@Override
	public Change createChange(IProgressMonitor pm) throws CoreException, OperationCanceledException {
		final Map<IFile, TextChange> fileChanges = new HashMap<>();

		IResourceProxyVisitor visitor = proxy -> {
			if ((proxy.getType() == IResource.FOLDER) || (proxy.getType() == IResource.PROJECT)) {
				return true;
			}

			if (!((proxy.getType() == IResource.FILE) && proxy.getName()
				.toLowerCase()
				.endsWith(".bnd"))) {
				return false;
			}

			/* we're dealing with a *.bnd file */

			/* get the proxied file */
			IFile resource = (IFile) proxy.requestResource();

			/* read the file as a single string */
			String bndFileText = null;
			try {
				bndFileText = FileUtils.readFully(resource)
					.get();
			} catch (Exception e1) {
				String str1 = "Could not read file " + proxy.getName();
				logger.logError(str1, e1);
				throw new OperationCanceledException(str1);
			}

			/*
			 * get the previous change for this file if it exists, or otherwise
			 * create a new change for it, but do not store it yet: wait until
			 * we know if there are actually changes in the file
			 */
			TextChange fileChange = getTextChange(resource);
			final boolean fileChangeIsNew = (fileChange == null);
			if (fileChange == null) {
				fileChange = new TextFileChange(proxy.getName(), resource);
				fileChange.setEdit(new MultiTextEdit());
			}
			TextEdit rootEdit = fileChange.getEdit();

			BndEditModel model = new BndEditModel();
			Document document = new Document(bndFileText);

			try {
				model.loadFrom(document);
			} catch (IOException e2) {
				String str2 = "Could not load document " + proxy.getName();
				logger.logError(str2, e2);
				throw new OperationCanceledException(str2);
			}

			/* loop over all renames to perform */
			for (Map.Entry<IPackageFragment, RenameArguments> entry : pkgFragments.entrySet()) {
				IPackageFragment pkgFragment = entry.getKey();
				RenameArguments arguments = entry.getValue();

				final String oldName = pkgFragment.getElementName();
				final String newName = arguments.getNewName();

				List<ImportPattern> newImportedPackages = makeNewHeaders(model.getImportPatterns(), oldName, newName);
				if (newImportedPackages != null) {
					model.setImportPatterns(newImportedPackages);
				}

				List<ExportedPackage> newExportedPackages = makeNewHeaders(model.getExportedPackages(), oldName,
					newName);
				if (newExportedPackages != null) {
					model.setExportedPackages(newExportedPackages);
				}

				List<String> newPrivatePackages = makeNewHeaders(model.getPrivatePackages(), oldName, newName);
				if (newPrivatePackages != null) {
					model.setPrivatePackages(newPrivatePackages);
				}

				Map<String, String> changes = model.getDocumentChanges();

				for (Iterator<Entry<String, String>> iter = changes.entrySet()
					.iterator(); iter.hasNext();) {
					Entry<String, String> change = iter.next();

					String propertyName = change.getKey();
					String stringValue = change.getValue();

					addEdit(document, rootEdit, propertyName, stringValue);

					iter.remove();
				}

				Pattern pattern = Pattern.compile(/* match start boundary */"(^|" + grammarSeparator + ")" +
				/* match bundle activator */"(Bundle-Activator\\s*:\\s*)" +
				/* match itself / package name */"(" + Pattern.quote(oldName) + ")" +
				/* match class name */"(\\.[^\\.]+)" +
				/* match end boundary */"(" + grammarSeparator + "|" + Pattern.quote("\\") + "|$)");

				/* find all matches to replace and add them to the root edit */
				Matcher matcher = pattern.matcher(bndFileText);
				while (matcher.find()) {
					rootEdit.addChild(new ReplaceEdit(matcher.start(3), matcher.group(3)
						.length(), newName));
				}
			}

			/*
			 * only store the changes when no changes were stored before for
			 * this file and when there are actually changes.
			 */
			if (fileChangeIsNew && rootEdit.hasChildren()) {
				fileChanges.put(resource, fileChange);
			}

			return false;
		};

		/* determine which projects have to be visited */
		Set<IProject> projectsToVisit = new HashSet<>();
		for (IPackageFragment pkgFragment : pkgFragments.keySet()) {
			projectsToVisit.add(pkgFragment.getResource()
				.getProject());
			for (IProject projectToVisit : pkgFragment.getResource()
				.getProject()
				.getReferencingProjects()) {
				projectsToVisit.add(projectToVisit);
			}
			for (IProject projectToVisit : pkgFragment.getResource()
				.getProject()
				.getReferencedProjects()) {
				projectsToVisit.add(projectToVisit);
			}
		}

		/* visit the projects */
		for (IProject projectToVisit : projectsToVisit) {
			projectToVisit.accept(visitor, IResource.NONE);
		}

		if (fileChanges.isEmpty()) {
			/* no changes at all */
			return null;
		}

		/* build a composite change with all changes */
		CompositeChange cs = new CompositeChange(changeTitle);
		for (TextChange fileChange : fileChanges.values()) {
			cs.add(fileChange);
		}

		return cs;
	}

	@SuppressWarnings("unchecked")
	private static <T> List<T> makeNewHeaders(List<T> headers, String oldName, String newName) {
		if (headers != null) {
			boolean changed = false;
			List<T> newHeaders = new ArrayList<>();

			for (T header : headers) {
				if (header instanceof HeaderClause) {
					HeaderClause newHeader = ((HeaderClause) header).clone();
					newHeaders.add((T) newHeader);

					if (newHeader.getName()
						.equals(oldName)) {
						newHeader.setName(newName);
						changed = true;
					}
				} else if (header instanceof String) {
					String newPrivatePackage = header.toString();

					if (newPrivatePackage.equals(oldName)) {
						newPrivatePackage = newName;
						changed = true;
					}

					newHeaders.add((T) newPrivatePackage);
				}
			}

			if (changed) {
				return newHeaders;
			}
		}

		return null;
	}

	private static IRegion findEntry(IDocument document, String name) {
		PropertiesLineReader reader = new PropertiesLineReader(document);
		try {
			LineType type = reader.next();
			while (type != LineType.eof) {
				if (type == LineType.entry) {
					String key = reader.key();
					if (name.equals(key))
						return reader.region();
				}
				type = reader.next();
			}
		} catch (Exception e) {}
		return null;
	}

	/**
	 * Copied from BndEditModel#updateDocument
	 */
	private static void addEdit(IDocument document, TextEdit rootEdit, String name, String value) {
		String newEntry;
		if (value != null) {
			StringBuilder buffer = new StringBuilder();
			buffer.append(name)
				.append(": ")
				.append(value);
			newEntry = buffer.toString();
		} else {
			newEntry = "";
		}

		try {
			IRegion region = findEntry(document, name);
			if (region != null) {
				// Replace an existing entry
				int offset = region.getOffset();
				int length = region.getLength();

				// If the replacement is empty, remove one extra character to
				// the right, i.e. the following newline,
				// unless this would take us past the end of the document
				if (newEntry.length() == 0 && offset + length + 1 < document.getLength()) {
					length++;
				}
				rootEdit.addChild(new ReplaceEdit(offset, length, newEntry));
			} else if (newEntry.length() > 0) {
				// This is a new entry, put it at the end of the file

				// Does the last line of the document have a newline? If not,
				// we need to add one.
				if (document.getLength() > 0 && document.getChar(document.getLength() - 1) != '\n')
					newEntry = "\n" + newEntry;
				rootEdit.addChild(new ReplaceEdit(document.getLength(), 0, newEntry));
			}
		} catch (Exception e) {}
	}
}
