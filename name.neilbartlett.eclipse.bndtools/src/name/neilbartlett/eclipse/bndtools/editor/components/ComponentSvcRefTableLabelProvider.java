package name.neilbartlett.eclipse.bndtools.editor.components;

import name.neilbartlett.eclipse.bndtools.Plugin;
import name.neilbartlett.eclipse.bndtools.editor.model.ComponentSvcReference;

import org.eclipse.jface.viewers.StyledCellLabelProvider;
import org.eclipse.jface.viewers.StyledString;
import org.eclipse.jface.viewers.ViewerCell;
import org.eclipse.swt.graphics.Image;
import org.eclipse.ui.plugin.AbstractUIPlugin;

public class ComponentSvcRefTableLabelProvider extends StyledCellLabelProvider {
	
	private Image dynamicImg = AbstractUIPlugin.imageDescriptorFromPlugin(Plugin.PLUGIN_ID, "/icons/lightning.png").createImage();
	private Image staticImg = AbstractUIPlugin.imageDescriptorFromPlugin(Plugin.PLUGIN_ID, "/icons/anchor.png").createImage();

	@Override
	public void update(ViewerCell cell) {
		ComponentSvcReference svcRef = (ComponentSvcReference) cell.getElement();
		int columnIndex = cell.getColumnIndex();
		StyledString styledString;
		switch (columnIndex) {
		case 0:
			styledString = new StyledString(svcRef.getName());
			
			String bind = svcRef.getBind();
			String unbind = svcRef.getUnbind();
			if(bind != null) {
				StringBuilder buffer = new StringBuilder();
				buffer.append(" {").append(bind).append('/');
				if(unbind != null) {
					buffer.append(unbind);
				}
				buffer.append('}');
				styledString.append(buffer.toString(), StyledString.DECORATIONS_STYLER);
			}
			cell.setImage(svcRef.isDynamic() ? dynamicImg : staticImg);
			cell.setText(styledString.toString());
			cell.setStyleRanges(styledString.getStyleRanges());
			break;
		case 1:
			styledString = new StyledString(svcRef.getServiceClass(), StyledString.QUALIFIER_STYLER);
			cell.setText(styledString.toString());
			cell.setStyleRanges(styledString.getStyleRanges());
			break;
		case 2:
			char[] cardinality = new char[] {
					svcRef.isOptional() ? '0' : '1',
					'.',
					'.',
					svcRef.isMultiple() ? 'n' : '1'};
			styledString = new StyledString(new String(cardinality), StyledString.COUNTER_STYLER);
			cell.setText(styledString.toString());
			cell.setStyleRanges(styledString.getStyleRanges());
			break;
		case 3:
			String target = svcRef.getTargetFilter();
			cell.setText(target != null ? target : "");
			break;
		}
	}
	
	@Override
	public void dispose() {
		super.dispose();
		dynamicImg.dispose();
		staticImg.dispose();
	}

}
