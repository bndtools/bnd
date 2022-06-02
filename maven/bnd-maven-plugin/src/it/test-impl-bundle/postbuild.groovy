import java.util.jar.Attributes
import java.util.jar.JarFile;

// Check the bundles exist!
File impl_bundle = new File(basedir, 'target/test-impl-bundle-0.0.1-SNAPSHOT.jar')
assert impl_bundle.isFile()

// Load manifests
JarFile impl_jar = new JarFile(impl_bundle)
Attributes impl_manifest = impl_jar.getManifest().getMainAttributes()

// Basic manifest check
assert impl_manifest.getValue('Bundle-SymbolicName') == 'test-impl-bundle'
assert impl_manifest.getValue('Bundle-Name') == 'Test Impl Bundle'
assert impl_manifest.getValue('Bundle-Version') == '0.0.1.SNAPSHOT'
assert impl_manifest.getValue('Bnd-LastModified') == null

// Check inheritance of properties in bnd.bnd from the parent project
assert impl_manifest.getValue('X-ParentProjectProperty') == 'it worked'

// Check -include of bnd files
assert impl_manifest.getValue('X-IncludedParentProjectProperty') == 'Included via -include in parent bnd.bnd file'
assert impl_manifest.getValue('X-IncludedProjectProperty') == 'Included via -include in project bnd.bnd file'

// Check POM properties
assert impl_manifest.getValue('Project-Build-OutputDirectory') == new File(basedir, 'target/classes').absolutePath
assert impl_manifest.getValue('Project-Build-SourceEncoding') == 'UTF-8'
assert impl_manifest.getValue('Project-GroupId-ArtifactId') == 'biz.aQute.bnd-test:test-impl-bundle'
assert impl_manifest.getValue('Project-NoSuchProperty') == '${project.nosuchproperty}'
assert impl_manifest.getValue('Settings-LocalRepository') == localRepositoryPath.absolutePath
assert impl_manifest.getValue('Settings-InteractiveMode') == 'false'
assert impl_manifest.getValue('SomeVar') == 'value'
assert impl_manifest.getValue('SomeParentVar') == 'parentValue'

// Check bnd properties
assert impl_manifest.getValue('Project-Name') == 'test-impl-bundle'
assert impl_manifest.getValue('Project-Dir') == basedir.absolutePath.replace(File.separatorChar, '/' as char)
assert impl_manifest.getValue('Project-Output') == new File(basedir, 'target').absolutePath
assert impl_manifest.getValue('Project-Buildpath')
assert impl_manifest.getValue('Project-Sourcepath')
assert impl_manifest.getValue('Parent-Here') == new File(basedir, "../process-parent").canonicalPath.replace(File.separatorChar, '/' as char)
assert impl_manifest.getValue('Project-License') == 'Apache License, Version 2.0'

// Check contents
assert impl_jar.getEntry('org/example/impl/') != null
assert impl_jar.getEntry('OSGI-INF/org.example.impl.ExampleComponent.xml') != null
assert impl_jar.getEntry('OSGI-INF/metatype/org.example.impl.Config.xml') != null
