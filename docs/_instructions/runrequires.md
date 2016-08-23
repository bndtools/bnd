---
layout: default
class: Resolve
title: -runrequires REQUIREMENT ( ',' REQUIREMENT )* 
summary: The root requirements for a resolve intended to create a constellation for the -runbundles.
---

	private void constructInputRequirements() {
		Parameters inputRequirements = new Parameters(properties.mergeProperties(Constants.RUNREQUIRES));
		if (inputRequirements == null || inputRequirements.isEmpty()) {
			inputResource = null;
		} else {
			List<Requirement> requires = CapReqBuilder.getRequirementsFrom(inputRequirements);

			ResourceBuilder resBuilder = new ResourceBuilder();
			CapReqBuilder identity = new CapReqBuilder(IdentityNamespace.IDENTITY_NAMESPACE).addAttribute(
					IdentityNamespace.IDENTITY_NAMESPACE, IDENTITY_INITIAL_RESOURCE);
			resBuilder.addCapability(identity);

			for (Requirement req : requires) {
				resBuilder.addRequirement(req);
			}

			inputResource = resBuilder.build();
		}
	}
