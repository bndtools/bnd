<?xml version="1.0" encoding="UTF-8"?>

<xsl:stylesheet version="1.0" 
	xmlns:xsl="http://www.w3.org/1999/XSL/Transform" 
	xmlns:beans="http://www.springframework.org/schema/beans" 
	xmlns:aop="http://www.springframework.org/schema/aop" 
	xmlns:context="http://www.springframework.org/schema/context" 
	xmlns:jee="http://www.springframework.org/schema/jee" 
	xmlns:jms="http://www.springframework.org/schema/jms" 
	xmlns:lang="http://www.springframework.org/schema/lang" 
	xmlns:osgi-compendium="http://www.springframework.org/schema/osgi-compendium" 
	xmlns:osgi="http://www.springframework.org/schema/osgi" 
	xmlns:tool="http://www.springframework.org/schema/tool" 
	xmlns:tx="http://www.springframework.org/schema/tx" 
	xmlns:util="http://www.springframework.org/schema/util" 
	xmlns:webflow-config="http://www.springframework.org/schema/webflow-config" 
	xmlns:gemini-blueprint="http://www.eclipse.org/gemini/blueprint/schema/blueprint"
	xmlns:blueprint="http://www.osgi.org/xmlns/blueprint/v1.0.0">
	<xsl:output method="text" />

	<xsl:template match="/">

		<!-- Match all attributes that holds a class or a comma delimited 
		     list of classes and print them -->

		<xsl:for-each select="
				//beans:bean/@class 
			|	//beans:*/@value-type 
 			|	//aop:*/@implement-interface
			|	//aop:*/@default-impl
			|	//context:load-time-weaver/@weaver-class
			|	//jee:jndi-lookup/@expected-type
			|	//jee:jndi-lookup/@proxy-interface
			| 	//jee:remote-slsb/@ejbType
			|	//jee:*/@business-interface
			|	//lang:*/@script-interfaces
			|	//osgi:*/@interface
			|	//gemini-blueprint:*/@interface
			|   //blueprint:*/@interface
			|   //blueprint:*/@class
			|	//util:list/@list-class
			|	//util:set/@set-class
			|	//util:map/@map-class
			|	//webflow-config:*/@class
		">
			<xsl:value-of select="." />
			<xsl:text>
			</xsl:text>
		</xsl:for-each>

		<!-- This seems some magic to get extra imports? -->

		<xsl:for-each select="//beans:bean[@class='org.springframework.osgi.service.exporter.support.OsgiServiceFactoryBean'
				or @class='org.springframework.osgi.service.importer.support.OsgiServiceProxyFactoryBean']">
			<xsl:for-each select="beans:property[@name='interfaces']">
				<xsl:value-of select="@value" />
				<xsl:text>
				</xsl:text>
			</xsl:for-each>
		</xsl:for-each>

	</xsl:template>


</xsl:stylesheet>

