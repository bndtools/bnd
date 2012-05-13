package bndtools.model.obr;

import java.util.Collection;

import org.apache.felix.bundlerepository.Resource;
import org.eclipse.jface.viewers.StyledString;
import org.eclipse.jface.viewers.ViewerCell;
import org.eclipse.swt.graphics.Image;

import bndtools.UIConstants;

public class ResolutionFailureTreeLabelProvider extends RequirementLabelProvider {

    private static final String LABEL_INITIAL = "INITIAL";
    private Image unmatchedImg = null;

    @Override
    public void update(ViewerCell cell) {
        StyledString label;
        Image image;

        Object element = cell.getElement();
        if (element instanceof Resource) {
            Resource resource = (Resource) element;
            label = getLabel(resource);
            String uri = resource.getURI();
            if (uri != null) label.append(" " + uri, StyledString.QUALIFIER_STYLER);
            image = getBundleImage();
        } else if (element instanceof PotentialMatch) {
            PotentialMatch match = (PotentialMatch) element;
            
            label = getLabel(match.getRequirement());
            Collection<Resource> matchedResources = match.getResources();
            if (matchedResources.isEmpty()) {
                label.append(" UNMATCHED", UIConstants.BOLD_STYLER);
            } else if (matchedResources.size() == 1) {
                label.append(" 1 potential match", StyledString.QUALIFIER_STYLER);
            } else {
                label.append(String.format(" %d potential matches", matchedResources.size()), StyledString.QUALIFIER_STYLER);
            }
            image = getIcon(match.getRequirement());
        } else {
            image = null;
            label = new StyledString("ERROR", UIConstants.BOLD_STYLER);
        }
        
        /*
        if(ResolutionFailureTreeContentProvider.UNMATCHED == reason) {
            if (unmatchedImg == null) unmatchedImg = AbstractUIPlugin.imageDescriptorFromPlugin(Plugin.PLUGIN_ID, "icons/cross.png").createImage();
            image = unmatchedImg;
            label = new StyledString("UNMATCHED", UIConstants.BOLD_STYLER);
        } else {
            image = getIcon(reason.getRequirement());
            label = new StyledString(getLabel(reason.getResource()), StyledString.COUNTER_STYLER);
            label.append(" ---> ");
            label.append(getLabel(reason.getRequirement()));
        }
        */

        cell.setImage(image);
        cell.setText(label.getString());
        cell.setStyleRanges(label.getStyleRanges());
    }

    private StyledString getLabel(Resource resource) {
        StyledString label;
        if (resource == null || resource.getId() == null) {
            label = new StyledString(LABEL_INITIAL, UIConstants.BOLD_STYLER);
        } else {
            label = new StyledString(resource.getSymbolicName(), UIConstants.BOLD_STYLER);
            if (resource.getVersion() != null)
                label.append(" " + resource.getVersion().toString(), StyledString.COUNTER_STYLER);
        }
        return label;
    }
    
    @Override
    public void dispose() {
        super.dispose();
        if (unmatchedImg != null && !unmatchedImg.isDisposed()) unmatchedImg.dispose();
    }
}
