import java.util.jar.*;

println "TODO: Need to write some test code for the generated bundles!"
println "basedir ${basedir}"
println "localRepositoryPath ${localRepositoryPath}"
println "mavenVersion ${mavenVersion}"

// Check the bundles exist!
File api_bundle = new File("${basedir}/test-api-bundle/target/test-api-bundle-0.0.1.jar");
assert api_bundle.isFile();
File impl_bundle = new File("${basedir}/test-impl-bundle/target/test-impl-bundle-0.0.1.jar");
assert impl_bundle.isFile();
File wrapper_bundle = new File("${basedir}/test-wrapper-bundle/target/test-wrapper-bundle-0.0.1.jar");
assert wrapper_bundle.isFile();

// Load manifests
Manifest api_manifest = new JarFile(api_bundle).getManifest();
Manifest impl_manifest = new JarFile(impl_bundle).getManifest();
Manifest wrapper_manifest = new JarFile(wrapper_bundle).getManifest();

// Basic manifest check
assert "test-api-bundle" == api_manifest.getMainAttributes().getValue("Bundle-SymbolicName");

// Check inheritance of properties in bnd.bnd from the parent project
assert "it worked" == api_manifest.getMainAttributes().getValue("X-ParentProjectProperty");
assert "it worked" == impl_manifest.getMainAttributes().getValue("X-ParentProjectProperty");
assert "overridden" == wrapper_manifest.getMainAttributes().getValue("X-ParentProjectProperty");