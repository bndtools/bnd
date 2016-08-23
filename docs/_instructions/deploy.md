---
layout: default
class: Project
title: -deploy   
summary: Deploy this project through Deploy plugins (MavenDeploy plugin). Needs work
---

NEEDS WORK?

	/**
	 * Deploy the current project to a repository
	 *
	 * @throws Exception
	 */
	public void deploy() throws Exception {
		Parameters deploy = new Parameters(getProperty(DEPLOY));
		if (deploy.isEmpty()) {
			warning("Deploying but %s is not set to any repo", DEPLOY);
			return;
		}
		File[] outputs = getBuildFiles();
		for (File output : outputs) {
			for (Deploy d : getPlugins(Deploy.class)) {
				trace("Deploying %s to: %s", output.getName(), d);
				try {
					if (d.deploy(this, output.getName(), new BufferedInputStream(new FileInputStream(output))))
						trace("deployed %s successfully to %s", output, d);
				}
				catch (Exception e) {
					msgs.Deploying(e);
				}
			}
		}
	}
