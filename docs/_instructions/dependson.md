---
layout: default
class: Project
title: -diffignore PACKAGE-SPEC ( ',' PACKAGE-SPEC ) *  
summary: Recursively add packages from the class path when referred and when they match one of the package specifications. 
---

					// We might have some other projects we want build
					// before we do anything, but these projects are not in
					// our path. The -dependson allows you to build them before.
					// The values are possibly negated globbing patterns.

					// dependencies.add( getWorkspace().getProject("cnf"));

					String dp = getProperty(Constants.DEPENDSON);
					Set<String> requiredProjectNames = new LinkedHashSet<String>(new Parameters(dp).keySet());

					// Allow DependencyConstributors to modify
					// requiredProjectNames
					List<DependencyContributor> dcs = getPlugins(DependencyContributor.class);
					for (DependencyContributor dc : dcs)
						dc.addDependencies(this, requiredProjectNames);

					Instructions is = new Instructions(requiredProjectNames);

					Set<Instruction> unused = new HashSet<Instruction>();
					Collection<Project> projects = getWorkspace().getAllProjects();
					Collection<Project> dependencies = is.select(projects, unused, false);

					for (Instruction u : unused)
						msgs.MissingDependson_(u.getInput());

