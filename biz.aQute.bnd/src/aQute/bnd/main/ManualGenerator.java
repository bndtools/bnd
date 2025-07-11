package aQute.bnd.main;

import static aQute.bnd.help.Syntax.normalizeFilename;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

import aQute.bnd.help.Syntax;
import aQute.lib.getopt.CommandLine;
import aQute.lib.getopt.Description;
import aQute.lib.getopt.Options;
import aQute.lib.markdown.MarkdownFormatter;

class ManualGenerator {

	public void generateSyntaxFiles(File base, Collection<Syntax> syntaxes) throws IOException {

		File headerFolder = new File(base, "_heads");
		headerFolder.mkdirs();
		File instrFolder = new File(base, "_instructions");
		instrFolder.mkdirs();


		for (Syntax sx : syntaxes) {
			if (sx.getHeader()
				.startsWith("-")) {
				// instruction
				String filename = normalizeFilename(sx.getHeader()
					.substring(1)) + ".md";
				writeSyntaxMD(sx, instrFolder, filename);
			} else {
				// header
				String filename = normalizeFilename(sx.getHeader()) + ".md";
				writeSyntaxMD(sx, headerFolder, filename);
			}
		}

	}

	public void generateCommandsFiles(File base, bnd bnd) throws IOException {
		File commandsFolder = new File(base, "_commands");
		commandsFolder.mkdirs();

		CommandLine cl = new CommandLine(bnd);
		Map<String, Method> commands = cl.getCommands(bnd);

		for (String command : commands.keySet()) {

			Method method = commands.get(command);

			File file = new File(commandsFolder, command + ".md");
			file.createNewFile();

			try (FileWriter fw = new FileWriter(file)) {
				MarkdownFormatter f = new MarkdownFormatter(fw);

				Map<String, String> overrideFrontmatter = new LinkedHashMap<>();
				String overideContent = null;

				File overrideFile = new File(commandsFolder, "_ext/" + file.getName());
				if (overrideFile.exists()) {
					String overide = Files.readString(overrideFile.toPath());
					overrideFrontmatter.putAll(parseFrontmatter(overide));
					overideContent = extractContentWithoutFrontmatter(overide);
				}

				f.format("---\n");
				f.format("layout: default\n");
				f.format("title: %s\n", overrideFrontmatter.getOrDefault("title", command));

				@SuppressWarnings("unchecked")
				Class<? extends Options> specification = (Class<? extends Options>) method.getParameterTypes()[0];
				Description descr = method.getAnnotation(Description.class);

				String summary = overrideFrontmatter.getOrDefault("summary", descr != null ? descr.value() : null);
				if (summary != null) {
					f.format("summary: |\n   %s\n", summary);
				}

				f.format(
					"note: AUTO-GENERATED FILE - DO NOT EDIT. You can add manual content via same filename in _ext sub-folder. ");
				f.format("\n---\n\n");

				cl.generateDocumentationForCommand(f, command, method, 0);

				if (overideContent != null) {
					f.format("<!-- Manual content from: ext/%s --><br /><br />%n", overrideFile.getName());
					fw.append(overideContent);
				}

				f.flush();
			}

		}
	}


	private void writeSyntaxMD(Syntax sx, File folder, String filename) throws IOException {

		File file = new File(folder, filename);
		file.createNewFile();

		try (FileWriter fw = new FileWriter(file)) {
			MarkdownFormatter f = new MarkdownFormatter(fw);

			Map<String, String> overrideFrontmatter = new LinkedHashMap<>();
			String overideContent = null;

			File overrideFile = new File(folder, "_ext/" + filename);
			if (overrideFile.exists()) {
				String overide = Files.readString(overrideFile.toPath());
				overrideFrontmatter.putAll(parseFrontmatter(overide));
				overideContent = extractContentWithoutFrontmatter(overide);
			}

			f.format("---\n");
			f.format("layout: default\n");
			f.format("title: %s\n", overrideFrontmatter.getOrDefault("title", sx.getHeader()));
			if (sx.getHeader()
				.startsWith("-")) {
				String classFM = overrideFrontmatter.getOrDefault("class", null);
				if (classFM != null) {
					f.format("class: %s\n", classFM);
				}
			} else {
				f.format("class: %s\n", overrideFrontmatter.getOrDefault("class", "Header"));
			}
			f.format("summary: |\n   %s\n", overrideFrontmatter.getOrDefault("summary", sx.getLead()));
			f.format(
				"note: AUTO-GENERATED FILE - DO NOT EDIT. You can add manual content via same filename in ext folder. ");
			f.format("\n---\n\n");


			printMarkdown(f, sx, "", 0);

			if (overideContent != null) {
				f.format("<!-- Manual content from: ext/%s --><br /><br />%n", overrideFile.getName());
				fw.append(overideContent);
			}

			f.flush();
		}
	}

	private static Map<String, String> parseFrontmatter(String markdown) {
		Map<String, String> result = new LinkedHashMap<>();
		String[] lines = markdown.split("\n");

		boolean inFrontmatter = false;
		for (String line : lines) {
			line = line.trim();
			if (line.equals("---")) {
				if (!inFrontmatter) {
					inFrontmatter = true;
				} else {
					break; // End of frontmatter
				}
				continue;
			}
			if (inFrontmatter && line.contains(":")) {
				String[] parts = line.split(":", 2);
				String key = parts[0].trim();
				String value = parts[1].trim()
					.replaceAll("^\"|\"$", ""); // remove optional quotes
				result.put(key, value);
			}
		}
		return result;
	}

	public static String extractContentWithoutFrontmatter(String markdown) {
		String[] lines = markdown.split("\n");
		StringBuilder content = new StringBuilder();

		boolean inFrontmatter = false;
		boolean frontmatterEnded = false;

		for (String line : lines) {
			if (line.trim()
				.equals("---")) {
				if (!inFrontmatter) {
					inFrontmatter = true;
				} else {
					frontmatterEnded = true;
				}
				continue;
			}
			if (inFrontmatter && !frontmatterEnded) {
				continue; // skip lines in frontmatter
			}
			content.append(line)
				.append("\n");
		}

		return content.toString();
	}

	private void printMarkdown(MarkdownFormatter f, Syntax sx, String indent, int level) {
		if (sx == null)
			return;

		if (sx.getExample() != null) {
			f.format("%s- Example: ", indent);
			f.inlineCode("%s", sx.getExample());
			f.endP();
		}
		if (sx.getValues() != null) {
			f.format("%s- Values: ", indent);
			f.inlineCode("%s", sx.getValues());
			f.endP();
		}

		if (sx.getPattern() != null) {
			f.format("%s- Pattern: ", indent);
			f.inlineCode("%s", sx.getPattern());
			f.endP();
		}
		if (sx.getChildren() != null && sx.getChildren().length > 0) {
			if(level == 0) {

				if (sx.getHeader()
					.startsWith("-")) {
					f.h(3 + level, "Directives");
				} else {
					f.h(3 + level, "Options");
				}
			}
			else {
				f.format("%s- Options: ", indent);
			}

			for (Syntax child : sx.getChildren()) {
				f.format("\n%s- `%s`", indent + "  ".repeat(level), child.getHeader());
				if(child.getLead() != null) {
					f.format(" %s", child.getLead());
				}
				f.format("\n");
				printMarkdown(f, child, indent + "  ".repeat(level + 1), level + 1);
			}
		}

	}

}
