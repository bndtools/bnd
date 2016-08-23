---
layout: default
class: Project
title: -exportcontents PACKAGE-SPEC, ( ',' PACKAGE-SPEC )*
summary: Exports the given packages but does not try to include them from the class path. The packages should be loaded with alternative means. 
---
	
			//
			// EXPORTS
			//
			{
				Set<Instruction> unused = Create.set();

				Instructions filter = new Instructions(getExportPackage());
				filter.append(getExportContents());

				exports = filter(filter, contained, unused);

				if (!unused.isEmpty()) {
					warning("Unused " + Constants.EXPORT_PACKAGE + " instructions: %s ", unused);
				}

				// See what information we can find to augment the
				// exports. I.e. look on the classpath
				augmentExports(exports);
			}

			//
	