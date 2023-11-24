package biz.aQute.bnd.proxy.generator;

import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Stream;

import aQute.bnd.exceptions.Exceptions;
import aQute.bnd.osgi.Analyzer;
import aQute.bnd.osgi.Clazz;
import aQute.bnd.osgi.Clazz.MethodDef;
import aQute.bnd.osgi.Descriptors.NamedDescriptor;
import aQute.bnd.osgi.Descriptors.TypeRef;
import biz.aQute.bnd.javagen.util.JavaSourceBuilder;

class Source {

	final Map<NamedDescriptor, MethodDef>	methods	= new LinkedHashMap<>();
	final Set<TypeRef>						visited	= new HashSet<>();
	final Analyzer							analyzer;
	final TypeRef							base;
	final TypeRef[]							implements_;
	final TypeRef							extends_;
	final TypeRef							facade;


	Source(Analyzer analyzer, TypeRef facade, TypeRef base, TypeRef... domains) throws Exception {

		if (domains.length == 0)
			throw new IllegalArgumentException("you must specify at least one type to implement or extend");

		Clazz primary = analyzer.findClass(domains[0]);
		if (primary == null)
			throw new IllegalArgumentException("The primary type " + domains[0] + " cannot be found");

		for (int i = 1; i < domains.length; i++) {
			Clazz c = analyzer.findClass(domains[i]);
			if (c == null)
				throw new IllegalArgumentException("The auxiliary type " + domains[i] + " cannot be found");

			if (!c.isInterface())
				throw new IllegalArgumentException("The auxiliary type " + domains[i] + " is not an interface");
		}

		if (primary.isInterface()) {
			this.extends_ = null;
			this.implements_ = domains;
		} else {
			this.extends_ = domains[0];
			this.implements_ = Arrays.copyOfRange(domains, 1, domains.length);
		}

		this.facade = facade;
		this.analyzer = analyzer;
		this.base = base;
	}

	String source() throws Exception {
		parse(extends_);
		for (TypeRef d : implements_) {
			parse(d);
		}

		JavaSourceBuilder sb = new JavaSourceBuilder();
		sb.package_(facade.getPackageRef());
		sb.nl();
		sb.import_(getImported());
		sb.nl();

		sb.public_()
			.class_(facade)
			.extends_(base)
			.body(j -> {

				j.public_()
					.interface_("Delegate")
					.extends_(implements_)
					.body(jj -> {

						getOverridableMethods(extends_).forEach(m -> {
							sb.method(m)
								.append(";")
								.nl();
						});
					})
					.nl(2);

				j.public_()
					.static_()
					.class_("Facade")
					.extends_(extends_)
					.implements_(implements_)
					.body(jjj -> {

						sb.final_()
							.format("Supplier<Delegate> bind;")
							.nl();
						sb.suppressWarnings("unchecked", "rawtypes");
						sb.format(
							"Facade(Function<Object,Supplier<Object>> binding) { this.bind = (Supplier) binding.apply(this) /* I know */; }")
							.nl(2);

						methods.values()
							.forEach(m -> {
								if (!m.isFinal()) {
									sb.override()
										.public_();
									sb.method(m)
										.body(y -> {
											if (!isVoid(m.getType())) {
												sb.return_();
											}
											sb.append("bind.get().");
											sb.call(m);
										})
										.nl();
								}
							});
					});

				sb.nl()
					.public_()
					.format(
						"Facade createFacade(Function<Object,Supplier<Object>> binder) { return new Facade(binder); }");
			});

		return sb.toString();
	}

	private Stream<MethodDef> getOverridableMethods(TypeRef type) {
		if (type == null)
			return Stream.empty();

		try {
			Clazz clazz = analyzer.findClass(type);
			return clazz.methods()
				.filter(m -> {
					return !(m.isConstructor() || m.isStatic() || m.isSynthetic() || m.isFinal());
				});
		} catch (Exception e) {
			throw Exceptions.duck(e);
		}
	}

	private boolean isVoid(TypeRef type) {
		return type.isPrimitive() && type.getFQN()
			.equals("void");
	}

	private Set<TypeRef> getImported() {
		TreeSet<TypeRef> result = new TreeSet<>();
		if (extends_ != null)
			result.add(extends_);
		for (TypeRef tr : implements_) {
			result.add(tr);
		}
		result.add(base);
		result.add(analyzer.getTypeRefFromFQN(Function.class.getName()));
		result.add(analyzer.getTypeRefFromFQN(Supplier.class.getName()));
		methods.keySet()
			.stream()
			.map(nd -> nd.descriptor)
			.forEach(d -> {
				result.add(d.getType());
				for (TypeRef t : d.getPrototype()) {
					result.add(t);
				}
			});
		result.removeIf(this::isNotImport);
		return result;
	}

	private boolean isNotImport(TypeRef ref) {
		try {
			Clazz c = analyzer.findClass(ref);
			if (c == null)
				return true;

			return false;
		} catch (Exception e) {
			throw Exceptions.duck(e);
		}
	}

	void parse(TypeRef type) throws Exception {
		assert type != null;

		if (type.isObject())
			return;

		if (!visited.add(type))
			return;

		Clazz clazz = analyzer.findClass(type);
		if (clazz == null)
			throw new IllegalArgumentException("cannot locate a type in the hierarchy: " + type);

		boolean isInterface = clazz.isInterface();

		clazz.methods()
			.forEach(m -> {

				if (m.isSynthetic())
					return;

				if (m.isStatic() || m.isConstructor()) {
					return;
				}

				NamedDescriptor p = m.getNamedDescriptor();

				MethodDef current = methods.putIfAbsent(p, m);
				if (current != null && current.isAbstract() && !m.isAbstract()) {
					methods.put(p, m);
				}
			});

		TypeRef super1 = clazz.getSuper();
		if (super1 == null)
			return;

		parse(super1);

		for (TypeRef intf : clazz.interfaces()) {
			parse(intf);
		}
	}

}
