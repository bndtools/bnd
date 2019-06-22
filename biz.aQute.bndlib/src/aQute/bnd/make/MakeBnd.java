package aQute.bnd.make;

import java.io.File;
import java.util.Map;

import aQute.bnd.osgi.Builder;
import aQute.bnd.osgi.Constants;
import aQute.bnd.osgi.Jar;
import aQute.bnd.osgi.JarResource;
import aQute.bnd.osgi.Resource;
import aQute.bnd.service.MakePlugin;

public class MakeBnd implements MakePlugin, Constants {

	@Override
	public Resource make(Builder builder, String destination, Map<String, String> argumentsOnMake) throws Exception {
		String type = argumentsOnMake.get("type");
		if (!"bnd".equals(type))
			return null;

		String recipe = argumentsOnMake.get("recipe");
		if (recipe == null) {
			builder.error("No recipe specified on a make instruction for %s, args=%s", destination, argumentsOnMake);
			return null;
		}
		File bndfile = builder.getFile(recipe);
		if (!bndfile.isFile()) {
			return null;
		}

		// We do not use a parent because then we would
		// build ourselves again. So we can not blindly
		// inherit the properties.
		Builder bchild = builder.getSubBuilder();
		builder.addClose(bchild);

		bchild.removeBundleSpecificHeaders();

		// We must make sure that we do not include ourselves again!
		bchild.setProperty(Constants.INCLUDE_RESOURCE, "");
		bchild.setProperty(Constants.INCLUDERESOURCE, "");
		bchild.setProperties(bndfile, builder.getBase());

		Jar jar = bchild.build();
		builder.getInfo(bchild, bndfile.getName() + ": ");

		if (jar != null) {
			if (builder.hasSources()) {
				Jar dot = builder.getJar();
				if (dot != null) {
					for (String key : jar.getResources()
						.keySet()) {
						if (key.startsWith("OSGI-OPT/src/"))
							dot.putResource(key, jar.getResource(key));
					}
				}
			}

			return new JarResource(jar);
		} else {
			builder.error("Could not create make resource, args=%s", argumentsOnMake);
			return null;
		}
	}

}
