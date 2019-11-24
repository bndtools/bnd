package aQute.bnd.signatures;

import static java.util.stream.Collectors.toMap;
import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.junit.Test;

import aQute.bnd.osgi.Analyzer;
import aQute.bnd.osgi.ClassDataCollector;
import aQute.bnd.osgi.Clazz;
import aQute.bnd.osgi.Clazz.FieldDef;
import aQute.bnd.osgi.Clazz.MethodDef;
import aQute.lib.io.IO;

public class ResolverTest {

	@Test
	public void testFieldResolving() throws Exception {
		try (Analyzer a = new Analyzer()) {
			Clazz c = new Clazz(a, "", null);
			List<FieldDef> fields = new ArrayList<>();
			c.parseClassFile(IO.stream(new File("bin_test/aQute/bnd/signatures/TypeUser1.class")),
				new ClassDataCollector() {

					@Override
					public void field(FieldDef defined) {
						fields.add(defined);
					}
				});

			ClassSignature classSig = a.getClassSignature(c.getClassSignature());
			System.out.printf("ClassSignature[%s]: %s\n", c, classSig);
			Map<String, FieldSignature> fieldSigs = fields.stream()
				.collect(toMap(FieldDef::getName,
					defined -> a.getFieldSignature((defined.getSignature() != null) ? defined.getSignature()
						: defined.descriptor())));
			System.out.printf("FieldSignature[%s]: %s\n", c, fieldSigs);

			FieldSignature fieldSig;
			FieldResolver resolver;
			JavaTypeSignature resolved;
			ClassTypeSignature type;

			fieldSig = fieldSigs.get("flr");
			resolver = new FieldResolver(classSig, fieldSig);
			resolved = resolver.resolveField();
			assertThat(resolved).isEqualTo(JavaTypeSignature.of("Lorg/osgi/service/log/LogReaderService;"));

			fieldSig = fieldSigs.get("fsr");
			resolver = new FieldResolver(classSig, fieldSig);
			resolved = resolver.resolveField();
			assertThat(resolved).isEqualTo(JavaTypeSignature.of("Lorg/osgi/framework/ServiceReference<+TLR;>;"));
			type = (ClassTypeSignature) resolved;
			resolved = resolver.resolveType(type.classType.typeArguments[0]);
			assertThat(resolved).isEqualTo(JavaTypeSignature.of("Lorg/osgi/service/log/LogReaderService;"));

			fieldSig = fieldSigs.get("flra1");
			resolver = new FieldResolver(classSig, fieldSig);
			resolved = resolver.resolveField();
			assertThat(resolved).isEqualTo(JavaTypeSignature.of("[Lorg/osgi/service/log/LogReaderService;"));

			fieldSig = fieldSigs.get("fsra1");
			resolver = new FieldResolver(classSig, fieldSig);
			resolved = resolver.resolveField();
			assertThat(resolved).isEqualTo(JavaTypeSignature.of("[Lorg/osgi/framework/ServiceReference<+TLR;>;"));

			fieldSig = fieldSigs.get("flra2");
			resolver = new FieldResolver(classSig, fieldSig);
			resolved = resolver.resolveField();
			assertThat(resolved).isEqualTo(JavaTypeSignature.of("[[Lorg/osgi/service/log/LogReaderService;"));

			fieldSig = fieldSigs.get("fsra2");
			resolver = new FieldResolver(classSig, fieldSig);
			resolved = resolver.resolveField();
			assertThat(resolved).isEqualTo(JavaTypeSignature.of("[[Lorg/osgi/framework/ServiceReference<+TLR;>;"));

			fieldSig = fieldSigs.get("inta1");
			resolver = new FieldResolver(classSig, fieldSig);
			resolved = resolver.resolveField();
			assertThat(resolved).isEqualTo(JavaTypeSignature.of("[I"));

			fieldSig = fieldSigs.get("longa2");
			resolver = new FieldResolver(classSig, fieldSig);
			resolved = resolver.resolveField();
			assertThat(resolved).isEqualTo(JavaTypeSignature.of("[[J"));

			fieldSig = fieldSigs.get("lra1");
			resolver = new FieldResolver(classSig, fieldSig);
			resolved = resolver.resolveField();
			assertThat(resolved).isEqualTo(JavaTypeSignature.of("[Lorg/osgi/service/log/LogReaderService;"));

			fieldSig = fieldSigs.get("lrlist");
			resolver = new FieldResolver(classSig, fieldSig);
			resolved = resolver.resolveField();
			assertThat(resolved).isEqualTo(JavaTypeSignature.of("Ljava/util/List<TLR;>;"));
			type = (ClassTypeSignature) resolved;
			resolved = resolver.resolveType(type.classType.typeArguments[0]);
			assertThat(resolved).isEqualTo(JavaTypeSignature.of("Lorg/osgi/service/log/LogReaderService;"));

			fieldSig = fieldSigs.get("lrcoll");
			resolver = new FieldResolver(classSig, fieldSig);
			resolved = resolver.resolveField();
			assertThat(resolved)
				.isEqualTo(JavaTypeSignature.of("Ljava/util/Collection<+Lorg/osgi/service/log/LogReaderService;>;"));
			type = (ClassTypeSignature) resolved;
			resolved = resolver.resolveType(type.classType.typeArguments[0]);
			assertThat(resolved).isEqualTo(JavaTypeSignature.of("Lorg/osgi/service/log/LogReaderService;"));

			fieldSig = fieldSigs.get("lrcoll2");
			resolver = new FieldResolver(classSig, fieldSig);
			resolved = resolver.resolveField();
			assertThat(resolved)
				.isEqualTo(JavaTypeSignature.of("Ljava/util/Collection<-Lorg/osgi/service/log/LogReaderService;>;"));
			type = (ClassTypeSignature) resolved;
			resolved = resolver.resolveType(type.classType.typeArguments[0]);
			assertThat(resolved).isEqualTo(JavaTypeSignature.of("Lorg/osgi/service/log/LogReaderService;"));

		}
	}

	@Test
	public void testMethodResolving() throws Exception {
		try (Analyzer a = new Analyzer()) {
			Clazz c = new Clazz(a, "", null);
			List<MethodDef> methods = new ArrayList<>();
			c.parseClassFile(IO.stream(new File("bin_test/aQute/bnd/signatures/TypeUser1.class")),
				new ClassDataCollector() {

					@Override
					public void method(MethodDef defined) {
						methods.add(defined);
					}
				});

			ClassSignature classSig = a.getClassSignature(c.getClassSignature());
			System.out.printf("ClassSignature[%s]: %s\n", c, classSig);

			Map<String, MethodSignature> methodSigs = methods.stream()
				.collect(toMap(MethodDef::getName,
					defined -> a.getMethodSignature((defined.getSignature() != null) ? defined.getSignature()
						: defined.descriptor())));
			System.out.printf("MethodSignature[%s]: %s\n", c, methodSigs);

			MethodSignature methodSig;
			MethodResolver resolver;
			JavaTypeSignature resolved;
			ClassTypeSignature type;

			methodSig = methodSigs.get("bindLR");
			resolver = new MethodResolver(classSig, methodSig);
			resolved = resolver.resolveParameter(0);
			assertThat(resolved).isEqualTo(JavaTypeSignature.of("Lorg/osgi/service/log/LogReaderService;"));
			assertThat(resolver.resolveResult()).isEqualTo(VoidDescriptor.V);

			methodSig = methodSigs.get("bindSR");
			resolver = new MethodResolver(classSig, methodSig);
			resolved = resolver.resolveParameter(0);
			assertThat(resolved).isEqualTo(JavaTypeSignature.of("Lorg/osgi/framework/ServiceReference<+TLR;>;"));
			type = (ClassTypeSignature) resolved;
			resolved = resolver.resolveType(type.classType.typeArguments[0]);
			assertThat(resolved).isEqualTo(JavaTypeSignature.of("Lorg/osgi/service/log/LogReaderService;"));
			assertThat(resolver.resolveResult()).isEqualTo(VoidDescriptor.V);

			methodSig = methodSigs.get("bindSO");
			resolver = new MethodResolver(classSig, methodSig);
			resolved = resolver.resolveParameter(0);
			assertThat(resolved)
				.isEqualTo(JavaTypeSignature.of("Lorg/osgi/framework/ServiceObjects<+TLR;>;"));
			type = (ClassTypeSignature) resolved;
			resolved = resolver.resolveType(type.classType.typeArguments[0]);
			assertThat(resolved).isEqualTo(JavaTypeSignature.of("Lorg/osgi/service/log/LogReaderService;"));
			assertThat(resolver.resolveResult())
				.isEqualTo(JavaTypeSignature.of("Ljava/util/Map<Ljava/lang/String;Ljava/lang/Object;>;"));

		}
	}

}
