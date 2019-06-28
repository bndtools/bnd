package org.bndtools.core.ui.resource;

public class ResolutionFailureFlatLabelProvider extends RequirementLabelProvider {

	// private static final String LABEL_INITIAL = "INITIAL";

	/*
	 * TODO
	 * @Override public void update(ViewerCell cell) { Reason reason = (Reason)
	 * cell.getElement(); Resource resource = reason.getResource(); Requirement
	 * requirement = reason.getRequirement();
	 * cell.setImage(getIcon(requirement)); StyledString label =
	 * getLabel(resource); label.append(" requires ",
	 * StyledString.QUALIFIER_STYLER); if (requirement.isOptional())
	 * label.append("optional ", StyledString.QUALIFIER_STYLER);
	 * label.append(getLabel(requirement)); cell.setText(label.getString());
	 * cell.setStyleRanges(label.getStyleRanges()); }
	 */

	/*
	 * TODO private static StyledString getLabel(Resource resource) {
	 * StyledString label; if (resource == null || resource.getId() == null) {
	 * label = new StyledString(LABEL_INITIAL, BoldStyler.INSTANCE_DEFAULT); }
	 * else { label = new StyledString(resource.getSymbolicName(),
	 * BoldStyler.INSTANCE_DEFAULT); if (resource.getVersion() != null)
	 * label.append(" " + resource.getVersion().toString(),
	 * StyledString.COUNTER_STYLER); } return label; }
	 */

}
