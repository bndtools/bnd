package aQute.bnd.metadata.dto;

import java.util.List;

/**
 * A representation of a bundle.
 */
public class BundleMetadataDTO {

	/**
	 * The manifest headers of the bundle.
	 * <p>
	 * Must not be {@code null}.
	 * </p>
	 */
	public ManifestHeadersDTO				manifestHeaders;

	/**
	 * A list of declared components.
	 * <p>
	 * If the bundle does not declare components this field must be empty.
	 * </p>
	 */
	public List<ComponentDescriptionDTO>	components;

	/**
	 * A list of declared metatypes.
	 * <p>
	 * If the bundle does not declare metatypes this field must be empty.
	 * </p>
	 */
	public List<ObjectClassDefinitionDTO>	metatypes;
}
