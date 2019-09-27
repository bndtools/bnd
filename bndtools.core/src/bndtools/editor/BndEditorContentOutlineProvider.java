package bndtools.editor;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.Viewer;

import aQute.bnd.build.Workspace;
import aQute.bnd.build.model.BndEditModel;
import aQute.bnd.build.model.clauses.ExportedPackage;
import aQute.bnd.build.model.clauses.HeaderClause;
import aQute.bnd.build.model.clauses.ImportPattern;
import aQute.bnd.osgi.Constants;
import bndtools.launch.LaunchConstants;

public class BndEditorContentOutlineProvider implements ITreeContentProvider, PropertyChangeListener {

	static final String			PRIVATE_PKGS	= "__private_pkgs";
	static final String			EXPORTS			= "__exports";
	static final String			IMPORT_PATTERNS	= "__import_patterns";
	static final String			PLUGINS			= "__plugins";

	private BndEditModel		model;
	private final TreeViewer	viewer;

	public BndEditorContentOutlineProvider(TreeViewer viewer) {
		this.viewer = viewer;
	}

	@Override
	public Object[] getElements(Object inputElement) {
		Object[] result;
		if (model.isProjectFile()) {
			result = new String[] {
				PRIVATE_PKGS, EXPORTS, IMPORT_PATTERNS, BndEditor.BUILD_PAGE, BndEditor.PROJECT_RUN_PAGE,
				BndEditor.SOURCE_PAGE
			};
		} else if (model.getBndResourceName()
			.endsWith(LaunchConstants.EXT_BNDRUN)) {
			result = new String[] {
				BndEditor.PROJECT_RUN_PAGE, BndEditor.SOURCE_PAGE
			};
		} else if (Workspace.BUILDFILE.equals(model.getBndResourceName())) {
			result = new String[] {
				PLUGINS, BndEditor.SOURCE_PAGE
			};
		} else {
			result = new String[] {
				PRIVATE_PKGS, EXPORTS, IMPORT_PATTERNS, BndEditor.SOURCE_PAGE
			};
		}
		return result;
	}

	@Override
	public void dispose() {
		if (model != null)
			model.removePropertyChangeListener(this);
	}

	@Override
	public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
		if (model != null)
			model.removePropertyChangeListener(this);

		model = (BndEditModel) newInput;
		if (model != null)
			model.addPropertyChangeListener(this);
	}

	@Override
	public Object[] getChildren(Object parentElement) {
		Object[] result = new Object[0];

		if (parentElement instanceof String) {
			if (EXPORTS.equals(parentElement)) {
				List<ExportedPackage> exports = model.getExportedPackages();
				if (exports != null)
					result = exports.toArray();
			} else if (PRIVATE_PKGS.equals(parentElement)) {
				List<String> packages = model.getPrivatePackages();
				if (packages != null) {
					List<PrivatePkg> wrapped = new ArrayList<>(packages.size());
					for (String pkg : packages) {
						wrapped.add(new PrivatePkg(pkg));
					}
					result = wrapped.toArray();
				}
			} else if (IMPORT_PATTERNS.equals(parentElement)) {
				List<ImportPattern> imports = model.getImportPatterns();
				if (imports != null)
					result = imports.toArray();
			} else if (PLUGINS.equals(parentElement)) {
				List<HeaderClause> plugins = model.getPlugins();
				if (plugins != null) {
					List<PluginClause> wrapped = new ArrayList<>(plugins.size());
					for (HeaderClause header : plugins)
						wrapped.add(new PluginClause(header));
					result = wrapped.toArray(new PluginClause[0]);
				}
			}
		}
		return result;
	}

	@Override
	public Object getParent(Object element) {
		return null;
	}

	@Override
	public boolean hasChildren(Object element) {
		if (element instanceof String) {
			if (EXPORTS.equals(element)) {
				List<ExportedPackage> exports = model.getExportedPackages();
				return exports != null && !exports.isEmpty();
			}
			if (PRIVATE_PKGS.equals(element)) {
				List<String> packages = model.getPrivatePackages();
				return packages != null && !packages.isEmpty();
			}
			if (IMPORT_PATTERNS.equals(element)) {
				List<ImportPattern> imports = model.getImportPatterns();
				return imports != null && !imports.isEmpty();
			}
			if (PLUGINS.equals(element)) {
				List<HeaderClause> plugins = model.getPlugins();
				return plugins != null && !plugins.isEmpty();
			}
		}
		return false;
	}

	@Override
	public void propertyChange(PropertyChangeEvent evt) {
		if (Constants.EXPORT_PACKAGE.equals(evt.getPropertyName())) {
			viewer.refresh(EXPORTS);
			viewer.expandToLevel(EXPORTS, 1);
		} else if (Constants.PRIVATE_PACKAGE.equals(evt.getPropertyName())) {
			viewer.refresh(PRIVATE_PKGS);
			viewer.expandToLevel(PRIVATE_PKGS, 1);
		} else if (Constants.IMPORT_PACKAGE.equals(evt.getPropertyName())) {
			viewer.refresh(IMPORT_PATTERNS);
			viewer.expandToLevel(IMPORT_PATTERNS, 1);
		} else if (Constants.PLUGIN.equals(evt.getPropertyName())) {
			viewer.refresh(PLUGINS);
			viewer.expandToLevel(PLUGINS, 1);
		}
	}
}

class PrivatePkg {
	final String pkg;

	PrivatePkg(String pkg) {
		this.pkg = pkg;
	}
}

class PluginClause {
	final HeaderClause header;

	PluginClause(HeaderClause header) {
		this.header = header;
	}
}
