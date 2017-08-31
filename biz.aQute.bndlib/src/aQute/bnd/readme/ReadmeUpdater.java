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
		
		if (option.showTitle() && content.getTitle().isPresent() && option.showDescription()
				&& content.getDescription().isPresent()) {

			readmeOutput.insert(0,
					ReadmeUtil.generateTitleDescription(content.getTitle().get(), content.getDescription().get()));

		} else if (option.showDescription() && content.getDescription().isPresent()) {

			readmeOutput.insert(0, ReadmeUtil.generateDescription(content.getDescription().get()));

		} else if (option.showTitle() && content.getTitle().isPresent()) {

			readmeOutput.insert(0, ReadmeUtil.generateTitle(content.getTitle().get()));
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

		if (option.showLicense() && content.getLicense().isPresent()) {

			readmeOutput.append(ReadmeUtil.generateLicense(content.getLicense().get()));
		}

		if (option.showCopyright() && content.getCopyright().isPresent()) {

			readmeOutput.append(ReadmeUtil.generateCopyright(content.getCopyright().get()));
		}

		if (option.showVendor() && option.showVersion() && content.getVendor().isPresent()
				&& content.getVersion().isPresent()) {

			readmeOutput
					.append(ReadmeUtil.generateVendorVersion(content.getVendor().get(), content.getVersion().get()));

		} else if (option.showVendor() && content.getVendor().isPresent()) {

			readmeOutput.append(ReadmeUtil.generateVendor(content.getVendor().get()));

		} else if (option.showVersion() && content.getVersion().isPresent()) {

			readmeOutput.append(ReadmeUtil.generateVersion(content.getVersion().get()));
		}
		
		return readmeOutput.toString();
	}
}
