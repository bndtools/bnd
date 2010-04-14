<?xml version="1.0" encoding="UTF-8"?>

<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform" xmlns:beans="http://www.springframework.org/schema/beans" xmlns:aop="http://www.springframework.org/schema/aop" xmlns:context="http://www.springframework.org/schema/context" xmlns:jee="http://www.springframework.org/schema/jee" xmlns:jms="http://www.springframework.org/schema/jms" xmlns:lang="http://www.springframework.org/schema/lang" xmlns:osgi-compendium="http://www.springframework.org/schema/osgi-compendium" xmlns:osgi="http://www.springframework.org/schema/osgi" xmlns:tool="http://www.springframework.org/schema/tool" xmlns:tx="http://www.springframework.org/schema/tx" xmlns:util="http://www.springframework.org/schema/util" xmlns:webflow-config="http://www.springframework.org/schema/webflow-config">
	<xsl:output method="text" />

	<xsl:template match="/">

		<!-- Match all attributes that holds a class or a comma delimited 
		     list of classes and print them -->

		<xsl:for-each select="//provider|//class">
			<xsl:value-of select="." />
			<xsl:text>
			</xsl:text>
		</xsl:for-each>
	</xsl:template>


</xsl:stylesheet>

