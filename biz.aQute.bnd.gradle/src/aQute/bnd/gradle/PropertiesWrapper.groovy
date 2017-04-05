/**
 * PropertiesWrapper for Gradle.
 *
 */

package aQute.bnd.gradle

class PropertiesWrapper extends Properties {
  PropertiesWrapper() {}

  @Override
  public String getProperty(String key) {
    List<String> props = key.split(/\./)
    try {
      Object value = props.drop(1).inject(get(props.first())) { obj, prop ->
        obj?."${prop}"
      }
      return value?.toString()
    } catch (MissingPropertyException mpe) {
      return null
    }
  }
}
