package aQute.bnd.signatures;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.Test;

import aQute.lib.stringrover.StringRover;

public class SignaturesTest {

	@Test
	public void testClassTypeSignature() {
		String signature = "LDefault;";
		ClassTypeSignature sig = JavaTypeSignature.of(signature);
		assertThat(sig.packageSpecifier).isEmpty();
		assertThat(sig.classType.identifier).isEqualTo("Default");
		assertThat(sig.binary).isEqualTo("Default");
		assertThat(sig.classType.typeArguments).isEmpty();
		assertThat(sig.innerTypes).isEmpty();
		assertThat(sig).hasToString(signature)
			.isEqualTo(JavaTypeSignature.of(signature))
			.hasSameHashCodeAs(JavaTypeSignature.of(signature));

		signature = "Ljava/lang/Object;";
		sig = JavaTypeSignature.of(signature);
		assertThat(sig.packageSpecifier).isEqualTo("java/lang/");
		assertThat(sig.classType.identifier).isEqualTo("Object");
		assertThat(sig.binary).isEqualTo("java/lang/Object");
		assertThat(sig.classType.typeArguments).isEmpty();
		assertThat(sig.innerTypes).isEmpty();
		assertThat(sig).hasToString(signature)
			.isEqualTo(ClassTypeSignature.OBJECT)
			.hasSameHashCodeAs(ClassTypeSignature.OBJECT);

		signature = "Lfoo/baz/Bar<TA;+TB;*-TC;>;";
		sig = JavaTypeSignature.of(signature);
		assertThat(sig.packageSpecifier).isEqualTo("foo/baz/");
		assertThat(sig.classType.identifier).isEqualTo("Bar");
		assertThat(sig.binary).isEqualTo("foo/baz/Bar");
		assertThat(sig.classType.typeArguments).hasSize(4);
		assertThat(sig.classType.typeArguments[0].wildcard).isEqualTo(WildcardIndicator.EXACT);
		assertThat(sig.classType.typeArguments[0].type).isInstanceOf(TypeVariableSignature.class)
			.hasFieldOrPropertyWithValue("identifier", "A");
		assertThat(sig.classType.typeArguments[1].wildcard).isEqualTo(WildcardIndicator.EXTENDS);
		assertThat(sig.classType.typeArguments[1].type).isInstanceOf(TypeVariableSignature.class)
			.hasFieldOrPropertyWithValue("identifier", "B");
		assertThat(sig.classType.typeArguments[2].wildcard).isEqualTo(WildcardIndicator.WILD);
		assertThat(sig.classType.typeArguments[2].type).isEqualTo(ClassTypeSignature.OBJECT);
		assertThat(sig.classType.typeArguments[3].wildcard).isEqualTo(WildcardIndicator.SUPER);
		assertThat(sig.classType.typeArguments[3].type).isInstanceOf(TypeVariableSignature.class)
			.hasFieldOrPropertyWithValue("identifier", "C");
		assertThat(sig.innerTypes).isEmpty();
		assertThat(sig).hasToString(signature)
			.isEqualTo(JavaTypeSignature.of(signature))
			.hasSameHashCodeAs(JavaTypeSignature.of(signature));

		signature = "Lfoo/baz/Bar<TA;>.Biz<+TB;>.Boo<-TC;>;";
		sig = JavaTypeSignature.of(signature);
		assertThat(sig.packageSpecifier).isEqualTo("foo/baz/");
		assertThat(sig.classType.identifier).isEqualTo("Bar");
		assertThat(sig.binary).isEqualTo("foo/baz/Bar$Biz$Boo");
		assertThat(sig.classType.typeArguments).hasSize(1);
		assertThat(sig.classType.typeArguments[0].wildcard).isEqualTo(WildcardIndicator.EXACT);
		assertThat(sig.classType.typeArguments[0].type).isInstanceOf(TypeVariableSignature.class)
			.hasFieldOrPropertyWithValue("identifier", "A");
		assertThat(sig.innerTypes).hasSize(2);
		assertThat(sig.innerTypes[0].identifier).isEqualTo("Biz");
		assertThat(sig.innerTypes[0].typeArguments).hasSize(1);
		assertThat(sig.innerTypes[0].typeArguments[0].wildcard).isEqualTo(WildcardIndicator.EXTENDS);
		assertThat(sig.innerTypes[0].typeArguments[0].type).isInstanceOf(TypeVariableSignature.class)
			.hasFieldOrPropertyWithValue("identifier", "B");
		assertThat(sig.innerTypes[1].identifier).isEqualTo("Boo");
		assertThat(sig.innerTypes[1].typeArguments).hasSize(1);
		assertThat(sig.innerTypes[1].typeArguments[0].wildcard).isEqualTo(WildcardIndicator.SUPER);
		assertThat(sig.innerTypes[1].typeArguments[0].type).isInstanceOf(TypeVariableSignature.class)
			.hasFieldOrPropertyWithValue("identifier", "C");
		assertThat(sig).hasToString(signature)
			.isEqualTo(JavaTypeSignature.of(signature))
			.hasSameHashCodeAs(JavaTypeSignature.of(signature));

		signature = "Ltest/CompareTest<TO;>.A1<[Ljava/util/Collection<Ljava/lang/String;>;>;";
		sig = JavaTypeSignature.of(signature);
		assertThat(sig.packageSpecifier).isEqualTo("test/");
		assertThat(sig.classType.identifier).isEqualTo("CompareTest");
		assertThat(sig.binary).isEqualTo("test/CompareTest$A1");
		assertThat(sig.classType.typeArguments).hasSize(1);
		assertThat(sig.classType.typeArguments[0].wildcard).isEqualTo(WildcardIndicator.EXACT);
		assertThat(sig.classType.typeArguments[0].type).isInstanceOf(TypeVariableSignature.class)
			.hasFieldOrPropertyWithValue("identifier", "O");
		assertThat(sig.innerTypes).hasSize(1);
		assertThat(sig.innerTypes[0].identifier).isEqualTo("A1");
		assertThat(sig.innerTypes[0].typeArguments).hasSize(1);
		assertThat(sig.innerTypes[0].typeArguments[0].wildcard).isEqualTo(WildcardIndicator.EXACT);
		assertThat(sig.innerTypes[0].typeArguments[0].type).isInstanceOf(ArrayTypeSignature.class)
			.hasFieldOrPropertyWithValue("component",
				JavaTypeSignature.of("Ljava/util/Collection<Ljava/lang/String;>;"));
		assertThat(sig).hasToString(signature)
			.isEqualTo(JavaTypeSignature.of(signature))
			.hasSameHashCodeAs(JavaTypeSignature.of(signature));

		// Ljava/util/TreeMap<TK;TV;>.EntrySet;
		signature = "Ljava/util/TreeMap<TK;TV;>.EntrySet;";
		sig = JavaTypeSignature.of(signature);
		assertThat(sig.packageSpecifier).isEqualTo("java/util/");
		assertThat(sig.classType.identifier).isEqualTo("TreeMap");
		assertThat(sig.binary).isEqualTo("java/util/TreeMap$EntrySet");
		assertThat(sig.classType.typeArguments).hasSize(2);
		assertThat(sig.classType.typeArguments[0].wildcard).isEqualTo(WildcardIndicator.EXACT);
		assertThat(sig.classType.typeArguments[0].type).isInstanceOf(TypeVariableSignature.class)
			.hasFieldOrPropertyWithValue("identifier", "K");
		assertThat(sig.classType.typeArguments[1].wildcard).isEqualTo(WildcardIndicator.EXACT);
		assertThat(sig.classType.typeArguments[1].type).isInstanceOf(TypeVariableSignature.class)
			.hasFieldOrPropertyWithValue("identifier", "V");
		assertThat(sig.innerTypes).hasSize(1);
		assertThat(sig.innerTypes[0].identifier).isEqualTo("EntrySet");
		assertThat(sig.innerTypes[0].typeArguments).isEmpty();
		assertThat(sig).hasToString(signature)
			.isEqualTo(JavaTypeSignature.of(signature))
			.hasSameHashCodeAs(JavaTypeSignature.of(signature));

	}

	@Test
	public void testBaseTypes() {
		for (BaseType b : BaseType.values()) {
			BaseType sig = JavaTypeSignature.of(b.name());
			assertThat(sig).isSameAs(b);
		}
	}

	@Test
	public void testVoid() {
		for (VoidDescriptor v : VoidDescriptor.values()) {
			Result sig = MethodSignature.parseResult(new StringRover(v.name()));
			assertThat(sig).isSameAs(v);
		}
	}

	@Test
	public void testFieldSignature() {
		String signature = "[I";
		FieldSignature sig = FieldSignature.of(signature);
		assertThat(sig.type).isInstanceOf(ArrayTypeSignature.class)
			.hasFieldOrPropertyWithValue("component", BaseType.I);
		assertThat(sig).hasToString(signature)
			.isEqualTo(FieldSignature.of(signature))
			.hasSameHashCodeAs(FieldSignature.of(signature));
		assertThat(sig.erasedBinaryReferences()).isEmpty();

		signature = "Ltest/CompareTest<TO;>.A1<[Ljava/util/Collection<Ljava/lang/String;>;>;";
		sig = FieldSignature.of(signature);
		assertThat(sig.type).isEqualTo(JavaTypeSignature.of(signature));
		assertThat(sig).hasToString(signature)
			.isEqualTo(FieldSignature.of(signature))
			.hasSameHashCodeAs(FieldSignature.of(signature));
		assertThat(sig.erasedBinaryReferences()).containsExactlyInAnyOrder("test/CompareTest$A1",
			"java/util/Collection", "java/lang/String");

	}

	@Test
	public void testClassSignature() {
		// Ljava/lang/Object;Ljava/io/Serializable;Ljava/lang/Comparable<Ljava/lang/String;>;Ljava/lang/CharSequence;
		String signature = "Ljava/lang/Object;Ljava/io/Serializable;Ljava/lang/Comparable<Ljava/lang/String;>;Ljava/lang/CharSequence;";
		ClassSignature sig = ClassSignature.of(signature);
		assertThat(sig.superClass).isEqualTo(JavaTypeSignature.of("Ljava/lang/Object;"));
		assertThat(sig.superInterfaces).containsExactly(JavaTypeSignature.of("Ljava/io/Serializable;"),
			JavaTypeSignature.of("Ljava/lang/Comparable<Ljava/lang/String;>;"),
			JavaTypeSignature.of("Ljava/lang/CharSequence;"));
		assertThat(sig.typeParameters).isEmpty();
		assertThat(sig).hasToString(signature)
			.isEqualTo(ClassSignature.of(signature))
			.hasSameHashCodeAs(ClassSignature.of(signature));
		assertThat(sig.erasedBinaryReferences()).containsExactlyInAnyOrder("java/lang/Object", "java/io/Serializable",
			"java/lang/Comparable", "java/lang/String", "java/lang/CharSequence");

		// <T:Ljava/lang/Object;>Ljava/lang/Object;
		signature = "<T:Ljava/lang/Object;>Ljava/lang/Object;";
		sig = ClassSignature.of(signature);
		assertThat(sig.superClass).isEqualTo(JavaTypeSignature.of("Ljava/lang/Object;"));
		assertThat(sig.superInterfaces).isEmpty();
		assertThat(sig.typeParameters)
			.containsExactly(TypeParameter.parseTypeParameter(new StringRover("T:Ljava/lang/Object;")));
		assertThat(sig).hasToString(signature)
			.isEqualTo(ClassSignature.of(signature))
			.hasSameHashCodeAs(ClassSignature.of(signature));
		assertThat(sig.erasedBinaryReferences()).containsExactlyInAnyOrder("java/lang/Object");

		// <T::Ljava/io/Serializable;>Ljava/lang/Object;
		signature = "<T::Ljava/io/Serializable;>Ljava/lang/Object;";
		sig = ClassSignature.of(signature);
		assertThat(sig.superClass).isEqualTo(JavaTypeSignature.of("Ljava/lang/Object;"));
		assertThat(sig.superInterfaces).isEmpty();
		assertThat(sig.typeParameters)
			.containsExactly(TypeParameter.parseTypeParameter(new StringRover("T::Ljava/io/Serializable;")));
		assertThat(sig).hasToString(signature)
			.isEqualTo(ClassSignature.of(signature))
			.hasSameHashCodeAs(ClassSignature.of(signature));
		assertThat(sig.erasedBinaryReferences()).containsExactlyInAnyOrder("java/lang/Object", "java/io/Serializable");

		// <T:Ljava/lang/Exception;:Ljava/io/Serializable;>Ljava/lang/Object;
		signature = "<T:Ljava/lang/Exception;:Ljava/io/Serializable;>Ljava/lang/Object;";
		sig = ClassSignature.of(signature);
		assertThat(sig.superClass).isEqualTo(JavaTypeSignature.of("Ljava/lang/Object;"));
		assertThat(sig.superInterfaces).isEmpty();
		assertThat(sig.typeParameters).containsExactly(
			TypeParameter.parseTypeParameter(new StringRover("T:Ljava/lang/Exception;:Ljava/io/Serializable;")));
		assertThat(sig).hasToString(signature)
			.isEqualTo(ClassSignature.of(signature))
			.hasSameHashCodeAs(ClassSignature.of(signature));
		assertThat(sig.erasedBinaryReferences()).containsExactlyInAnyOrder("java/lang/Object", "java/io/Serializable",
			"java/lang/Exception");

		// <X:TO;>Ljava/lang/Object;
		// <VERYLONGTYPE:Ljava/lang/Object;X:LaQute/bnd/osgi/Jar;>Ljava/lang/Object;
		// <E:Ljava/lang/Object;>Ljava/util/AbstractSet<TE;>;
		// <K:Ljava/lang/Object;V:Ljava/lang/Object;>Ljava/lang/Object;

		// Ltest/CompareTest<TO;>.A1<[Ljava/util/Collection<Ljava/lang/String;>;>;
		signature = "Ltest/CompareTest<TO;>.A1<[Ljava/util/Collection<Ljava/lang/String;>;>;";
		sig = ClassSignature.of(signature);
		assertThat(sig.superClass)
			.isEqualTo(JavaTypeSignature.of("Ltest/CompareTest<TO;>.A1<[Ljava/util/Collection<Ljava/lang/String;>;>;"));
		assertThat(sig.superInterfaces).isEmpty();
		assertThat(sig.typeParameters).isEmpty();
		assertThat(sig).hasToString(signature)
			.isEqualTo(ClassSignature.of(signature))
			.hasSameHashCodeAs(ClassSignature.of(signature));
		assertThat(sig.erasedBinaryReferences()).containsExactlyInAnyOrder("java/lang/String", "java/util/Collection",
			"test/CompareTest$A1");

	}

	@Test
	public void testMethodSignature() {
		// (Ljava/util/List<LaQute/bnd/header/Attrs;>;Ljava/lang/Class<*>;ZZ)V
		String signature = "(Ljava/util/List<LaQute/bnd/header/Attrs;>;Ljava/lang/Class<*>;ZZ)V";
		MethodSignature sig = MethodSignature.of(signature);
		assertThat(sig.typeParameters).isEmpty();
		assertThat(sig.parameterTypes).hasSize(4)
			.containsExactly(JavaTypeSignature.of("Ljava/util/List<LaQute/bnd/header/Attrs;>;"),
				JavaTypeSignature.of("Ljava/lang/Class<*>;"), BaseType.Z, BaseType.Z);
		assertThat(sig.resultType).isEqualTo(VoidDescriptor.V);
		assertThat(sig.throwTypes).isEmpty();
		assertThat(sig).hasToString(signature)
			.isEqualTo(MethodSignature.of(signature))
			.hasSameHashCodeAs(MethodSignature.of(signature));
		assertThat(sig.erasedBinaryReferences()).containsExactlyInAnyOrder("java/util/List", "java/lang/Class",
			"aQute/bnd/header/Attrs");

		// ()[Ljava/lang/Class<*>;
		signature = "()[Ljava/lang/Class<*>;";
		sig = MethodSignature.of(signature);
		assertThat(sig.typeParameters).isEmpty();
		assertThat(sig.parameterTypes).isEmpty();
		assertThat(sig.resultType).isEqualTo(JavaTypeSignature.of("[Ljava/lang/Class<*>;"));
		assertThat(sig.throwTypes).isEmpty();
		assertThat(sig).hasToString(signature)
			.isEqualTo(MethodSignature.of(signature))
			.hasSameHashCodeAs(MethodSignature.of(signature));
		assertThat(sig.erasedBinaryReferences()).containsExactlyInAnyOrder("java/lang/Class");

		// ()Ljava/lang/Class<+Ljava/lang/annotation/Annotation;>;
		// <Y:Ljava/lang/Object;X:Ltest/CompareTest<TO;>.A1<TY;>;>()Ltest/CompareTest<TO;>.A1<+TX;>;
		signature = "<Y:Ljava/lang/Object;X:Ltest/CompareTest<TO;>.A1<TY;>;>()Ltest/CompareTest<TO;>.A1<+TX;>;";
		sig = MethodSignature.of(signature);
		assertThat(sig.typeParameters).hasSize(2)
			.containsExactly(TypeParameter.parseTypeParameter(new StringRover("Y:Ljava/lang/Object;")),
				TypeParameter.parseTypeParameter(new StringRover("X:Ltest/CompareTest<TO;>.A1<TY;>;")));
		assertThat(sig.parameterTypes).isEmpty();
		assertThat(sig.resultType).isEqualTo(JavaTypeSignature.of("Ltest/CompareTest<TO;>.A1<+TX;>;"));
		assertThat(sig.throwTypes).isEmpty();
		assertThat(sig).hasToString(signature)
			.isEqualTo(MethodSignature.of(signature))
			.hasSameHashCodeAs(MethodSignature.of(signature));
		assertThat(sig.erasedBinaryReferences()).containsExactlyInAnyOrder("java/lang/Object", "test/CompareTest$A1");

		// (Lorg/osgi/service/log/Logger;)V^TE;
		signature = "(Lorg/osgi/service/log/Logger;)V^TE;";
		sig = MethodSignature.of(signature);
		assertThat(sig.typeParameters).isEmpty();
		assertThat(sig.parameterTypes).hasSize(1)
			.containsExactly(JavaTypeSignature.of("Lorg/osgi/service/log/Logger;"));
		assertThat(sig.resultType).isEqualTo(VoidDescriptor.V);
		assertThat(sig.throwTypes).hasSize(1)
			.containsExactly(TypeVariableSignature.parseTypeVariableSignature(new StringRover("TE;")));
		assertThat(sig).hasToString(signature)
			.isEqualTo(MethodSignature.of(signature))
			.hasSameHashCodeAs(MethodSignature.of(signature));
		assertThat(sig.erasedBinaryReferences()).containsExactlyInAnyOrder("org/osgi/service/log/Logger");

		// <E:Ljava/lang/Throwable;>(Ljava/lang/Throwable;)V^TE;
		signature = "<E:Ljava/lang/Throwable;>(Ljava/lang/Throwable;)V^TE;";
		sig = MethodSignature.of(signature);
		assertThat(sig.typeParameters).hasSize(1)
			.containsExactly(TypeParameter.parseTypeParameter(new StringRover("E:Ljava/lang/Throwable;")));
		assertThat(sig.parameterTypes).hasSize(1)
			.containsExactly(JavaTypeSignature.of("Ljava/lang/Throwable;"));
		assertThat(sig.resultType).isEqualTo(VoidDescriptor.V);
		assertThat(sig.throwTypes).hasSize(1)
			.containsExactly(TypeVariableSignature.parseTypeVariableSignature(new StringRover("TE;")));
		assertThat(sig).hasToString(signature)
			.isEqualTo(MethodSignature.of(signature))
			.hasSameHashCodeAs(MethodSignature.of(signature));
		assertThat(sig.erasedBinaryReferences()).containsExactlyInAnyOrder("java/lang/Throwable");

		// <X:Ljava/lang/Throwable;>(Ljava/lang/Throwable;Ljava/lang/Class<TX;>;)V^TX;
		signature = "<X:Ljava/lang/Throwable;>(Ljava/lang/Throwable;Ljava/lang/Class<TX;>;)V^TX;";
		sig = MethodSignature.of(signature);
		assertThat(sig.typeParameters).hasSize(1)
			.containsExactly(TypeParameter.parseTypeParameter(new StringRover("X:Ljava/lang/Throwable;")));
		assertThat(sig.parameterTypes).hasSize(2)
			.containsExactly(JavaTypeSignature.of("Ljava/lang/Throwable;"),
				JavaTypeSignature.of("Ljava/lang/Class<TX;>;"));
		assertThat(sig.resultType).isEqualTo(VoidDescriptor.V);
		assertThat(sig.throwTypes).hasSize(1)
			.containsExactly(TypeVariableSignature.parseTypeVariableSignature(new StringRover("TX;")));
		assertThat(sig).hasToString(signature)
			.isEqualTo(MethodSignature.of(signature))
			.hasSameHashCodeAs(MethodSignature.of(signature));
		assertThat(sig.erasedBinaryReferences()).containsExactlyInAnyOrder("java/lang/Throwable", "java/lang/Class");

		// <X:Ljava/lang/Exception;>(Ljava/lang/Throwable;Ljava/lang/Class<TX;>;)Ljava/lang/RuntimeException;^Ljava/io/IOException;^TX;
		signature = "<X:Ljava/lang/Exception;>(Ljava/lang/Throwable;Ljava/lang/Class<TX;>;)Ljava/lang/RuntimeException;^Ljava/io/IOException;^TX;";
		sig = MethodSignature.of(signature);
		assertThat(sig.typeParameters).hasSize(1)
			.containsExactly(TypeParameter.parseTypeParameter(new StringRover("X:Ljava/lang/Exception;")));
		assertThat(sig.parameterTypes).hasSize(2)
			.containsExactly(JavaTypeSignature.of("Ljava/lang/Throwable;"),
				JavaTypeSignature.of("Ljava/lang/Class<TX;>;"));
		assertThat(sig.resultType).isEqualTo(JavaTypeSignature.of("Ljava/lang/RuntimeException;"));
		assertThat(sig.throwTypes).hasSize(2)
			.containsExactly(JavaTypeSignature.of("Ljava/io/IOException;"),
				TypeVariableSignature.parseTypeVariableSignature(new StringRover("TX;")));
		assertThat(sig).hasToString(signature)
			.isEqualTo(MethodSignature.of(signature))
			.hasSameHashCodeAs(MethodSignature.of(signature));
		assertThat(sig.erasedBinaryReferences()).containsExactlyInAnyOrder("java/lang/Throwable", "java/lang/Class",
			"java/lang/Exception", "java/lang/RuntimeException", "java/io/IOException");

		// <X1:Ljava/lang/Exception;X2:Ljava/lang/Exception;>(Ljava/lang/Throwable;Ljava/lang/Class<TX1;>;Ljava/lang/Class<TX2;>;)Ljava/lang/RuntimeException;^Ljava/io/IOException;^TX1;^TX2;
		signature = "<X1:Ljava/lang/Exception;X2:Ljava/lang/Exception;>(Ljava/lang/Throwable;Ljava/lang/Class<TX1;>;Ljava/lang/Class<TX2;>;)Ljava/lang/RuntimeException;^Ljava/io/IOException;^TX1;^TX2;";
		sig = MethodSignature.of(signature);
		assertThat(sig.typeParameters).hasSize(2)
			.containsExactly(TypeParameter.parseTypeParameter(new StringRover("X1:Ljava/lang/Exception;")),
				TypeParameter.parseTypeParameter(new StringRover("X2:Ljava/lang/Exception;")));
		assertThat(sig.parameterTypes).hasSize(3)
			.containsExactly(JavaTypeSignature.of("Ljava/lang/Throwable;"),
				JavaTypeSignature.of("Ljava/lang/Class<TX1;>;"), JavaTypeSignature.of("Ljava/lang/Class<TX2;>;"));
		assertThat(sig.resultType).isEqualTo(JavaTypeSignature.of("Ljava/lang/RuntimeException;"));
		assertThat(sig.throwTypes).hasSize(3)
			.containsExactly(JavaTypeSignature.of("Ljava/io/IOException;"),
				TypeVariableSignature.parseTypeVariableSignature(new StringRover("TX1;")),
				TypeVariableSignature.parseTypeVariableSignature(new StringRover("TX2;")));
		assertThat(sig).hasToString(signature)
			.isEqualTo(MethodSignature.of(signature))
			.hasSameHashCodeAs(MethodSignature.of(signature));
		assertThat(sig.erasedBinaryReferences()).containsExactlyInAnyOrder("java/lang/Throwable", "java/lang/Class",
			"java/lang/Exception", "java/lang/RuntimeException", "java/io/IOException");

		// <T::Ljava/lang/Comparable<*>;>(Ljava/util/Iterator<TT;>;)LaQute/lib/collections/SortedList<TT;>;
		signature = "<T::Ljava/lang/Comparable<*>;>(Ljava/util/Iterator<TT;>;)LaQute/lib/collections/SortedList<TT;>;";
		sig = MethodSignature.of(signature);
		assertThat(sig.typeParameters).hasSize(1)
			.containsExactly(TypeParameter.parseTypeParameter(new StringRover("T::Ljava/lang/Comparable<*>;")));
		assertThat(sig.parameterTypes).hasSize(1)
			.containsExactly(JavaTypeSignature.of("Ljava/util/Iterator<TT;>;"));
		assertThat(sig.resultType).isEqualTo(JavaTypeSignature.of("LaQute/lib/collections/SortedList<TT;>;"));
		assertThat(sig.throwTypes).isEmpty();
		assertThat(sig).hasToString(signature)
			.isEqualTo(MethodSignature.of(signature))
			.hasSameHashCodeAs(MethodSignature.of(signature));
		assertThat(sig.erasedBinaryReferences()).containsExactlyInAnyOrder("java/lang/Comparable", "java/util/Iterator",
			"aQute/lib/collections/SortedList");

	}

	@Test
	public void testTypeParameter() {
		StringRover r = new StringRover("NOBOUNDS:");
		TypeParameter nobounds = TypeParameter.parseTypeParameter(r);
		assertThat(nobounds.identifier).isEqualTo("NOBOUNDS");
		assertThat(nobounds.classBound).isNull();
		assertThat(nobounds.interfaceBounds).isEmpty();
		assertThat(nobounds).hasToString("NOBOUNDS:")
			.isEqualTo(TypeParameter.parseTypeParameter(r.duplicate()
				.reset()));

		r = new StringRover("CLASSBOUNDS:Lclass/Bound;");
		TypeParameter classbounds = TypeParameter.parseTypeParameter(r);
		assertThat(classbounds.identifier).isEqualTo("CLASSBOUNDS");
		assertThat(classbounds.classBound).isEqualTo(JavaTypeSignature.of("Lclass/Bound;"));
		assertThat(classbounds.interfaceBounds).isEmpty();
		assertThat(classbounds).hasToString("CLASSBOUNDS:Lclass/Bound;")
			.isEqualTo(TypeParameter.parseTypeParameter(r.duplicate()
				.reset()));

		r = new StringRover("INTERFACEBOUNDS::Linterface/Bound;");
		TypeParameter interfacebounds = TypeParameter.parseTypeParameter(r);
		assertThat(interfacebounds.identifier).isEqualTo("INTERFACEBOUNDS");
		assertThat(interfacebounds.classBound).isNull();
		assertThat(interfacebounds.interfaceBounds).containsExactly(JavaTypeSignature.of("Linterface/Bound;"));
		assertThat(interfacebounds).hasToString("INTERFACEBOUNDS::Linterface/Bound;")
			.isEqualTo(TypeParameter.parseTypeParameter(r.duplicate()
				.reset()));

		r = new StringRover("BOTHBOUNDS:Lclass/Bound;:Linterface/Bound1;:Linterface/Bound2;");
		TypeParameter bothbounds = TypeParameter.parseTypeParameter(r);
		assertThat(bothbounds.identifier).isEqualTo("BOTHBOUNDS");
		assertThat(bothbounds.classBound).isEqualTo(JavaTypeSignature.of("Lclass/Bound;"));
		assertThat(bothbounds.interfaceBounds).containsExactly(JavaTypeSignature.of("Linterface/Bound1;"),
			JavaTypeSignature.of("Linterface/Bound2;"));
		assertThat(bothbounds).hasToString("BOTHBOUNDS:Lclass/Bound;:Linterface/Bound1;:Linterface/Bound2;")
			.isEqualTo(TypeParameter.parseTypeParameter(r.duplicate()
				.reset()));
	}

}
