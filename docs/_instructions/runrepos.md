---
layout: default
class: Resolve
title: -runrepos REPO-NAME ( ',' REPO-NAME )* 
summary:  Order and select the repository for resolving against. The default order is all repositories in their plugin creation order.
---

	private void loadRepositories() throws IOException {
		// Get all of the repositories from the plugin registry
		List<Repository> allRepos = registry.getPlugins(Repository.class);

		// Workspace ws = registry.getPlugin(Workspace.class);
		// if (ws != null) {
		// for (InfoRepository ir : registry.getPlugins(InfoRepository.class)) {
		// allRepos.add(new InfoRepositoryWrapper(ir, ws.getCache("ir-" +
		// ir.getName())));
		// }
		// }


		// Reorder/filter if specified by the run model

		String rn = properties.mergeProperties(Constants.RUNREPOS);
		if (rn == null) {
			// No filter, use all
			for (Repository repo : allRepos) {
				super.addRepository(repo);
			}
		} else {
			Parameters repoNames = new Parameters(rn);

			// Map the repository names...
			Map<String,Repository> repoNameMap = new HashMap<String,Repository>(allRepos.size());
			for (Repository repo : allRepos)
				repoNameMap.put(repo.toString(), repo);

			// Create the result list
			for (String repoName : repoNames.keySet()) {
				Repository repo = repoNameMap.get(repoName);
				if (repo != null)
					super.addRepository(repo);
			}
		}
	}
