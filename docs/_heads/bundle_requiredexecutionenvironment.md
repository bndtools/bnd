---
layout: default
title: Bundle-RequiredExecutionEnvironment ::= ee-name ( ',' ee-name )*
class: Header
summary: |
   The Bundle-RequiredExecutionEnvironment contains a comma-separated list of execution environ- ments that must be present on the OSGi framework. See Execution Environment on page 44. This header is deprecated.
note: AUTO-GENERATED FILE - DO NOT EDIT. You can add manual content via same filename in ext folder. 
---

- Example: `Bundle-RequiredExecutionEnvironment: CDC-1.0/Foundation-1.0`

- Values: `OSGi/Minimum-1.0,OSGi/Minimum-1.1,OSGi/Minimum-1.2,JRE-1.1,J2SE-1.2,J2SE-1.3,J2SE-1.4,J2SE-1.5,JavaSE-1.6,JavaSE-1.7,JavaSE/compact1-1.8,JavaSE/compact2-1.8,JavaSE/compact3-1.8,JavaSE-1.8,JavaSE-9,JavaSE-10,JavaSE-11,JavaSE-12,JavaSE-13,JavaSE-14,JavaSE-15,JavaSE-16,JavaSE-17,JavaSE-18,JavaSE-19,JavaSE-20,JavaSE-21,JavaSE-22,JavaSE-23,JavaSE-24,JavaSE-25,JavaSE-26,JavaSE-27,JavaSE-28,JavaSE-29,JavaSE-30,CDC-1.0/Foundation-1.0,CDC-1.1/Foundation-1.1,PersonalJava-1.1,PersonalJava-1.2,CDC-1.0/PersonalBasis-1.0,CDC-1.0/PersonalJava-1.0,CDC-1.1/PersonalBasis-1.1,CDC-1.1/PersonalJava-1.1`

- Pattern: `.*`

<!-- Manual content from: ext/bundle_requiredexecutionenvironment.md --><br /><br />
	
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
	
