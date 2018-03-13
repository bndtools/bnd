package aQute.libg.sed;

import static java.nio.charset.StandardCharsets.UTF_8;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import aQute.lib.io.IO;

public class Sed {
	final File					file;
	final Replacer				macro;
	File						output;
	boolean						backup			= true;

	final Map<Pattern, String>	replacements	= new LinkedHashMap<>();

	public Sed(Replacer macro, File file) {
		assert file.isFile();
		this.file = file;
		this.macro = macro;
	}

	public Sed(File file) {
		assert file.isFile();
		this.file = file;
		this.macro = null;
	}

	public void setOutput(File f) {
		output = f;
	}

	public void replace(String pattern, String replacement) {
		replacements.put(Pattern.compile(pattern), replacement);
	}

	public int doIt() throws IOException {
		int actions = 0;
		File out;
		if (output != null)
			out = output;
		else
			out = new File(file.getAbsolutePath() + ".tmp");
		try (BufferedReader brdr = IO.reader(file, UTF_8); //
			PrintWriter pw = IO.writer(out, UTF_8)) {
			String line;
			while ((line = brdr.readLine()) != null) {
				for (Pattern p : replacements.keySet()) {
					try {
						String replace = replacements.get(p);
						Matcher m = p.matcher(line);

						StringBuffer sb = new StringBuffer();
						while (m.find()) {
							String tmp = setReferences(m, replace);
							if (macro != null)
								tmp = Matcher.quoteReplacement(macro.process(tmp));
							m.appendReplacement(sb, tmp);
							actions++;
						}
						m.appendTail(sb);

						line = sb.toString();
					} catch (Exception e) {
						throw new IOException("where: " + line + ", pattern: " + p.pattern() + ": " + e, e);
					}
				}
				pw.print(line);
				pw.print('\n');
			}
		}

		if (output == null) {
			if (backup) {
				File bak = new File(file.getAbsolutePath() + ".bak");
				IO.rename(file, bak);
			}
			IO.rename(out, file);
		}

		return actions;
	}

	private String setReferences(Matcher m, String replace) {
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < replace.length(); i++) {
			char c = replace.charAt(i);
			if (c == '$' && i < replace.length() - 1 && Character.isDigit(replace.charAt(i + 1))) {
				int n = replace.charAt(i + 1) - '0';
				if (n <= m.groupCount())
					sb.append(m.group(n));
				i++;
			} else
				sb.append(c);
		}
		return sb.toString();
	}

	public void setBackup(boolean b) {
		this.backup = b;
	}
}
