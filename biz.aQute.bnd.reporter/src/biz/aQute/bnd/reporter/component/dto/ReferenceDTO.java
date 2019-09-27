package biz.aQute.bnd.reporter.component.dto;

import org.osgi.dto.DTO;

public class ReferenceDTO extends DTO {

	/**
	 * The name of the reference.
	 * <p>
	 * This is declared in the {@code name} attribute of the {@code reference}
	 * element. This must be the default name if the component description does
	 * not declare a name for the reference.
	 */
	public String	name;

	/**
	 * The service interface of the reference.
	 * <p>
	 * This is declared in the {@code interface} attribute of the
	 * {@code reference} element.
	 */
	public String	interfaceName;

	/**
	 * The cardinality of the reference.
	 * <p>
	 * This is declared in the {@code cardinality} attribute of the
	 * {@code reference} element. This must be the default cardinality if the
	 * component description does not declare a cardinality for the reference.
	 */
	public String	cardinality;

	/**
	 * The policy of the reference.
	 * <p>
	 * This is declared in the {@code policy} attribute of the {@code reference}
	 * element. This must be the "static" policy if the component description
	 * does not declare a policy for the reference.
	 */
	public String	policy			= "static";

	/**
	 * The policy option of the reference.
	 * <p>
	 * This is declared in the {@code policy-option} attribute of the
	 * {@code reference} element. This must be the "reluctant" policy option if
	 * the component description does not declare a policy option for the
	 * reference.
	 */
	public String	policyOption	= "reluctant";

	/**
	 * The target of the reference.
	 * <p>
	 * This is declared in the {@code target} attribute of the {@code reference}
	 * element. This must be {@code null} if the component description does not
	 * declare a target for the reference.
	 */
	public String	target;

	/**
	 * The name of the bind method of the reference.
	 * <p>
	 * This is declared in the {@code bind} attribute of the {@code reference}
	 * element. This must be {@code null} if the component description does not
	 * declare a bind method for the reference.
	 */
	public String	bind;

	/**
	 * The name of the unbind method of the reference.
	 * <p>
	 * This is declared in the {@code unbind} attribute of the {@code reference}
	 * element. This must be {@code null} if the component description does not
	 * declare an unbind method for the reference.
	 */
	public String	unbind;

	/**
	 * The name of the updated method of the reference.
	 * <p>
	 * This is declared in the {@code updated} attribute of the
	 * {@code reference} element. This must be {@code null} if the component
	 * description does not declare an updated method for the reference.
	 */
	public String	updated;

	/**
	 * The name of the field of the reference.
	 * <p>
	 * This is declared in the {@code field} attribute of the {@code reference}
	 * element. This must be {@code null} if the component description does not
	 * declare a field for the reference.
	 */
	public String	field;

	/**
	 * The field option of the reference.
	 * <p>
	 * This is declared in the {@code field-option} attribute of the
	 * {@code reference} element. This must be {@code null} if the component
	 * description does not declare a field for the reference.
	 */
	public String	fieldOption;

	/**
	 * The scope of the reference.
	 * <p>
	 * This is declared in the {@code scope} attribute of the {@code reference}
	 * element. This must be the "bundle" scope if the component description
	 * does not declare a scope for the reference.
	 */
	public String	scope			= "bundle";
}
