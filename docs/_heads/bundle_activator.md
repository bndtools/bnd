---
layout: default
class: Header
title: Bundle-Activator CLASS
summary: The Bundle-Activator header specifies the name of the class used to start and stop the bundle
---
	
	private void verifyActivator() throws Exception {
		String bactivator = main.get(Constants.BUNDLE_ACTIVATOR);
		if (bactivator != null) {
			TypeRef ref = analyzer.getTypeRefFromFQN(bactivator);
			if (analyzer.getClassspace().containsKey(ref)) {
				Clazz activatorClazz = analyzer.getClassspace().get(ref);
				
				if (activatorClazz.isInterface()) {
					registerActivatorErrorLocation(error("The Bundle Activator " + bactivator + 
							" is an interface and therefore cannot be instantiated."),
							bactivator, ActivatorErrorType.IS_INTERFACE);
				} else {
					if(activatorClazz.isAbstract()) {
						registerActivatorErrorLocation(error("The Bundle Activator " + bactivator + 
								" is abstract and therefore cannot be instantiated."),
								bactivator, ActivatorErrorType.IS_ABSTRACT);
					}
					if(!activatorClazz.isPublic()) {
						registerActivatorErrorLocation(error("Bundle Activator classes must be public, and " + 
								bactivator + " is not."), bactivator, ActivatorErrorType.NOT_PUBLIC);
					}
					if(!activatorClazz.hasPublicNoArgsConstructor()) {
						registerActivatorErrorLocation(error("Bundle Activator classes must have a public zero-argument constructor and " + 
								bactivator + " does not."), bactivator, ActivatorErrorType.NO_SUITABLE_CONSTRUCTOR);
					}

					if (!activatorClazz.is(QUERY.IMPLEMENTS, 
							new Instruction("org.osgi.framework.BundleActivator"), analyzer)) {
						registerActivatorErrorLocation(error("The Bundle Activator " + bactivator + 
								" does not implement BundleActivator."), bactivator, ActivatorErrorType.NOT_AN_ACTIVATOR);
					}
				}
				return;
			}

			PackageRef packageRef = ref.getPackageRef();
			if (packageRef.isDefaultPackage())
				registerActivatorErrorLocation(error("The Bundle Activator is not in the bundle and it is in the default package "),
						bactivator, ActivatorErrorType.DEFAULT_PACKAGE);
			else if (!analyzer.isImported(packageRef)) {
				registerActivatorErrorLocation(error(Constants.BUNDLE_ACTIVATOR + 
						" not found on the bundle class path nor in imports: " + bactivator),
						bactivator, ActivatorErrorType.NOT_ACCESSIBLE);
			} else {
				registerActivatorErrorLocation(warning(Constants.BUNDLE_ACTIVATOR + " " + bactivator + 
						" is being imported into the bundle rather than being contained inside it. This is usually a bundle packaging error"),
						bactivator, ActivatorErrorType.IS_IMPORTED);
			}
		}
	}
	
	
	
	
				String s = getProperty(BUNDLE_ACTIVATOR);
			if (s != null) {
				activator = getTypeRefFromFQN(s);
				referTo(activator);
				trace("activator %s %s", s, activator);
			}
	