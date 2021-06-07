---
layout: default
title: repodigests ( ';' NAME )*
summary: Get the repository digests (describing their contents) for all or the specified names 
class: Workspace
layout: default
---

	public Object _repodigests(String[] args) throws Exception {
		Macro.verifyCommand(args, "${repodigests;[;<repo names>]...}, get the repository digests", null, 1, 10000);
		List<RepositoryPlugin> repos = getRepositories();
		if (args.length > 1) {
			repos: for (Iterator<RepositoryPlugin> it = repos.iterator(); it.hasNext();) {
				String name = it.next().getName();
				for (int i = 1; i < args.length; i++) {
					if (name.equals(args[i])) {
						it.remove();
						continue repos;
					}
				}
				it.remove();
			}
		}
		List<String> digests = new ArrayList<String>();
		for (RepositoryPlugin repo : repos) {
			try {
				// TODO use RepositoryDigest interface when it is widely
				// implemented
				Method m = repo.getClass().getMethod("getDigest");
				byte[] digest = (byte[]) m.invoke(repo);
				digests.add(Hex.toHexString(digest));
			}
			catch (Exception e) {
				if (args.length != 1)
					error("Specified repo %s for digests is not found", repo.getName());
				// else Ignore
			}
		}
		return join(digests, ",");
	}
