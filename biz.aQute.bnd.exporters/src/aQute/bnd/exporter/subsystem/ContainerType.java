package aQute.bnd.exporter.subsystem;

import java.io.File;
import java.util.stream.Stream;

import org.osgi.framework.Constants;
import org.osgi.service.subsystem.SubsystemConstants;;

public enum ContainerType {

	BUNDLE("jar", Constants.BUNDLE_LOCALIZATION, Constants.BUNDLE_LOCALIZATION_DEFAULT_BASENAME,
		"OSGI-INF/SUBSYSTEM.MF", Constants.BUNDLE_SYMBOLICNAME, Constants.BUNDLE_VERSION, Constants.BUNDLE_NAME,
		Constants.BUNDLE_DESCRIPTION, Constants.BUNDLE_COPYRIGHT, Constants.BUNDLE_DOCURL, Constants.BUNDLE_VENDOR,
		Constants.BUNDLE_CONTACTADDRESS, Constants.BUNDLE_CATEGORY),

	SUBSYSTEM("eas", SubsystemConstants.SUBSYSTEM_LOCALIZATION,
		SubsystemConstants.SUBSYSTEM_LOCALIZATION_DEFAULT_BASENAME, "OSGI-INF/SUBSYSTEM.MF",
		SubsystemConstants.SUBSYSTEM_SYMBOLICNAME, SubsystemConstants.SUBSYSTEM_VERSION,
		SubsystemConstants.SUBSYSTEM_NAME, SubsystemConstants.SUBSYSTEM_DESCRIPTION,
		SubsystemConstants.SUBSYSTEM_COPYRIGHT, SubsystemConstants.SUBSYSTEM_DOCURL,
		SubsystemConstants.SUBSYSTEM_VENDOR, SubsystemConstants.SUBSYSTEM_CONTACTADDRESS,
		SubsystemConstants.SUBSYSTEM_CATEGORY);

	private final String	localizationDefaultBasenameKey;
	private final String	categoryKey;
	private final String	contactAddressKey;
	private final String	copyrightKey;
	private final String	descriptionKey;
	private final String	docURLKey;
	private final String	extension;
	private final String	localizationKey;
	private final String	manifestUri;
	private final String	nameKey;
	private final String	symbolicNameKey;
	private final String	vendorKey;
	private final String	versionKey;

	ContainerType(String extension, String localizationKey, String localizationDefaultBasenameKey, String manifestUri,
		String symbolicNameKey, String versionKey, String nameKey, String descriptionKey, String copyrightKey,
		String docURLKey, String vendorKey, String contactAddressKey, String categoryKey) {
		this.extension = extension;
		this.manifestUri = manifestUri;
		this.localizationKey = localizationKey;
		this.localizationDefaultBasenameKey = localizationDefaultBasenameKey;
		this.symbolicNameKey = symbolicNameKey;
		this.versionKey = versionKey;
		this.nameKey = nameKey;
		this.descriptionKey = descriptionKey;
		this.copyrightKey = copyrightKey;
		this.docURLKey = docURLKey;
		this.vendorKey = vendorKey;
		this.contactAddressKey = contactAddressKey;
		this.categoryKey = categoryKey;
	}

	public String getLocalizationDefaultBasenameKey() {
		return localizationDefaultBasenameKey;
	}

	public String getCategoryKey() {
		return categoryKey;
	}

	public String getContactAddressKey() {
		return contactAddressKey;
	}

	public String getCopyrightKey() {
		return copyrightKey;
	}

	public String getDescriptionKey() {
		return descriptionKey;
	}

	public String getDocURLKey() {
		return docURLKey;
	}

	public String getExtension() {
		return extension;
	}

	public String getLocalizationKey() {
		return localizationKey;
	}

	public String getManifestURI() {
		return manifestUri;
	}

	public String getNameKey() {
		return nameKey;
	}

	public String getSymbolicNameKey() {
		return symbolicNameKey;
	}

	public String getVendorKey() {
		return vendorKey;
	}

	public String getVersionKey() {
		return versionKey;
	}

	public static ContainerType byFile(File file) {

		if (file == null || !file.isFile()) {
			return null;
		}
		String filename = file.getName()
			.toLowerCase();
		return Stream.of(ContainerType.values())
			.filter(c -> filename.endsWith("." + c.getExtension()))
			.findFirst()
			.orElse(null);
	}
}
