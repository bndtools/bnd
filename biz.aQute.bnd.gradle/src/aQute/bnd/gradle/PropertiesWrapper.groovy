/**
 * PropertiesWrapper for Gradle.
 *
 */

package aQute.bnd.gradle

import static aQute.bnd.gradle.BndUtils.unwrap

class PropertiesWrapper extends Properties {
	protected Properties defaults

	PropertiesWrapper(Properties defaults) {
		super()
		this.defaults = defaults
	}

	PropertiesWrapper() {
		this(null)
	}

	@Override
	public String getProperty(String key) {
		List<String> props = key.split(/\./)
		try {
			Object value = props.drop(1).inject(get(props.first())) { obj, prop ->
				obj?."${prop}"
			}
			value = unwrap(value, true)
			return Objects.nonNull(value) ? value.toString() : defaultValue(key)
		} catch (MissingPropertyException mpe) {
			return defaultValue(key)
		}
	}

	private defaultValue(String key) {
		return Objects.nonNull(defaults) ? defaults.getProperty(key) : null
	}
}
