package aQute.bnd.metadata.dto;

import java.util.List;
import java.util.Map;

/**
 * A representation of a bundle.
 */
public class ManifestHeadersDTO extends LocalizableManifestHeadersDTO {

	/**
	 * The lazy activation of the bundle.
	 * <p>
	 * This is declared in the {@code Bundle-ActivationPolicy} header, if the bundle
	 * does not declare a lazy activation this field must be {@code null}.
	 * </p>
	 */
	public LazyActivationDTO							lazyActivation;

	/**
	 * The fully qualified name of the activator class of the bundle.
	 * <p>
	 * This is declared in the {@code Bundle-Activator} header, if the bundle does
	 * not declare an activator this field must be {@code null}.
	 * </p>
	 */
	public String										activator;

	/**
	 * A list of the class paths of the bundle.
	 * <p>
	 * This is declared in the {@code Bundle-ClassPath} header, if the bundle does
	 * not declare class paths this field must be set to the default value.
	 * </p>
	 */
	public List<String>									classPaths;

	/**
	 * A map of localized bundle headers whose key is the local.
	 * <p>
	 * The format of the local is defined in {@code java.util.Locale}, eg:
	 * en_GB_welsh. If it is not specified this field must be empty.
	 * </p>
	 */
	public Map<String,LocalizableManifestHeadersDTO>	localizations;

	/**
	 * The manifest version of the bundle.
	 * <p>
	 * This is declared in the {@code Bundle-ManifestVersion} header, if the bundle
	 * does not declare a manifest version this field must be set to the default
	 * value.
	 * </p>
	 */
	public Integer										manifestVersion;

	/**
	 * The native code declared by the bundle.
	 * <p>
	 * This is declared in the {@code Bundle-NativeCode} header, if the bundle does
	 * not declare native code this field must be {@code null}.
	 * </p>
	 */
	public NativeCodeDTO								nativeCode;

	/**
	 * The required execution environment.
	 * <p>
	 * This is declared in the {@code Bundle-NativeCode} header, if the bundle does
	 * not declare required execution environment this field must be empty.
	 * </p>
	 */
	public List<String>									requiredExecutionEnvironments;

	/**
	 * The symbolic name of the bundle.
	 * <p>
	 * This is declared in the {@code Bundle-SymbolicName} header, must not be
	 * {@code null}.
	 * </p>
	 */
	public BundleSymbolicNameDTO						symbolicName;

	/**
	 * The version of the bundle.
	 * <p>
	 * This is declared in the {@code Bundle-Version} header, if the bundle does not
	 * declare a version this field must be set to the default value.
	 * </p>
	 */
	public VersionDTO									version;

	/**
	 * A list of the dynamic imported packages of the bundle.
	 * <p>
	 * This is declared in the {@code DynamicImport-Package} header, if the bundle
	 * does not declare dynamic import packages this field must be empty.
	 * </p>
	 */
	public List<DynamicImportPackageDTO>				dynamicImportPackages;

	/**
	 * A list of the exported packages of the bundle.
	 * <p>
	 * This is declared in the {@code Export-Package} header, if the bundle does not
	 * declare export packages this field must be empty.
	 * </p>
	 */
	public List<ExportPackageDTO>						exportPackages;

	/**
	 * The fragment host of the bundle.
	 * <p>
	 * This is declared in the {@code Fragment-Host} header, if the bundle does not
	 * declare a fragment host this field must be {@code null}.
	 * </p>
	 */
	public FragmentHostDTO								fragmentHost;

	/**
	 * A list of the imported packages of the bundle.
	 * <p>
	 * This is declared in the {@code Import-Package} header, if the bundle does not
	 * declare import packages this field must be empty.
	 * </p>
	 */
	public List<ImportPackageDTO>						importPackages;

	/**
	 * A list of the provided capabilities of the bundle.
	 * <p>
	 * This is declared in the {@code Provide-Capability } header, if the bundle
	 * does not declare provided capabilities this field must be empty.
	 * </p>
	 */
	public List<ProvideCapabilityDTO>					provideCapabilities;

	/**
	 * A list of the required bundles of the bundle.
	 * <p>
	 * This is declared in the {@code Require-Bundle } header, if the bundle does
	 * not declare required bundles this field must be empty.
	 * </p>
	 */
	public List<RequireBundleDTO>						requireBundles;

	/**
	 * A list of the required capabilities of the bundle.
	 * <p>
	 * This is declared in the {@code Require-Capability } header, if the bundle
	 * does not declare required capabilities this field must be empty.
	 * </p>
	 */
	public List<RequireCapabilityDTO>					requireCapabilities;
}
