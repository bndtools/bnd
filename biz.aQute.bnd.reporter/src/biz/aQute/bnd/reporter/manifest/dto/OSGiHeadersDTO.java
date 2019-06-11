package biz.aQute.bnd.reporter.manifest.dto;

import java.util.List;

import org.osgi.dto.DTO;

public class OSGiHeadersDTO extends DTO {

	public String							bundleName;
	public String							bundleDescription;
	public VersionDTO						bundleVersion;
	public List<String>						bundleCategories;
	public List<IconDTO>					bundleIcons;
	public String							bundleDocURL;
	public String							bundleUpdateLocation;
	public List<LicenseDTO>					bundleLicenses;
	public List<DeveloperDTO>				bundleDevelopers;
	public ScmDTO							bundleSCM;
	public String							bundleCopyright;
	public String							bundleVendor;
	public ContactAddressDTO				bundleContactAddress;
	public BundleSymbolicNameDTO			bundleSymbolicName;
	public List<ImportPackageDTO>			importPackages;
	public List<DynamicImportPackageDTO>	dynamicImportPackages;
	public List<ExportPackageDTO>			exportPackages;
	public List<ProvideCapabilityDTO>		provideCapabilities;
	public List<RequireCapabilityDTO>		requireCapabilities;
	public List<RequireBundleDTO>			requireBundles;
	public List<String>						bundleRequiredExecutionEnvironments;
	public ActivationPolicyDTO				bundleActivationPolicy;
	public FragmentHostDTO					fragmentHost;
	public String							bundleActivator;
	public List<String>						bundleClassPaths;
	public NativeCodeDTO					bundleNativeCode;
	public String							bundleLocalization;
	public Integer							bundleManifestVersion;
}
