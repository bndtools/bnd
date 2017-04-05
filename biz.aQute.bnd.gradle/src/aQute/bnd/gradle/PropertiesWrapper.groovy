/**
 * PropertiesWrapper for Gradle.
 *
 */

package aQute.bnd.gradle

class PropertiesWrapper extends Properties {
  PropertiesWrapper() {}

  @Override
  public String getProperty(String key) {
    final int i = key.indexOf('.')
    final String name = (i > 0) ? key.substring(0, i) : key
    Object value = get(name)
    if ((value != null) && (i > 0)) {
      try {
        value = key.substring(i + 1).split(/\./).inject(value) { obj, prop ->
          obj?."${prop}"
        }
      } catch (MissingPropertyException mpe) {
        value = null
      }
    }
    if (value == null) {
      return null
    }
    return value.toString()
  }
}
