import java.util.jar.*;

println 'Tests for bnd-maven-plugin'
println " basedir: ${basedir}"
println " localRepositoryPath: ${localRepositoryPath}"
println " mavenVersion: ${mavenVersion}"

// Check the bundles exist!
File api_bundle = new File(basedir, 'test-api-bundle/target/test-api-bundle-0.0.1.jar')
assert api_bundle.isFile()
File impl_bundle = new File(basedir, 'test-impl-bundle/target/test-impl-bundle-0.0.1.jar')
assert impl_bundle.isFile()
File wrapper_bundle = new File(basedir, 'test-wrapper-bundle/target/test-wrapper-bundle-0.0.1.jar')
assert wrapper_bundle.isFile()

// Load manifests
JarFile api_jar = new JarFile(api_bundle)
Attributes api_manifest = api_jar.getManifest().getMainAttributes()
JarFile impl_jar = new JarFile(impl_bundle)
Attributes impl_manifest = impl_jar.getManifest().getMainAttributes()
JarFile wrapper_jar = new JarFile(wrapper_bundle)
Attributes wrapper_manifest = wrapper_jar.getManifest().getMainAttributes()

// Basic manifest check
assert 'test-api-bundle' == api_manifest.getValue('Bundle-SymbolicName')
assert 'test-impl-bundle' == impl_manifest.getValue('Bundle-SymbolicName')
assert 'test-wrapper-bundle' == wrapper_manifest.getValue('Bundle-SymbolicName')
assert '0.0.1' == api_manifest.getValue('Bundle-Version')
assert '0.0.1' == impl_manifest.getValue('Bundle-Version')
assert '0.0.1' == wrapper_manifest.getValue('Bundle-Version')

// Check inheritance of properties in bnd.bnd from the parent project
assert 'it worked' == api_manifest.getValue('X-ParentProjectProperty')
assert 'it worked' == impl_manifest.getValue('X-ParentProjectProperty')
assert 'overridden' == wrapper_manifest.getValue('X-ParentProjectProperty')

// Check POM properties
assert new File(basedir, 'test-impl-bundle/target/classes').absolutePath == impl_manifest.getValue('Project-Build-OutputDirectory')
assert 'UTF-8' == impl_manifest.getValue('Project-Build-SourceEncoding')
assert 'biz.aQute.bnd-test:test-impl-bundle' == impl_manifest.getValue('Project-GroupId-ArtifactId')
assert '${project.nosuchproperty}' == impl_manifest.getValue('Project-NoSuchProperty')
assert localRepositoryPath.absolutePath == impl_manifest.getValue('Settings-LocalRepository')
assert 'false' == impl_manifest.getValue('Settings-InteractiveMode')
assert 'value' == impl_manifest.getValue('SomeVar')
assert 'parentValue' == impl_manifest.getValue('SomeParentVar')

// Check contents
assert null != api_jar.getEntry('org/example/api/')
assert null != api_jar.getEntry('org/example/types/')
assert null != api_jar.getEntry('OSGI-OPT/src/')
assert null != impl_jar.getEntry('org/example/impl/')
assert null != impl_jar.getEntry('OSGI-INF/org.example.impl.ExampleComponent.xml')
assert null != impl_jar.getEntry('OSGI-INF/metatype/org.example.impl.Config.xml')
assert null != wrapper_jar.getEntry('org/example/api/')
assert null != wrapper_jar.getEntry('org/example/types/')
