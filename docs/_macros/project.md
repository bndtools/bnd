___
title: 	repo ';' BSN ( ';' VERSION ( ';' ('HIGHEST' | 'LOWEST' | 'EXACT )? )?
class: 	Project
summary: Provides the file path to a bundle in one of the repositories
---
layout: default-


	public String _repo(String args[]) throws Exception {
		if (args.length < 2) {
			msgs.RepoTooFewArguments(_repoHelp, args);
			return null;
		}

		String bsns = args[1];
		String version = null;
		Strategy strategy = Strategy.HIGHEST;

		if (args.length > 2) {
			version = args[2];
			if (args.length == 4) {
				if (args[3].equalsIgnoreCase("HIGHEST"))
					strategy = Strategy.HIGHEST;
				else if (args[3].equalsIgnoreCase("LOWEST"))
					strategy = Strategy.LOWEST;
				else if (args[3].equalsIgnoreCase("EXACT"))
					strategy = Strategy.EXACT;
				else
					msgs.InvalidStrategy(_repoHelp, args);
			}
		}

		Collection<String> parts = split(bsns);
		List<String> paths = new ArrayList<String>();

		for (String bsn : parts) {
			Container container = getBundle(bsn, version, strategy, null);
			if (container.getError() != null) {
				error("${repo} macro refers to an artifact %s-%s (%s) that has an error: %s", bsn, version, strategy,
						container.getError());
			} else
				add(paths, container);
		}
		return join(paths);
	}
	