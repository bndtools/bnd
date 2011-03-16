package aQute.bnd.make.metatype;

import java.io.*;
import java.util.*;
import java.util.regex.*;

import aQute.bnd.annotation.metatype.*;
import aQute.lib.io.*;
import aQute.lib.osgi.*;
import aQute.lib.osgi.Clazz.MethodDef;
import aQute.lib.tag.*;
import aQute.libg.generics.*;

public class MetaTypeReader extends ClassDataCollector implements Resource {
	final Analyzer				reporter;
	Clazz						clazz;
	String						interfaces[];
	Tag							metadata	= new Tag("metatype:MetaData", new String[] {
			"xmlns:metatype", "http://www.osgi.org/xmlns/metatype/v1.1.0" });
	Tag							ocd			= new Tag(metadata, "OCD");
	Tag							designate	= new Tag(metadata, "Designate");
	Tag							object		= new Tag(designate, "Object");

	// Resource
	String						extra;

	// One time init
	boolean						finished;

	// Designate
	boolean						override;
	String						designatePid;
	boolean						factory;

	// AD
	Map<MethodDef, Metadata.AD>	methods		= new LinkedHashMap<MethodDef, Metadata.AD>();

	// OCD
	Annotation					ocdAnnotation;

	MethodDef					method;

	public MetaTypeReader(Clazz clazz, Analyzer reporter) {
		this.clazz = clazz;
		this.reporter = reporter;
	}

	public void annotation(Annotation annotation) {
		try {
			Metadata.OCD ocd = annotation.getAnnotation(Metadata.OCD.class);
			Metadata.AD ad = annotation.getAnnotation(Metadata.AD.class);
			if (ocd != null) {
				this.ocdAnnotation = annotation;
			}
			if (ad != null) {
				assert method != null;
				methods.put(method, ad);
			}
		} catch (Exception e) {
			reporter.error("Error during annotation parsing %s : %s", clazz, e);
			e.printStackTrace();
		}
	}

	/**
	 * @param id
	 * @param name
	 * @param cardinality
	 * @param required
	 * @param deflt
	 * @param type
	 * @param max
	 * @param min
	 * @param optionLabels
	 * @param optionValues
	 */

	static Pattern	COLLECTION	= Pattern
										.compile("(.*(Collection|Set|List|Queue|Stack|Deque))<(L.+;)>");

	private void addMethod(MethodDef method, Metadata.AD ad) throws Exception {

		// Set all the defaults.
		String rtype = method.getReturnType();
		String id = method.name;
		String name = Clazz.unCamel(id);

		int cardinality = 0;

		if (rtype.endsWith("[]")) {
			cardinality = Integer.MAX_VALUE;
			rtype = rtype.substring(0, rtype.length() - 2);
		}
		if (rtype.indexOf('<') > 0) {
			if (cardinality != 0)
				reporter.error(
						"AD for %s.%s uses an array of collections in return type (%s), Metatype allows either Vector or array",
						clazz.getFQN(), method.name, method.getReturnType());
			Matcher m = COLLECTION.matcher(rtype);
			if (m.matches()) {
				rtype = Clazz.objectDescriptorToFQN(m.group(3));
				cardinality = Integer.MIN_VALUE;
			}
		}

		String type = getType(rtype);
		boolean required = true;
		String deflt = null;
		String max = null;
		String min = null;
		String[] optionLabels = null;
		String[] optionValues = null;
		String description = null;

		Clazz c = reporter.findClass(Clazz.fqnToPath(rtype));
		if (c != null && c.isEnum()) {
			optionValues = parseOptionValues(c);
		}

		// Now parse the annotation for any overrides

		if (ad != null) {
			if (ad.id() != null)
				id = ad.id();
			if (ad.name() != null)
				name = ad.name();
			if (ad.cardinality() != 0)
				cardinality = ad.cardinality();
			if (ad.type() != null)
				type = ad.type();
			if (ad.required() || ad.deflt() == null)
				required = true;

			if (ad.description() != null)
				description = ad.description();

			if (ad.optionLabels() == null)
				optionLabels = ad.optionLabels();
			if (ad.optionValues() == null)
				optionValues = ad.optionValues();

			if (ad.min() != null)
				min = ad.min();
			if (ad.max() != null)
				max = ad.max();

			if (ad.deflt() != null)
				deflt = ad.deflt();
		}

		// Verify
		if (!type.matches("Long|Double|Float|Integer|Byte|Char|Boolean|Short|String"))
			reporter.error("Invalid AD type for %s, %s", clazz.getFQN(), type);

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

		if (optionLabels != null) {
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
			public void field(Clazz.FieldDef def) {
				if (def.isEnum()) {
					values.add(def.name);
				}
			}
		});
		return values.toArray(new String[values.size()]);
	}

	String getType(String rtype) {
		if (rtype.endsWith("[]")) {
			rtype = rtype.substring(0, rtype.length() - 2);
			if (rtype.endsWith("[]"))
				throw new IllegalArgumentException("Can only handle array of depth one");
		}

		if ("boolean".equals(rtype) || Boolean.class.getName().equals(rtype))
			return "Boolean";
		else if ("byte".equals(rtype) || Byte.class.getName().equals(rtype))
			return "Byte";
		else if ("char".equals(rtype) || Character.class.getName().equals(rtype))
			return "Char";
		else if ("short".equals(rtype) || Short.class.getName().equals(rtype))
			return "Short";
		else if ("int".equals(rtype) || Integer.class.getName().equals(rtype))
			return "Integer";
		else if ("long".equals(rtype) || Long.class.getName().equals(rtype))
			return "Long";
		else if ("float".equals(rtype) || Float.class.getName().equals(rtype))
			return "Float";
		else if ("double".equals(rtype) || Double.class.getName().equals(rtype))
			return "Double";
		else
			return "String";
	}

	@Override public void method(MethodDef mdef) {
		method = mdef;
		methods.put(mdef, null);
	}

	public String getExtra() {
		return extra;
	}

	public long lastModified() {
		return 0;
	}

	public InputStream openInputStream() throws IOException {
		final PipedInputStream pin = new PipedInputStream();
		final PipedOutputStream pout = new PipedOutputStream(pin);
		Processor.getExecutor().execute(new Runnable() {
			public void run() {
				try {
					write(pout);
				} catch (IOException e) {
					// Cause an exception in the other end
					IO.close(pin);
				}
				IO.close(pout);
			}
		});
		return pin;
	}

	public void setExtra(String extra) {
		this.extra = extra;
	}

	public void write(OutputStream out) throws IOException {
		try {
			finish();
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
		PrintWriter pw = new PrintWriter(new OutputStreamWriter(out));
		pw.println("<?xml version='1.0'?>");
		metadata.print(0, pw);
		pw.flush();
	}

	void finish() throws Exception {
		if (!finished) {
			finished = true;
			clazz.parseClassFileWithCollector(this);
			Metadata.OCD ocd = null;
			if (this.ocdAnnotation != null)
				ocd = this.ocdAnnotation.getAnnotation(Metadata.OCD.class);
			else
				ocd = Configurable.createConfigurable(Metadata.OCD.class,
						new HashMap<String, Object>());

			// defaults
			String id = clazz.getFQN();
			String name = Clazz.unCamel(Clazz.getShortName(clazz.getFQN()));
			String description = null;
			String localization = id;
			String designatePid = this.designatePid;
			boolean factory = this.factory;

			if (override) {
				// Seems an error in Felix Metatype
				// requires that the OCD has an ID that matches the PID :-(
				id = designatePid;
			} else if (ocd.id() != null)
				id = ocd.id();

			if (ocd.name() != null)
				name = ocd.name();

			if (ocd.localization() != null)
				localization = ocd.localization();

			if (ocd.description() != null)
				description = ocd.description();

			if (!override) {
				if (ocdAnnotation.get("designate") != null) {
					factory = false;
					designatePid = ocdAnnotation.get("designate");
				} else if (ocdAnnotation.get("designateFactory") != null) {
					factory = true;
					designatePid = ocdAnnotation.get("designateFactory");
				} else {
					factory = false;
					designatePid = id;
				}
			}

			this.ocd.addAttribute("name", name);
			this.ocd.addAttribute("id", id);
			this.ocd.addAttribute("description", description);
			this.ocd.addAttribute("localization", localization);

			// do ADs
			for (Map.Entry<MethodDef, Metadata.AD> entry : methods.entrySet())
				addMethod(entry.getKey(), entry.getValue());

			this.designate.addAttribute("pid", designatePid);
			if (factory)
				this.designate.addAttribute("factoryPid", designatePid);

			this.object.addAttribute("ocdref", id);

		}
	}

	public void setDesignate(String pid, boolean factory) {
		this.override = true;
		this.factory = factory;
		this.designatePid = pid;
	}
}
