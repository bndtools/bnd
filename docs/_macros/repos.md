---
layout: default
title: repos
summary: A list of the current repositories
class: Project
---


	public String _repos(@SuppressWarnings("unused")
	String args[]) throws Exception {
		List<RepositoryPlugin> repos = getPlugins(RepositoryPlugin.class);
		List<String> names = new ArrayList<String>();
		for (RepositoryPlugin rp : repos)
			names.add(rp.getName());
		return join(names, ", ");
	}
