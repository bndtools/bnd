package aQute.bnd.readme;

/**
 * Configuration for {@link ReadmeUpdater}.
 */
final public class ReadmeConfiguration {

	private boolean	_showTitle;
	private boolean	_showDescription;
	private boolean	_showComponents;
	private boolean	_showLicense;
	private boolean	_showCopyright;
	private boolean	_showContacts;
	private boolean	_showContents;
	private boolean	_showVendor;
	private boolean	_showVersion;
	private int		_componentsIndex;
	private String	_componentsMessage;
	private String	_contactsMessage;

	private ReadmeConfiguration() {

	}

	/**
	 * @return true if the title must be included in the readme file
	 */
	public boolean showTitle() {

		return _showTitle;
	}

	/**
	 * @return true if the description must be included in the readme file
	 */
	public boolean showDescription() {

		return _showDescription;
	}

	/**
	 * @return true if the components description must be included in the readme
	 *         file
	 */
	public boolean showComponents() {

		return _showComponents;
	}

	/**
	 * @return true if the license must be included in the readme file
	 */
	public boolean showLicense() {

		return _showLicense;
	}

	/**
	 * @return true if the copyright must be included in the readme file
	 */
	public boolean showCopyright() {

		return _showCopyright;
	}

	/**
	 * @return true if the contacts must be included in the readme file
	 */
	public boolean showContacts() {

		return _showContacts;
	}

	/**
	 * @return true if the content of the workspace must be included in the
	 *         workspace readme file
	 */
	public boolean showContents() {

		return _showContents;
	}

	/**
	 * @return true if the vendor must be included in the readme file
	 */
	public boolean showVendor() {

		return _showVendor;
	}

	/**
	 * @return true if the version must be included in the readme file
	 */
	public boolean showVersion() {

		return _showVersion;
	}

	/**
	 * @return The index at which the components section will be inserted,
	 *         positive value
	 */
	public int componentsIndex() {

		return _componentsIndex;
	}

	/**
	 * @return A custom message to insert after the components description, not
	 *         null
	 */
	public String componentsMessage() {

		return _componentsMessage;
	}

	/**
	 * @return A custom message to insert before the contact list, not null
	 */
	public String contactsMessage() {

		return _contactsMessage;
	}

	public static Builder builder() {

		return new Builder();
	}

	public static class Builder {

		private boolean	_showDefault	= true;
		private Boolean	_showTitle;
		private Boolean	_showDescription;
		private Boolean	_showComponents;
		private Boolean	_showLicense;
		private Boolean	_showCopyright;
		private Boolean	_showContacts;
		private Boolean	_showContents;
		private Boolean	_showVendor;
		private Boolean	_showVersion;
		private int		_componentsIndex	= 1;
		private String	_componentsMessage;
		private String	_contactsMessage;

		private Builder() {

		}

		/**
		 * Set the default value for the show* option.
		 * 
		 * @return this builder
		 */
		public Builder showDefault(boolean value) {

			_showDefault = value;

			return this;
		}

		/**
		 * Set if the title must be included in the readme file.
		 * 
		 * @return this builder
		 */
		public Builder showTitle(boolean value) {

			_showTitle = value;
			 
			 return this;
		}

		/**
		 * Set if the description must be included in the readme file.
		 * 
		 * @return this builder
		 */
		public Builder showDescription(boolean value) {

			_showDescription = value;
			 
			 return this;
		}

		/**
		 * Set if the components description must be included in the readme
		 * file.
		 * 
		 * @return this builder
		 */
		public Builder showComponents(boolean value) {

			_showComponents = value;
			 
			 return this;
		}

		/**
		 * Set if the license must be included in the readme file.
		 * 
		 * @return this builder
		 */
		public Builder showLicense(boolean value) {

			_showLicense = value;
			 
			 return this;
		}

		/**
		 * Set if the copyright must be included in the readme file.
		 * 
		 * @return this builder
		 */
		public Builder showCopyright(boolean value) {

			_showCopyright = value;
			 
			 return this;
		}

		/**
		 * Set if the contacts must be included in the readme file.
		 * 
		 * @return this builder
		 */
		public Builder showContacts(boolean value) {

			_showContacts = value;
			 
			 return this;
		}

		/**
		 * Set if the content of the workspace must be included in the workspace
		 * readme file.
		 * 
		 * @return this builder
		 */
		public Builder showContents(boolean value) {

			_showContents = value;

			return this;
		}

		/**
		 * Set if the vendor must be included in the readme file.
		 * 
		 * @return this builder
		 */
		public Builder showVendor(boolean value) {

			_showVendor = value;
			 
			 return this;
		}

		/**
		 * Set if the version must be included in the readme file.
		 * 
		 * @return this builder
		 */
		public Builder showVersion(boolean value) {

			_showVersion = value;
			 
			 return this;
		}

		/**
		 * Set the index at which the components section will be inserted,
		 * positive value.
		 * 
		 * @return this builder
		 */
		public Builder componentsIndex(int value) {

			_componentsIndex = value;
			 
			 return this;
		}

		/**
		 * Set a custom message to insert after the components description, may
		 * be null, may be empty.
		 * 
		 * @return this builder
		 */
		public Builder componentsMessage(String value) {

			_componentsMessage = value;
			 
			 return this;
		}

		/**
		 * Set a custom message to insert before the contact list, may be null,
		 * may be empty.
		 * 
		 * @return this builder
		 */
		public Builder contactsMessage(String value) {

			_contactsMessage = value;
			 
			 return this;
		}

		public ReadmeConfiguration build() {

			final ReadmeConfiguration configuration = new ReadmeConfiguration();

			configuration._showTitle = (_showTitle != null) ? _showTitle : _showDefault;
			configuration._showDescription = (_showDescription != null) ? _showDescription : _showDefault;
			configuration._showComponents = (_showComponents != null) ? _showComponents : _showDefault;
			configuration._showLicense = (_showLicense != null) ? _showLicense : _showDefault;
			configuration._showCopyright = (_showCopyright != null) ? _showCopyright : _showDefault;
			configuration._showContacts = (_showContacts != null) ? _showContacts : _showDefault;
			configuration._showContents = (_showContents != null) ? _showContents : _showDefault;
			configuration._showVendor = (_showVendor != null) ? _showVendor : _showDefault;
			configuration._showVersion = (_showVersion != null) ? _showVersion : _showDefault;
			configuration._componentsMessage = (_componentsMessage != null) ? _componentsMessage : "";
			configuration._contactsMessage = (_contactsMessage != null) ? _contactsMessage : "";
			configuration._componentsIndex = _componentsIndex;

			if (configuration._componentsIndex < 0) {

				configuration._componentsIndex = 0;
			}

			return configuration;
		}
	}
}
