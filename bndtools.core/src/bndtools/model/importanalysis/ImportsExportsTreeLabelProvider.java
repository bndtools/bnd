/*******************************************************************************
 * Copyright (c) 2010 Neil Bartlett.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Neil Bartlett - initial API and implementation
 *******************************************************************************/
package bndtools.model.importanalysis;

import static bndtools.model.importanalysis.ImportsExportsTreeContentProvider.EXPORTS_PLACEHOLDER;
import static bndtools.model.importanalysis.ImportsExportsTreeContentProvider.IMPORTS_PLACEHOLDER;
import static bndtools.model.importanalysis.ImportsExportsTreeContentProvider.REQUIRED_PLACEHOLDER;

import java.util.Map;
import java.util.Map.Entry;

import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.viewers.StyledCellLabelProvider;
import org.eclipse.jface.viewers.StyledString;
import org.eclipse.jface.viewers.ViewerCell;
import org.eclipse.swt.graphics.Image;
import org.eclipse.ui.plugin.AbstractUIPlugin;

import aQute.bnd.build.model.clauses.HeaderClause;
import aQute.bnd.osgi.Constants;
import bndtools.Plugin;
import bndtools.UIConstants;
import bndtools.model.importanalysis.ImportsExportsTreeContentProvider.ExportUsesPackage;
import bndtools.model.importanalysis.ImportsExportsTreeContentProvider.ImportUsedByClass;
import bndtools.model.importanalysis.ImportsExportsTreeContentProvider.ImportUsedByPackage;

public class ImportsExportsTreeLabelProvider extends StyledCellLabelProvider {

    private final Image pkgFolderImg = AbstractUIPlugin.imageDescriptorFromPlugin(Plugin.PLUGIN_ID, "/icons/packagefolder_obj.gif").createImage();
    private final Image bundleFolderImg = AbstractUIPlugin.imageDescriptorFromPlugin(Plugin.PLUGIN_ID, "/icons/bundlefolder.png").createImage();
    private final Image classImg = AbstractUIPlugin.imageDescriptorFromPlugin(Plugin.PLUGIN_ID, "/icons/class_obj.gif").createImage();

    private final ImageDescriptor packageDescriptor = AbstractUIPlugin.imageDescriptorFromPlugin(Plugin.PLUGIN_ID, "/icons/package_obj.gif");
    private final Image packageImg = packageDescriptor.createImage();

    // private final ImageDescriptor questionOverlay =
    // AbstractUIPlugin.imageDescriptorFromPlugin(Plugin.PLUGIN_ID,
    // "/icons/question_overlay.gif");
    // private final Image packageOptImg = new DecorationOverlayIcon(packageImg,
    // questionOverlay, IDecoration.TOP_LEFT).createImage();
    private final Image packageOptImg = AbstractUIPlugin.imageDescriptorFromPlugin(Plugin.PLUGIN_ID, "/icons/package_opt.gif").createImage();
    private final Image packageImpExpImg = AbstractUIPlugin.imageDescriptorFromPlugin(Plugin.PLUGIN_ID, "/icons/package_impexp.gif").createImage();

    private final Image requiredBundleImg = AbstractUIPlugin.imageDescriptorFromPlugin(Plugin.PLUGIN_ID, "/icons/brick.png").createImage();
    private final Image requiredBundleSatisfiedImg = AbstractUIPlugin.imageDescriptorFromPlugin(Plugin.PLUGIN_ID, "/icons/bundle_impexp.png").createImage();
    private final Image requiredBundleOptImg = AbstractUIPlugin.imageDescriptorFromPlugin(Plugin.PLUGIN_ID, "/icons/bundle_opt.png").createImage();

    public ImportsExportsTreeLabelProvider() {}

    @Override
    public void dispose() {
        super.dispose();
        pkgFolderImg.dispose();
        bundleFolderImg.dispose();
        classImg.dispose();
        packageImg.dispose();
        packageOptImg.dispose();
        packageImpExpImg.dispose();
        requiredBundleImg.dispose();
        requiredBundleSatisfiedImg.dispose();
        requiredBundleOptImg.dispose();
    }

    @Override
    public void update(ViewerCell cell) {
        if (IMPORTS_PLACEHOLDER.equals(cell.getElement())) {
            if (cell.getColumnIndex() == 0) {
                cell.setImage(pkgFolderImg);
                cell.setText("Import Packages");
            }
        } else if (EXPORTS_PLACEHOLDER.equals(cell.getElement())) {
            if (cell.getColumnIndex() == 0) {
                cell.setImage(pkgFolderImg);
                cell.setText("Export Packages");
            }
        } else if (REQUIRED_PLACEHOLDER.equals(cell.getElement())) {
            if (cell.getColumnIndex() == 0) {
                cell.setImage(bundleFolderImg);
                cell.setText("Required Bundles");
            }
        } else if (cell.getElement() instanceof ImportUsedByPackage) {
            if (cell.getColumnIndex() == 0) {
                StyledString styledString = new StyledString("Used By: ", UIConstants.ITALIC_QUALIFIER_STYLER);
                styledString.append(((ImportUsedByPackage) cell.getElement()).usedByName);
                cell.setText(styledString.getString());
                cell.setStyleRanges(styledString.getStyleRanges());
            }
        } else if (cell.getElement() instanceof ImportUsedByClass) {
            if (cell.getColumnIndex() == 0) {
                ImportUsedByClass importUsedBy = (ImportUsedByClass) cell.getElement();
                String fqn = importUsedBy.clazz.getFQN();
                String className = fqn.substring(fqn.lastIndexOf('.') + 1);
                cell.setText(className);
                cell.setImage(classImg);
            }
        } else if (cell.getElement() instanceof ExportUsesPackage) {
            if (cell.getColumnIndex() == 0) {
                StyledString styledString = new StyledString("Uses: ", UIConstants.ITALIC_QUALIFIER_STYLER);
                styledString.append(((ExportUsesPackage) cell.getElement()).name);
                cell.setText(styledString.getString());
                cell.setStyleRanges(styledString.getStyleRanges());
            }
        } else if (cell.getElement() instanceof RequiredBundle) {
            RequiredBundle rb = (RequiredBundle) cell.getElement();
            switch (cell.getColumnIndex()) {
            case 0 :
                StyledString label;
                if (rb.isSatisfied())
                    label = new StyledString(rb.getName(), StyledString.QUALIFIER_STYLER);
                else
                    label = new StyledString(rb.getName());

                String version = rb.getAttribs().get(Constants.BUNDLE_VERSION_ATTRIBUTE);
                if (version != null)
                    label.append(" " + version, StyledString.COUNTER_STYLER);

                String resolution = rb.getAttribs().get(Constants.RESOLUTION_DIRECTIVE);
                boolean optional = org.osgi.framework.Constants.RESOLUTION_OPTIONAL.equals(resolution);
                if (resolution != null)
                    label.append(" <" + resolution + ">", UIConstants.ITALIC_QUALIFIER_STYLER);

                cell.setText(label.getString());
                cell.setStyleRanges(label.getStyleRanges());

                if (optional)
                    cell.setImage(requiredBundleOptImg);
                else if (rb.isSatisfied())
                    cell.setImage(requiredBundleSatisfiedImg);
                else
                    cell.setImage(requiredBundleImg);
                break;
            case 1 :
                cell.setText(formatAttribs(rb.getAttribs()));
                break;
            default :
                break;
            }
        } else if (cell.getElement() instanceof ImportPackage || cell.getElement() instanceof ExportPackage) {
            HeaderClause entry = (HeaderClause) cell.getElement();
            switch (cell.getColumnIndex()) {
            case 0 :
                boolean selfImport = false;
                if (entry instanceof ImportPackage) {
                    selfImport = ((ImportPackage) entry).isSelfImport();
                }

                StyledString styledString;
                if (selfImport) {
                    styledString = new StyledString(entry.getName(), StyledString.QUALIFIER_STYLER);
                } else {
                    styledString = new StyledString(entry.getName());
                }

                String version = entry.getAttribs().get(Constants.VERSION_ATTRIBUTE);
                if (version != null)
                    styledString.append(" " + version, StyledString.COUNTER_STYLER);

                String resolution = entry.getAttribs().get(Constants.RESOLUTION_DIRECTIVE);
                boolean optional = org.osgi.framework.Constants.RESOLUTION_OPTIONAL.equals(resolution);
                if (resolution != null) {
                    styledString.append(" <" + resolution + ">", UIConstants.ITALIC_QUALIFIER_STYLER);
                }

                cell.setText(styledString.getString());
                cell.setStyleRanges(styledString.getStyleRanges());
                if (optional) {
                    cell.setImage(packageOptImg);
                } else if (selfImport) {
                    cell.setImage(packageImpExpImg);
                } else {
                    cell.setImage(packageImg);
                }
                break;
            case 1 :
                cell.setText(formatAttribs(entry.getAttribs()));
                break;
            default :
                break;
            }
        }
    }

    static String formatAttribs(Map<String,String> attribs) {
        StringBuilder builder = new StringBuilder();
        boolean first = true;
        for (Entry<String,String> attribEntry : attribs.entrySet()) {
            if (!first)
                builder.append("; ");
            String key = attribEntry.getKey();
            if (!Constants.VERSION_ATTRIBUTE.equals(key) && !Constants.RESOLUTION_DIRECTIVE.equals(key) && !Constants.USES_DIRECTIVE.equals(key) && !Constants.BUNDLE_VERSION_ATTRIBUTE.equals(key)) {
                builder.append(key).append('=').append(attribEntry.getValue());
                first = false;
            }
        }
        return builder.toString();
    }
}
