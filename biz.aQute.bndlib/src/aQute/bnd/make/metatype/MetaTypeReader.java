package aQute.bnd.make.metatype;

import static java.nio.charset.StandardCharsets.UTF_8;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import aQute.bnd.annotation.metatype.Meta;
import aQute.bnd.osgi.Analyzer;
import aQute.bnd.osgi.Annotation;
import aQute.bnd.osgi.ClassDataCollector;
import aQute.bnd.osgi.Clazz;
import aQute.bnd.osgi.Clazz.MethodDef;
import aQute.bnd.osgi.Descriptors.TypeRef;
import aQute.bnd.osgi.Processor;
import aQute.bnd.osgi.WriteResource;
import aQute.lib.io.IO;
import aQute.lib.tag.Tag;
import aQute.libg.generics.Create;

public class MetaTypeReader extends WriteResource {
	final Analyzer				reporter;
	Clazz						clazz;
	String						interfaces[];
	Tag							metadata	= new Tag("metatype:MetaData", new String[] {
		"xmlns:metatype", "http://www.osgi.org/xmlns/metatype/v1.1.0"
	});
	Tag							ocd			= new Tag(metadata, "OCD");
	Tag							designate	= new Tag(metadata, "Designate");
	Tag							object		= new Tag(designate, "Object");

	// Resource
	String						extra;

	// Should we process super interfaces
	boolean						inherit;

	// One time init
	boolean						finished;

	// Designate
	boolean						override;
	String						designatePid;
	boolean						factory;

	// AD
	Map<MethodDef, Annotation>	methods		= new LinkedHashMap<>();

	// OCD
	Annotation					ocdAnnotation;

	MethodDef					method;

	public MetaTypeReader(Clazz clazz, Analyzer reporter) {
		this.clazz = clazz;
		this.reporter = reporter;
		this.inherit = Processor.isTrue(reporter.getProperty("-metatype-inherit"));
	}

	private final static Pattern COLLECTION = Pattern.compile("(.*(Collection|Set|List|Queue|Stack|Deque))<(L.+;)>;");

	private void addMethod(MethodDef method, Annotation a) throws Exception {

		if (method.isStatic())
			return;

		// Set all the defaults.

		String rtype = method.getGenericReturnType();
		String id = mangleMethodName(method.getName());
		String name = Clazz.unCamel(id);

		int cardinality = 0;

		if (rtype.startsWith("[")) {
			cardinality = Integer.MAX_VALUE;
			rtype = rtype.substring(1);
		}
		if (rtype.indexOf('<') > 0) {
			if (cardinality != 0)
				reporter.error(
					"AD for %s.%s uses an array of collections in return type (%s), Metatype allows either Vector or array",
					clazz.getClassName()
						.getFQN(),
					method.getName(), method.getType()
						.getFQN());
			Matcher m = COLLECTION.matcher(rtype);
			if (m.matches()) {
				rtype = Clazz.objectDescriptorToFQN(m.group(3));
				cardinality = Integer.MIN_VALUE;
			}
		}

		Meta.Type type = getType(rtype);
		Meta.AD ad = a == null ? null : a.getAnnotation(Meta.AD.class);

		boolean required = true;
		if (a != null && ad != null) {
			required = a.get("required") != null ? ad.required() : a.get("deflt") == null;
		}
		String deflt = null;
		String max = null;
		String min = null;
		String[] optionLabels = null;
		String[] optionValues = null;
		String description = null;

		TypeRef typeRef = reporter.getTypeRef(rtype);
		Clazz c = reporter.findClass(typeRef);
		if (c != null && c.isEnum()) {
			optionValues = parseOptionValues(c);
		}

		// Now parse the annotation for any overrides

		if (a != null && ad != null) {
			if (a.get("id") != null)
				id = ad.id();
			if (a.get("name") != null)
				name = ad.name();
			if (a.get("cardinality") != null)
				cardinality = ad.cardinality() == 0 ? cardinality : ad.cardinality();
			if (a.get("type") != null)
				type = ad.type();
			if (a.get("description") != null)
				description = ad.description();

			if (a.get("optionLabels") != null)
				optionLabels = ad.optionLabels();
			if (a.get("optionValues") != null)
				optionValues = ad.optionValues();

			if (a.get("min") != null)
				min = ad.min();
			if (a.get("max") != null)
				max = ad.max();

			if (a.get("deflt") != null)
				deflt = ad.deflt();
		}
		if (optionValues != null) {
			if (optionLabels == null || optionLabels.length == 0) {
				optionLabels = new String[optionValues.length];
				for (int i = 0; i < optionValues.length; i++)
					optionLabels[i] = Clazz.unCamel(optionValues[i]);
			}

			if (optionLabels.length != optionValues.length) {
				reporter.error("Option labels and option values not the same length for %s", id);
				optionLabels = optionValues;
			}
		}

		Tag adt = new Tag(this.ocd, "AD");
		adt.addAttribute("name", name);
		adt.addAttribute("id", id);
		adt.addAttribute("cardinality", cardinality);
		adt.addAttribute("required", required);
		adt.addAttribute("default", deflt);
		adt.addAttribute("type", type);
		adt.addAttribute("max", max);
		adt.addAttribute("min", min);
		adt.addAttribute("description", description);

		if (optionLabels != null && optionValues != null) {
			for (int i = 0; i < optionLabels.length; i++) {
				Tag option = new Tag(adt, "Option");
				option.addAttribute("label", optionLabels[i]);
				option.addAttribute("value", optionValues[i]);
			}
		}
	}

	private String[] parseOptionValues(Clazz c) throws Exception {
		final List<String> values = Create.list();

		c.parseClassFileWithCollector(new ClassDataCollector() {
			@Override
			public void field(Clazz.FieldDef def) {
				if (def.isEnum()) {
					values.add(def.getName());
				}
			}
		});
		return values.toArray(new String[0]);
	}

	Meta.Type getType(String rtype) {
		if (rtype.startsWith("[")) {
			rtype = rtype.substring(1);
			if (rtype.startsWith("[")) {
				throw new IllegalArgumentException("Can only handle array of depth one");
			}
		}
		switch (rtype) {
			case "Z" :
			case "Ljava/lang/Boolean;" :
				return Meta.Type.Boolean;
			case "B" :
			case "Ljava/lang/Byte;" :
				return Meta.Type.Byte;
			case "C" :
			case "Ljava/lang/Character;" :
				return Meta.Type.Character;
			case "S" :
			case "Ljava/lang/Short;" :
				return Meta.Type.Short;
			case "I" :
			case "Ljava/lang/Integer;" :
				return Meta.Type.Integer;
			case "J" :
			case "Ljava/lang/Long;" :
				return Meta.Type.Long;
			case "F" :
			case "Ljava/lang/Float;" :
				return Meta.Type.Float;
			case "D" :
			case "Ljava/lang/Double;" :
				return Meta.Type.Double;
			default :
				return Meta.Type.String;
		}
	}

	class Find extends ClassDataCollector {

		@Override
		public void method(MethodDef mdef) {
			method = mdef;
			methods.put(mdef, null);
		}

		@Override
		public void annotation(Annotation annotation) {
			try {
				if (Meta.OCD.class.getName()
					.equals(annotation.getName()
						.getFQN())) {
					MetaTypeReader.this.ocdAnnotation = annotation;
				} else if (Meta.AD.class.getName()
					.equals(annotation.getName()
						.getFQN())) {
					assert method != null;
					methods.put(method, annotation);
				}
			} catch (Exception e) {
				reporter.error("Error during annotation parsing %s : %s", clazz, e);
				e.printStackTrace();
			}
		}

	}

	@Override
	public void write(OutputStream out) throws IOException {
		try {
			finish();
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
		PrintWriter pw = IO.writer(out, UTF_8);
		pw.print("<?xml version='1.0' encoding='UTF-8'?>\n");
		metadata.print(0, pw);
		pw.flush();
	}

	void finish() throws Exception {
		if (!finished) {
			finished = true;
			clazz.parseClassFileWithCollector(new Find());

			// defaults
			String id = clazz.getClassName()
				.getFQN();
			String name = Clazz.unCamel(clazz.getClassName()
				.getShortName());
			String description = null;
			String localization = id;
			boolean factory = this.factory;

			if (this.ocdAnnotation != null) {
				Meta.OCD ocd = this.ocdAnnotation.getAnnotation(Meta.OCD.class);
				if (this.ocdAnnotation.get("id") != null)
					id = ocd.id();

				if (this.ocdAnnotation.get("name") != null)
					name = ocd.name();

				if (this.ocdAnnotation.get("localization") != null)
					localization = ocd.localization();

				if (this.ocdAnnotation.get("description") != null)
					description = ocd.description();
			}

			String pid = id;
			if (override) {
				pid = this.designatePid;
				factory = this.factory;
				id = this.designatePid; // for the felix problems
			} else {
				if (ocdAnnotation.get("factory") != null) {
					factory = true;
				}
			}

			this.ocd.addAttribute("name", name);
			this.ocd.addAttribute("id", id);
			this.ocd.addAttribute("description", description);
			this.metadata.addAttribute("localization", localization);

			// do ADs
			for (Map.Entry<MethodDef, Annotation> entry : methods.entrySet())
				addMethod(entry.getKey(), entry.getValue());

			this.designate.addAttribute("pid", pid);
			if (factory)
				this.designate.addAttribute("factoryPid", pid);

			this.object.addAttribute("ocdref", id);

			if (inherit) {
				handleInheritedClasses(clazz);
			}
		}
	}

	private void handleInheritedClasses(Clazz child) throws Exception {
		TypeRef[] ifaces = child.getInterfaces();
		if (ifaces != null) {
			for (TypeRef ref : ifaces) {
				parseAndMergeInheritedMetadata(ref, child);
			}
		}
		TypeRef superClazz = child.getSuper();
		if (superClazz != null) {
			parseAndMergeInheritedMetadata(superClazz, child);
		}
	}

	private void parseAndMergeInheritedMetadata(TypeRef ref, Clazz child) throws Exception {
		if (ref.isJava())
			return;
		Clazz ec = reporter.findClass(ref);
		if (ec == null) {
			reporter.error("Missing inherited class for Metatype annotations: %s from %s", ref, child.getClassName());
		} else {
			@SuppressWarnings("resource")
			MetaTypeReader mtr = new MetaTypeReader(ec, reporter);
			mtr.setDesignate(designatePid, factory);
			mtr.finish();
			for (Map.Entry<MethodDef, Annotation> entry : mtr.methods.entrySet())
				addMethod(entry.getKey(), entry.getValue());

			handleInheritedClasses(ec);
		}
	}

	public void setDesignate(String pid, boolean factory) {
		this.override = true;
		this.factory = factory;
		this.designatePid = pid;
	}

	@Override
	public long lastModified() {
		return 0;
	}

	private static String mangleMethodName(String id) {
		StringBuilder sb = new StringBuilder(id);
		for (int i = 0; i < sb.length(); i++) {
			char c = sb.charAt(i);
			boolean twice = i < sb.length() - 1 && sb.charAt(i + 1) == c;
			if (c == '$' || c == '_') {
				if (twice)
					sb.deleteCharAt(i + 1);
				else if (c == '$')
					sb.deleteCharAt(i--); // Remove dollars
				else
					sb.setCharAt(i, '.'); // Make _ into .
			}
		}
		return sb.toString();
	}
}
