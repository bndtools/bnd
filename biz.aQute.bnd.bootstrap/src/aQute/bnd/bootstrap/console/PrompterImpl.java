package aQute.bnd.bootstrap.console;

import java.io.*;
import java.lang.reflect.*;
import java.util.*;

import jline.console.*;
import aQute.bnd.annotation.metatype.*;
import aQute.bnd.annotation.metatype.Meta.OCD;
import aQute.bnd.annotation.metatype.Meta.AD;
import aQute.lib.converter.*;
import aQute.lib.justif.*;

public class PrompterImpl {

	private ConsoleReader console;

	public PrompterImpl(ConsoleReader console) {
		this.console = console;
	}

	public <T> T prompt(Class<T> spec) throws Exception {
		final Map<String, Object> properties = new HashMap<String, Object>();
		PrintWriter pw = new PrintWriter(console.getOutput());

		T target = Converter.cnv(spec, properties);

		String typeName = uncamel(shortName(spec.getName()));
		String typeDescription = null;

		OCD ocd = spec.getAnnotation(Meta.OCD.class);
		if (ocd != null) {
			typeDescription = ocd.description();
			typeName = ocd.name();
		}

		Method methods[] = spec.getMethods();
		int n = 0;
		outer: while (true) {

			while (n >= methods.length) {
				pw.print("S[ave], C[ancel], V[iew], R[estart]");
				pw.flush();

				switch (Character.toLowerCase(console.readCharacter())) {
				case 'y':
					break outer;

				case 'c':
					return null;

				case 'v':
					view(pw, spec, target);
					break;

				case 'r':
					n = 0;
					break;
				}

				pw.println("Either ok, cancel, view, or reset");
			}

			Method current = methods[n];
			String name = current.getName();

			String description = null;
			String deflt = null;
			double max = Double.MAX_VALUE;
			double min = Double.MIN_VALUE;
			boolean required = true;
			boolean multiple = Collection.class.isAssignableFrom(current
					.getReturnType()) || current.getReturnType().isArray();

			AD ad = current.getAnnotation(Meta.AD.class);

			if (ad != null) {
				if (checkNull(ad.name()) != null)
					name = ad.name();

				description = checkNull(ad.description());
				if (checkNull(ad.deflt()) != null)
					;
				deflt = ad.deflt();

				if (checkNull(ad.max()) != null)
					max = Double.parseDouble(ad.max());
				if (checkNull(ad.min()) != null)
					min = Double.parseDouble(ad.min());

				required = ad.required();
			}
			Object value = current.invoke(target);

			Justif justif = new Justif();
			justif.formatter().format("%-3s: %s (%s): ", n, name,
					current.invoke(target));

			
			String line = console.readLine(justif.formatter().toString());
			if ("exit".equals(line))
				break;

			n++;
		}

		return target;
	}

	private <R> void view(PrintWriter pw, Class<R> spec, R o) {
	}

	private String checkNull(String v) {
		return Meta.NULL.equals(v) ? null : v;
	}

	private String uncamel(String v) {
		return v;
	}

	private String shortName(String name) {
		int n = name.lastIndexOf('.');
		return name.substring(n + 1);
	}

	@Meta.OCD(description = "Test Interface")
	interface Test {
		@Meta.AD(description = "IP Port", deflt = "8080")
		int port();

		String host();
	}

	public static void main(String args[]) throws Exception {

		ConsoleReader cr = new ConsoleReader(System.in, System.out);
		PrompterImpl pi = new PrompterImpl(cr);

		pi.prompt(Test.class);

	}

}
