package test.annotationheaders;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertTrue;

import org.junit.jupiter.api.Test;

import aQute.bnd.header.Parameters;
import aQute.bnd.osgi.Builder;
import aQute.bnd.osgi.Domain;
import aQute.lib.io.IO;

public class ServiceProviderFileTest {
	@SuppressWarnings("unchecked")
	@Test
	public void testServiceProviderOnPackage() throws Exception {
		try (Builder b = new Builder();) {
			b.addClasspath(IO.getFile("bin_test"));
			b.setProperty("-includeresource", """
					META-INF/services/com.example.service.Type;\
						literal='\
						#import aQute.bnd.annotation.spi.ServiceProvider;\n\
						#@ServiceProvider(attribute:List<String>="a=1,b=2", effective:=foobar)\n\
						java.lang.String'
				""");
			b.build();
			assertTrue(b.check());
			Domain manifest = Domain.domain(b.getJar()
				.getManifest());
			Parameters provideCapability = manifest.getProvideCapability();

			assertThat(provideCapability.get("osgi.service")).isNotNull();
			assertThat(provideCapability.get("osgi.service")).containsEntry("objectClass", "com.example.service.Type")
				.containsEntry("a", "1")
				.containsEntry("b", "2")
				.containsEntry("effective:", "active");

			assertThat(provideCapability.get("osgi.serviceloader")).isNotNull();
			assertThat(provideCapability.get("osgi.serviceloader")
				.get("osgi.serviceloader")).isEqualTo("com.example.service.Type");
			assertThat(provideCapability.get("osgi.serviceloader"))
				.containsEntry("osgi.serviceloader", "com.example.service.Type")
				.containsEntry("a", "1")
				.containsEntry("b", "2")
				.containsEntry("register:", "java.lang.String");

			Parameters requireCapability = manifest.getRequireCapability();

			System.out.println(provideCapability.toString()
				.replace(',', '\n'));
			System.out.println(requireCapability.toString()
				.replace(',', '\n'));
		}
	}

	@Test
	public void testBothMetaInfoAndAnnotations() throws Exception {
		try (Builder b = new Builder();) {
			b.addClasspath(IO.getFile("bin_test"));
			b.setPrivatePackage("test.annotationheaders.spi.providerF");
			b.setProperty("-includeresource", """
					META-INF/services/com.example.service.Type;\
						literal='\
					    	#import aQute.bnd.annotation.spi.ServiceProvider;\n\
						    #@ServiceProvider(attribute:List<String>="a=1,b=2")\n\
						    java.lang.String'
				""");
			b.build();
			assertTrue(b.check());
			Domain manifest = Domain.domain(b.getJar()
				.getManifest());
			Parameters provideCapability = manifest.getProvideCapability();
			Parameters requireCapability = manifest.getRequireCapability();
			assertThat(provideCapability.size()).isEqualTo(4);
			assertThat(requireCapability.size()).isEqualTo(2);
		}
	}

	@Test
	public void testBothMetaInfoAndAnnotationsNoParentheses() throws Exception {
		try (Builder b = new Builder();) {
			b.addClasspath(IO.getFile("bin_test"));
			b.setPrivatePackage("test.annotationheaders.spi.providerF");
			b.setProperty("-includeresource", """
					META-INF/services/com.example.service.Type;\
						literal='\
					    	#import aQute.bnd.annotation.spi.ServiceProvider;\n\
						    #@ServiceProvider\n\
						    java.lang.String'
				""");
			b.build();
			assertTrue(b.check());
			Domain manifest = Domain.domain(b.getJar()
				.getManifest());
			Parameters provideCapability = manifest.getProvideCapability();
			Parameters requireCapability = manifest.getRequireCapability();
			assertThat(provideCapability.size()).isEqualTo(4);
			assertThat(requireCapability.size()).isEqualTo(2);
		}
	}

	@SuppressWarnings("unchecked")
	@Test
	public void testAutoGenerateServiceProviderAnnotation() throws Exception {
		try (Builder b = new Builder();) {
			b.addClasspath(IO.getFile("bin_test"));
			b.setProperty("-includeresource", """
					META-INF/services/com.example.service.Type;\
						literal='\
						java.lang.String'
				""");
			b.setProperty("-metainf-services", "auto");
			b.build();
			assertTrue(b.check());
			Domain manifest = Domain.domain(b.getJar()
				.getManifest());
			Parameters provideCapability = manifest.getProvideCapability();

			assertThat(provideCapability.get("osgi.service")).isNotNull();
			assertThat(provideCapability.get("osgi.service")).containsEntry("objectClass", "com.example.service.Type");

			assertThat(provideCapability.get("osgi.serviceloader")).isNotNull();
			assertThat(provideCapability.get("osgi.serviceloader")
				.get("osgi.serviceloader")).isEqualTo("com.example.service.Type");
			assertThat(provideCapability.get("osgi.serviceloader"))
				.containsEntry("osgi.serviceloader", "com.example.service.Type")
				.containsEntry("register:", "java.lang.String");

			Parameters requireCapability = manifest.getRequireCapability();

			System.out.println(provideCapability.toString()
				.replace(',', '\n'));
			System.out.println(requireCapability.toString()
				.replace(',', '\n'));
		}
	}

	@SuppressWarnings("unchecked")
	@Test
	public void testInvalidServiceImplementationNamesShouldBeIgnored() throws Exception {
		try (Builder b = new Builder();) {
			b.addClasspath(IO.getFile("bin_test"));
			b.setProperty("-includeresource", """
					META-INF/services/com.example.service.Type;\
						literal='\
						key=value'
				""");
			b.setProperty("-metainf-services", "auto");
			b.build();
			assertTrue(b.check());
			Domain manifest = Domain.domain(b.getJar()
				.getManifest());
			Parameters provideCapability = manifest.getProvideCapability();

			assertThat(provideCapability.get("osgi.service")).isNull();

			Parameters requireCapability = manifest.getRequireCapability();

			System.out.println(provideCapability.toString()
				.replace(',', '\n'));
			System.out.println(requireCapability.toString()
				.replace(',', '\n'));
		}
	}


}
