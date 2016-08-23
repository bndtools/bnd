---
layout: default
class: Header
title: Bundle-RequiredExecutionEnvironment ::= ee-name ( ',' ee-name )*
summary: The Bundle-RequiredExecutionEnvironment contains a comma-separated list of execution environ- ments that must be present on the OSGi framework. See Execution Environment on page 44. This header is deprecated. 
---
	
		verifyListHeader(Constants.BUNDLE_REQUIREDEXECUTIONENVIRONMENT, EENAME, false);
	
		final static Pattern	EENAME	= Pattern.compile("CDC-1\\.0/Foundation-1\\.0" + "|CDC-1\\.1/Foundation-1\\.1"
											+ "|OSGi/Minimum-1\\.[1-9]" + "|JRE-1\\.1" + "|J2SE-1\\.2" + "|J2SE-1\\.3"
											+ "|J2SE-1\\.4" + "|J2SE-1\\.5" + "|JavaSE-1\\.6" + "|JavaSE-1\\.7"
											+ "|JavaSE-1\\.8" + "|PersonalJava-1\\.1" + "|PersonalJava-1\\.2"
											+ "|CDC-1\\.0/PersonalBasis-1\\.0" + "|CDC-1\\.0/PersonalJava-1\\.0");
		final static EE[]			ees								= {
			new EE("CDC-1.0/Foundation-1.0", V1_3, V1_1),
			new EE("CDC-1.1/Foundation-1.1", V1_3, V1_2),
			new EE("OSGi/Minimum-1.0", V1_3, V1_1),
			new EE("OSGi/Minimum-1.1", V1_3, V1_2),
			new EE("JRE-1.1", V1_1, V1_1), //
			new EE("J2SE-1.2", V1_2, V1_1), //
			new EE("J2SE-1.3", V1_3, V1_1), //
			new EE("J2SE-1.4", V1_3, V1_2), //
			new EE("J2SE-1.5", V1_5, V1_5), //
			new EE("JavaSE-1.6", V1_6, V1_6), //
			new EE("PersonalJava-1.1", V1_1, V1_1), //
			new EE("JavaSE-1.7", V1_7, V1_7), //
			new EE("JavaSE-1.8", V1_8, V1_8), //
			new EE("PersonalJava-1.1", V1_1, V1_1), //
			new EE("PersonalJava-1.2", V1_1, V1_1), new EE("CDC-1.0/PersonalBasis-1.0", V1_3, V1_1),
			new EE("CDC-1.0/PersonalJava-1.0", V1_3, V1_1), new EE("CDC-1.1/PersonalBasis-1.1", V1_3, V1_2),
			new EE("CDC-1.1/PersonalJava-1.1", V1_3, V1_2)
																};
	