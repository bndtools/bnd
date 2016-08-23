---
order: 140
title: Best practices
layout: default
---

## Workspaces

The idea of a workspace is that it is cohesive. It contains a set of shared bundles and some of those bundles are exported to a repository. Other bundles are imported from repositories. This is the basic model of modularity.

This strongly implies that you should NOT share cnf between workspace, just like you should not share private fields from classes nor private classes from packages. So the idea is that your organizations has a number of workspaces that contain the code that has a strong relation. For example a product, your company service APIs, or shared base libraries. Since all these projects are in one workspace you get a lot of ease of use like refactoring and immediate feedback.

That said, there is often a desire to have some of the information, like the repositories in a shared place because you do not want to maintain it multiple times. Since bnd is completely based on inherited properties this is not that hard. The top of the properties are coming from the cnf/build.bnd file. Since this is a bnd file, you can actually include another bnd file there. Since this can be included via a URL, you can refer it to a file on your git repository. For convenience, you could make it refer to master but a better way is to make it refer to an actual commit. The reason is that if you checkout your project 5 years from now, it is unlikely that your build will be ok with the latest version of the included bnd file.

	build.bnd:

	-include: https://examplegit.com/foo/bar/master/shared/shared.bnd

A more defined way to handle this long term versioning problem is to use git modules. They have  a bad name but as far as I understand their bad name is because people don’t like that they are not automatically updated, which is exactly what you want when you want to build your project 5 years from now. Git modules require an explicit command to upgrade it to the latest or another version. The parent git repository stores the commit at which it is linked. With git modules you could make a subdirectory in cnf and then include a shared file from there:

	cnf
		shared/
			.git/
			shared.bnd
		build.bnd

	build.bnd:
	-include shared/shared.bnd

So to summarize, share workspaces, not projects. Use continuous integration that publishes your bundles to a repository that is shared with all. Share one bnd file with a git module.

