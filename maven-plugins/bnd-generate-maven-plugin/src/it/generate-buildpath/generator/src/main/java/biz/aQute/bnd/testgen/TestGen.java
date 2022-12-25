package biz.aQute.bnd.testgen;

import java.io.File;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import aQute.bnd.build.Container;
import aQute.bnd.service.externalplugin.ExternalPlugin;
import aQute.bnd.service.generate.BuildContext;
import aQute.bnd.service.generate.Generator;
import aQute.lib.io.IO;

@ExternalPlugin(name = "testgen", objectClass = Generator.class)
public class TestGen implements Generator<TestGenOptions> {

	public Optional<String> generate(BuildContext context, TestGenOptions options) throws Exception {
		File output = options.output().orElse(context.getFile("gen-src"));
		output.mkdirs();
		if (!output.isDirectory())
			return Optional.of("no output directory " + output);

		System.out.println("Project " + context.getProject());
		
		String testMode = context.get("testMode", "default");
		
		System.out.println("mode " + testMode);
		
		Collection<Container> buildpath = context.getProject().getBuildpath();
		System.out.println("buildpath " + buildpath.size());
		List<String> bsns = buildpath.stream().map(c -> c.getBundleSymbolicName()).collect(Collectors.toList());
		bsns.forEach(s -> System.out.println("buildpath bsn : " + s));
		
		Collection<Container> testpath = context.getProject().getTestpath();
		if("testPath".equals(testMode)) {
			System.out.println("testpath " + testpath.size());
			if(testpath.isEmpty()) {
				context.error("Testpath should not have been empty");
			}
			List<String> testBsns = testpath.stream().map(c -> c.getBundleSymbolicName()).collect(Collectors.toList());
			testBsns.forEach(s -> System.out.println("testpath bsn : " + s));
		} else if(!testpath.isEmpty()) {
			context.error("Testpath should have been empty");
		}
		
		File file = null;
		if(context.getProject().getErrors().isEmpty()) {
			file = new File(output, "ok.txt");
		} else {
			file = new File(output, "failed.txt");
			bsns = new ArrayList<>(bsns);
			bsns.addAll(context.getProject().getErrors());
		}
		
		System.out.println("Writing to file " + file);
		FileOutputStream fos =  new FileOutputStream(file);
		IO.write(String.join("\n", bsns).getBytes(), fos);
		fos.close();
		return Optional.empty();
	}

}
