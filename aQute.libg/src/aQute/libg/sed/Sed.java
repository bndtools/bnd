package aQute.libg.sed;

import java.io.*;
import java.util.*;
import java.util.regex.*;

import aQute.lib.io.*;

public class Sed {
	final File					file;
	final Replacer				macro;
	File						output;
	boolean						backup			= true;

	final Map<Pattern,String>	replacements	= new LinkedHashMap<Pattern,String>();

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
		BufferedReader brdr = new BufferedReader(new InputStreamReader(new FileInputStream(file), "UTF-8"));
		File out;
		if (output != null)
			out = output;
		else
			out = new File(file.getAbsolutePath() + ".tmp");
		PrintWriter pw = new PrintWriter(new OutputStreamWriter(new FileOutputStream(out), "UTF-8"));
		try {
			String line;
			while ((line = brdr.readLine()) != null) {
				for (Pattern p : replacements.keySet()) {
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
				}
				pw.println(line);
			}
        } finally {
        	brdr.close();
			pw.close();
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
