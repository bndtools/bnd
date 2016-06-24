package aQute.bnd.make;

import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.regex.Matcher;

import aQute.bnd.header.Attrs;
import aQute.bnd.header.Parameters;
import aQute.bnd.osgi.Builder;
import aQute.bnd.osgi.Instruction;
import aQute.bnd.osgi.Processor;
import aQute.bnd.osgi.Resource;
import aQute.bnd.service.MakePlugin;

public class Make {
	Builder								builder;
	Map<Instruction,Map<String,String>>	make;

	public Make(Builder builder) {
		this.builder = builder;
	}

	public Resource process(String source) {
		Map<Instruction,Map<String,String>> make = getMakeHeader();
		builder.trace("make %s", source);

		for (Map.Entry<Instruction,Map<String,String>> entry : make.entrySet()) {
			Instruction instr = entry.getKey();
			Matcher m = instr.getMatcher(source);
			if (m.matches() || instr.isNegated()) {
				Map<String,String> arguments = replace(m, entry.getValue());
				List<MakePlugin> plugins = builder.getPlugins(MakePlugin.class);
				for (MakePlugin plugin : plugins) {
					try {
						Resource resource = plugin.make(builder, source, arguments);
						if (resource != null) {
							builder.trace("Made %s from args %s with %s", source, arguments, plugin);
							return resource;
						}
					} catch (Exception e) {
						builder.error("Plugin %s generates error when use in making %s with args %s", e, plugin, source,
								arguments);
					}
				}
			}
		}
		return null;
	}

	private Map<String,String> replace(Matcher m, Map<String,String> value) {
		Map<String,String> newArgs = Processor.newMap();
		for (Map.Entry<String,String> entry : value.entrySet()) {
			String s = entry.getValue();
			s = replace(m, s);
			newArgs.put(entry.getKey(), s);
		}
		return newArgs;
	}

	String replace(Matcher m, CharSequence s) {
		StringBuilder sb = new StringBuilder();
		int max = '0' + m.groupCount() + 1;
		for (int i = 0; i < s.length(); i++) {
			char c = s.charAt(i);
			if (c == '$' && i < s.length() - 1) {
				c = s.charAt(++i);
				if (c >= '0' && c <= max) {
					int index = c - '0';
					String replacement = m.group(index);
					if (replacement != null)
						sb.append(replacement);
				} else {
					if (c == '$')
						i++;
					sb.append(c);
				}
			} else
				sb.append(c);
		}
		return sb.toString();
	}

	Map<Instruction,Map<String,String>> getMakeHeader() {
		if (make != null)
			return make;
		make = Processor.newMap();

		Parameters make = builder.getMergedParameters(Builder.MAKE);

		for (Entry<String,Attrs> entry : make.entrySet()) {
			String pattern = Processor.removeDuplicateMarker(entry.getKey());

			Instruction instr = new Instruction(pattern);
			this.make.put(instr, entry.getValue());
		}

		return this.make;
	}
}
