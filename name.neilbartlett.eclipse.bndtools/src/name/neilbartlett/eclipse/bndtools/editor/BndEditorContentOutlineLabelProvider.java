package name.neilbartlett.eclipse.bndtools.editor;

import name.neilbartlett.eclipse.bndtools.Plugin;
import name.neilbartlett.eclipse.bndtools.editor.model.ExportedPackage;
import name.neilbartlett.eclipse.bndtools.editor.model.ImportPattern;
import name.neilbartlett.eclipse.bndtools.editor.model.ServiceComponent;

import org.eclipse.jface.viewers.StyledCellLabelProvider;
import org.eclipse.jface.viewers.ViewerCell;
import org.eclipse.swt.graphics.Image;
import org.eclipse.ui.plugin.AbstractUIPlugin;

public class BndEditorContentOutlineLabelProvider extends StyledCellLabelProvider {
	
	final Image pageImg = AbstractUIPlugin.imageDescriptorFromPlugin(Plugin.PLUGIN_ID, "/icons/page_white_text.png").createImage();
	final Image packageImg = AbstractUIPlugin.imageDescriptorFromPlugin(Plugin.PLUGIN_ID, "/icons/package_obj.gif").createImage();
	final Image brickImg = AbstractUIPlugin.imageDescriptorFromPlugin(Plugin.PLUGIN_ID, "/icons/brick.png").createImage();
	
	@Override
	public void update(ViewerCell cell) {
		Object element = cell.getElement();
		
		if(element instanceof String) {
			// Top-level placeholder
			if(BndEditor.OVERVIEW_PAGE.equals(element)) {
				cell.setText("Overview");
			} else if(BndEditor.COMPONENTS_PAGE.equals(element)) {
				cell.setText("Components");
			} else if(BndEditor.EXPORTS_PAGE.equals(element)) {
				cell.setText("Exports");
			} else if(BndEditor.IMPORTS_PAGE.equals(element)) {
				cell.setText("Imports");
			} else if(BndEditor.SOURCE_PAGE.equals(element)) {
				cell.setText("Source");
			}
			cell.setImage(pageImg);
		} else if(element instanceof ServiceComponent) {
			ServiceComponent component = (ServiceComponent) element;
			cell.setText(component.getName());
			cell.setImage(brickImg);
		} else if(element instanceof ExportedPackage) {
			cell.setText(((ExportedPackage) element).getName());
			cell.setImage(packageImg);
		} else if(element instanceof ImportPattern) {
			cell.setText(((ImportPattern) element).getName());
			cell.setImage(packageImg);
		}
	}
	
	@Override
	public void dispose() {
		super.dispose();
		pageImg.dispose();
		packageImg.dispose();
		brickImg.dispose();
	}
}
