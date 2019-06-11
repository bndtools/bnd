package biz.aQute.bnd.reporter.component.dto;

import java.util.LinkedList;
import java.util.List;

import org.osgi.dto.DTO;

/**
 * A representation of an {@code ObjectClassDefinition}
 */
public class ObjectClassDefinitionDTO extends DTO {

	/**
	 * The name of the OCD.
	 * <p>
	 * Must not be {@code null}.
	 * <p>
	 */
	public String						name;

	/**
	 * The description of the OCD.
	 * <p>
	 * If it is not specified this field must be {@code null}.
	 * <p>
	 */
	public String						description;

	/**
	 * The id of the OCD.
	 * <p>
	 * Must not be {@code null}.
	 * </p>
	 */
	public String						id;

	/**
	 * A list of pids.
	 * <p>
	 * If it is not specified this field must empty.
	 * </p>
	 */
	public List<String>					pids		= new LinkedList<>();

	/**
	 * A list of factory pids.
	 * <p>
	 * If it is not specified this field must empty.
	 * </p>
	 */
	public List<String>					factoryPids	= new LinkedList<>();

	/**
	 * A list of the icons of this OCD.
	 * <p>
	 * If it is not specified this field must be empty.
	 * <p>
	 */
	public List<IconDTO>				icons		= new LinkedList<>();

	/**
	 * A list of attributes.
	 * <p>
	 * If it is not specified this field must empty.
	 * </p>
	 */
	public List<AttributeDefinitionDTO>	attributes	= new LinkedList<>();
}
