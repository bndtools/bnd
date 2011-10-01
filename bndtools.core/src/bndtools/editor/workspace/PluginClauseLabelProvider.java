package bndtools.editor.workspace;

import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

import org.eclipse.jface.viewers.StyledCellLabelProvider;
import org.eclipse.jface.viewers.StyledString;
import org.eclipse.jface.viewers.ViewerCell;
import org.eclipse.swt.graphics.Image;
import org.eclipse.ui.plugin.AbstractUIPlugin;

import bndtools.Plugin;
import bndtools.model.clauses.HeaderClause;

public class PluginClauseLabelProvider extends StyledCellLabelProvider {

    private Image pluginImg = null;

    @Override
    public void update(ViewerCell cell) {
        HeaderClause header = (HeaderClause) cell.getElement();

        StyledString label = new StyledString(header.getName());

        Map<String, String> attribs = header.getAttribs();
        if (!attribs.isEmpty())
            label.append(" ");
        for (Iterator<Entry<String, String>> iter = attribs.entrySet().iterator(); iter.hasNext(); ) {
            Entry<String, String> entry = iter.next();
            label.append(entry.getKey(), StyledString.QUALIFIER_STYLER);
            label.append("=", StyledString.QUALIFIER_STYLER);
            label.append(entry.getValue(), StyledString.COUNTER_STYLER);

            if (iter.hasNext())
                label.append(", ");
        }

        cell.setText(label.toString());
        cell.setStyleRanges(label.getStyleRanges());

        if (pluginImg == null)
            pluginImg = AbstractUIPlugin.imageDescriptorFromPlugin(Plugin.PLUGIN_ID, "/icons/plugin.png").createImage();
        cell.setImage(pluginImg);
    }

    @Override
    public void dispose() {
        super.dispose();
        if (pluginImg != null)
            pluginImg.dispose();
    }
}
