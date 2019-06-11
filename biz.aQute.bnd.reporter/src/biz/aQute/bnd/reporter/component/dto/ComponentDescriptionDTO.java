package biz.aQute.bnd.reporter.component.dto;

import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.osgi.dto.DTO;

import biz.aQute.bnd.reporter.manifest.dto.TypedAttributeValueDTO;

/**
 * A representation of a declared component description.
 */
public class ComponentDescriptionDTO extends DTO {

	/**
	 * The name of the component.
	 * <p>
	 * This is declared in the {@code name} attribute of the {@code component}
	 * element. This must be the default name if the component description does
	 * not declare a name.
	 */
	public String								name;

	/**
	 * The component factory name.
	 * <p>
	 * This is declared in the {@code factory} attribute of the
	 * {@code component} element. This must be {@code null} if the component
	 * description is not declared as a component factory.
	 */
	public String								factory;

	/**
	 * The service scope.
	 * <p>
	 * This is declared in the {@code scope} attribute of the {@code service}
	 * element. This must be {@code null} if the component description does not
	 * declare any service interfaces.
	 */
	public String								scope;

	/**
	 * The fully qualified name of the implementation class.
	 * <p>
	 * This is declared in the {@code class} attribute of the
	 * {@code implementation} element, Must not be {@code null}.
	 */
	public String								implementationClass;

	/**
	 * The initial enabled state.
	 * <p>
	 * This is declared in the {@code enabled} attribute of the
	 * {@code component} element. If it is not specified this field must be set
	 * to true.
	 * </p>
	 */
	public boolean								defaultEnabled		= true;

	/**
	 * The immediate state.
	 * <p>
	 * This is declared in the {@code immediate} attribute of the
	 * {@code component} element. If it is not specified this field must be set
	 * to the default value.
	 * </p>
	 */
	public boolean								immediate;

	/**
	 * The fully qualified names of the service interfaces.
	 * <p>
	 * These are declared in the {@code interface} attribute of the
	 * {@code provide} elements. This field must be empty if the component
	 * description does not declare any service interfaces.
	 */
	public List<String>							serviceInterfaces	= new LinkedList<>();

	/**
	 * A map of the declared component properties.
	 * <p>
	 * These are declared in the {@code property} and {@code properties}
	 * elements. This field must be empty if the component description does not
	 * declare any properties.
	 */
	public Map<String, TypedAttributeValueDTO>	properties			= new LinkedHashMap<>();

	/**
	 * The referenced services.
	 * <p>
	 * These are declared in the {@code reference} elements. This field must be
	 * empty if the component description does not declare references to any
	 * services.
	 */
	public List<ReferenceDTO>					references			= new LinkedList<>();

	/**
	 * The name of the activate method.
	 * <p>
	 * This is declared in the {@code activate} attribute of the
	 * {@code component} element. This must be {@code null} if the component
	 * description does not declare an activate method name.
	 */
	public String								activate;

	/**
	 * The name of the deactivate method.
	 * <p>
	 * This is declared in the {@code deactivate} attribute of the
	 * {@code component} element. This must be {@code null} if the component
	 * description does not declare a deactivate method name.
	 */
	public String								deactivate;

	/**
	 * The name of the modified method.
	 * <p>
	 * This is declared in the {@code modified} attribute of the
	 * {@code component} element. This must be {@code null} if the component
	 * description does not declare a modified method name.
	 */
	public String								modified;

	/**
	 * The configuration policy.
	 * <p>
	 * This is declared in the {@code configuration-policy} attribute of the
	 * {@code component} element. This must be the "optional" configuration
	 * policy if the component description does not declare a configuration
	 * policy.
	 */
	public String								configurationPolicy	= "optional";

	/**
	 * The configuration pids.
	 * <p>
	 * These are declared in the {@code configuration-pid} attribute of the
	 * {@code component} element. This must contain the default configuration
	 * pid if the component description does not declare a configuration pid.
	 */
	public List<String>							configurationPid	= new LinkedList<>();
}
