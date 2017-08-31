package aQute.bnd.readme;

final public class ReadmeUpdater {

	public static String updateReadme(String readmeInput, ReadmeConfiguration option, ReadmeInformation content) {

		if (readmeInput == null) {

			throw new NullPointerException("readmeInput must not be null");
		}

		if (option == null) {

			throw new NullPointerException("option must not be null");
		}

		if (content == null) {

			throw new NullPointerException("content must not be null");
		}

		StringBuffer readmeOutput = ReadmeUtil.removeAllTags(readmeInput);
		
		if (option.showTitle() && content.getTitle() != null && option.showDescription()
				&& content.getDescription() != null) {

			readmeOutput.insert(0,
					ReadmeUtil.generateTitleDescription(content.getTitle(), content.getDescription()));

		} else if (option.showDescription() && content.getDescription() != null) {

			readmeOutput.insert(0, ReadmeUtil.generateDescription(content.getDescription()));

		} else if (option.showTitle() && content.getTitle() != null) {

			readmeOutput.insert(0, ReadmeUtil.generateTitle(content.getTitle()));
		}

		if (option.showContents() && !content.getContents().isEmpty()) {

			readmeOutput.insert(ReadmeUtil.findPosition(readmeOutput, 1),
					ReadmeUtil.generateContents(content.getContents()));
		}

		if (option.showComponents() && !content.getComponents().isEmpty()) {

			readmeOutput.insert(ReadmeUtil.findPosition(readmeOutput, option.componentsIndex()),
					ReadmeUtil.generateComponents(content.getComponents(), option.componentsMessage()));
		}

		if (option.showContacts() && !content.getContacts().isEmpty()) {

			readmeOutput.append(ReadmeUtil.generateContacts(content.getContacts(), option.contactsMessage()));
		}

		if (option.showLicense() && content.getLicense() != null) {

			readmeOutput.append(ReadmeUtil.generateLicense(content.getLicense()));
		}

		if (option.showCopyright() && content.getCopyright() != null) {

			readmeOutput.append(ReadmeUtil.generateCopyright(content.getCopyright()));
		}

		if (option.showVendor() && option.showVersion() && content.getVendor() != null
				&& content.getVersion() != null) {

			readmeOutput
					.append(ReadmeUtil.generateVendorVersion(content.getVendor(), content.getVersion()));

		} else if (option.showVendor() && content.getVendor() != null) {

			readmeOutput.append(ReadmeUtil.generateVendor(content.getVendor()));

		} else if (option.showVersion() && content.getVersion() != null) {

			readmeOutput.append(ReadmeUtil.generateVersion(content.getVersion()));
		}
		
		return readmeOutput.toString();
	}
}
