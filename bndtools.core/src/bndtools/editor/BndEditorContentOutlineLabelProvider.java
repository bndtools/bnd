package bndtools.editor;

import org.bndtools.core.ui.icons.Icons;
import org.eclipse.jface.viewers.StyledCellLabelProvider;
import org.eclipse.jface.viewers.ViewerCell;
import org.eclipse.swt.graphics.Image;
import org.eclipse.ui.plugin.AbstractUIPlugin;

import aQute.bnd.build.model.clauses.ExportedPackage;
import aQute.bnd.build.model.clauses.ImportPattern;
import bndtools.Plugin;

public class BndEditorContentOutlineLabelProvider extends StyledCellLabelProvider {

	final Image	pageImg		= AbstractUIPlugin.imageDescriptorFromPlugin(Plugin.PLUGIN_ID, "/icons/page_white_text.png")
		.createImage();
	final Image	packageImg	= Icons.desc("package")
		.createImage();
	final Image	brickImg	= Icons.desc("bundle")
		.createImage();
	final Image	pluginImg	= AbstractUIPlugin.imageDescriptorFromPlugin(Plugin.PLUGIN_ID, "/icons/plugin.png")
		.createImage();

	@Override
	public void update(ViewerCell cell) {
		Object element = cell.getElement();

		if (element instanceof String) {
			// Top-level placeholder
			if (BndEditor.BUILD_PAGE.equals(element)) {
				cell.setText("Build");
			} else if (BndEditor.PROJECT_RUN_PAGE.equals(element)) {
				cell.setText("Run");
			} else if (BndEditorContentOutlineProvider.EXPORTS.equals(element)) {
				cell.setText("Exports");
			} else if (BndEditorContentOutlineProvider.PRIVATE_PKGS.equals(element)) {
				cell.setText("Private Packages");
			} else if (BndEditorContentOutlineProvider.IMPORT_PATTERNS.equals(element)) {
				cell.setText("Import Patterns");
			} else if (BndEditor.SOURCE_PAGE.equals(element)) {
				cell.setText("Source");
			} else if (BndEditorContentOutlineProvider.PLUGINS.equals(element)) {
				cell.setText("Plugins");
			}
			cell.setImage(pageImg);
		} else if (element instanceof ExportedPackage) {
			cell.setText(((ExportedPackage) element).getName());
			cell.setImage(packageImg);
		} else if (element instanceof ImportPattern) {
			cell.setText(((ImportPattern) element).getName());
			cell.setImage(packageImg);
		} else if (element instanceof PrivatePkg) {
			cell.setText(((PrivatePkg) element).pkg);
			cell.setImage(packageImg);
		} else if (element instanceof PluginClause) {
			cell.setText(((PluginClause) element).header.getName());
			cell.setImage(pluginImg);
		}
	}

	@Override
	public void dispose() {
		super.dispose();
		pageImg.dispose();
		packageImg.dispose();
		brickImg.dispose();
		pluginImg.dispose();
	}
}
