---
layout: default
class: Builder
title: -nobuildincache BOOLEAN   
summary:  Do not use a build in cache for the launcher and JUnit. 
---

	@Override
	protected void setTypeSpecificPlugins(Set<Object> list) {
		try {
			super.setTypeSpecificPlugins(list);
			list.add(this);
			list.add(maven);
			list.add(settings);

			if (!isTrue(getProperty(NOBUILDINCACHE))) {
				list.add(new CachedFileRepo());
			}

			resourceRepositoryImpl = new ResourceRepositoryImpl();
			resourceRepositoryImpl.setCache(IO.getFile(getProperty(CACHEDIR, "~/.bnd/caches/shas")));
			resourceRepositoryImpl.setExecutor(getExecutor());
			resourceRepositoryImpl.setIndexFile(getFile(buildDir, "repo.json"));
			resourceRepositoryImpl.setURLConnector(new MultiURLConnectionHandler(this));
			customize(resourceRepositoryImpl, null);
			list.add(resourceRepositoryImpl);

		}
		catch (RuntimeException e) {
			throw e;
		}
		catch (Exception e) {
			throw new RuntimeException(e);
		}
	}
