package biz.aQute.bnd.reporter.component.dto;

import org.osgi.dto.DTO;

/**
 * A representation of an {@code AttributeDefinition} option.
 */
public class OptionDTO extends DTO {

	/**
	 * The label of the option.
	 * <p>
	 * Must not be {@code null}.
	 * </p>
	 */
	public String	label;

	/**
	 * The value of the option.
	 * <p>
	 * Must not be {@code null}.
	 * </p>
	 */
	public String	value;
}
