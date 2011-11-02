<?xml version="1.0" encoding="UTF-8"?>
<xs:stylesheet xmlns:xs="http://www.w3.org/1999/XSL/Transform"
	xmlns='http://www.osgi.org/xmlns/scr/v1.1.0'
	version="1.0">
	
	<xs:output method="text"/>
	
	<xs:template match="/">
		<xs:apply-templates select="//component">
			<xs:sort order="ascending" select="@name"/>				
		</xs:apply-templates>
	</xs:template>
	
	<xs:template match="component">		Name                        <xs:value-of select="@name"/>
		Enabled                     <xs:value-of select="@enabled"/>
		Factory                     <xs:value-of select="@factory"/>
		Immediate                   <xs:value-of select="@imediate"/>
		Configuration Policy        <xs:value-of select="@configuration-policy"/>
		Activate                    <xs:value-of select="@activate"/>
		Modified                    <xs:value-of select="@modified"/>
		Deactivate                  <xs:value-of select="@deactivate"/>
		Implementation              <xs:value-of select="implementation/@class"/>
		
		<xs:apply-templates select="service"/>
		<xs:apply-templates select="reference"/>
		<xs:apply-templates select="properties"/>
		<xs:apply-templates select="property"/>
		
	</xs:template>
		
	<xs:template match="property">
		<xs:value-of select="@name"/>  <xs:value-of select="@type"/> <xs:value-of select="@value"/> 				
	</xs:template>
	
	<xs:template match="properties">
		Properties                  <xs:value-of select="@entry"/> 				
	</xs:template>
	
	<xs:template match="service">
		Service (servicefactory=<xs:value-of select="@servicefactory"/>)
		<xs:for-each select="provide">
		  <xs:value-of select="@interface"/> 				
		</xs:for-each>
	</xs:template>
	
	<xs:template match="reference">
		Reference <xs:value-of select="@name"/>
		  Interface                 <xs:value-of select="@interface"/>
		  Cardinality               <xs:value-of select="@cardinality"/>
		  Policy                    <xs:value-of select="@policy"/>
		  Target                    <xs:value-of select="@target"/>
		  Bind method               <xs:value-of select="@bind"/>
		  Unbind method             <xs:value-of select="@unbind"/>
	</xs:template>
</xs:stylesheet>