---
layout: default
title: bash
summary: |
   Generate autocompletion file for bash
note: AUTO-GENERATED FILE - DO NOT EDIT. You can add manual content via same filename in _ext sub-folder. 
---

### Synopsis: 
	   bash  ...


<!-- Manual content from: ext/bash.md --><br /><br />

## Examples

	@Description("Generate autocompletion file for bash")
	public void _bash(Options options) throws Exception {
		File tmp = File.createTempFile("bnd-completion", ".tmp");
		tmp.deleteOnExit();

		try {
			IO.copy(getClass().getResource("bnd-completion.bash"), tmp);

			Sed sed = new Sed(tmp);
			sed.setBackup(false);

			Reporter r = new ReporterAdapter();
			CommandLine c = new CommandLine(r);
			Map<String,Method> commands = c.getCommands(this);
			StringBuilder sb = new StringBuilder();
			for (String commandName : commands.keySet()) {
				sb.append(" " + commandName);
			}
			sb.append(" help");

			sed.replace("%listCommands%", sb.toString().substring(1));
			sed.doIt();
			IO.copy(tmp, out);
		}
		finally {
			tmp.delete();
		}
	}
