<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
	version="1.0">
	<xsl:output method="html"/>
	
	<xsl:template match="/">
		<html>
			<xsl:apply-templates/>
		</html>
	</xsl:template>
	
	<xsl:template match="repository">
		<head>
			<META HTTP-EQUIV="Content-Type"
				CONTENT="text/html; charset=iso-8859-1"/>
			<title>
				<xsl:value-of select="@name"/>
			</title>
			<link href="https://osgi.org/www/testreport.css" type="text/css" rel="stylesheet"/>
		</head>
		<body>
			<h1>
				<xsl:value-of select="@name"/>
			</h1>
		<p>Last modified 	
				<xsl:value-of select="@lastmodified"/>.</p>

			<table>
				<tr><th width="200px">Link</th><th>Version</th><th>doc/src</th><th>Description</th><th>Bytes</th></tr>
				<xsl:apply-templates select="resource">
					<xsl:sort select="@presentationname"/>
				</xsl:apply-templates>
			</table>
		</body>
	</xsl:template>
	
	<xsl:template match="resource">
		<tr>
			<td>
				<a href="{normalize-space(@uri)}"><xsl:value-of select="@presentationname"/></a>
				
			</td>
			<td><xsl:value-of select="@version"/></td>
			<td>
					<xsl:if test="documentation">
						<a href="{normalize-space(documentation)}">D</a>
					</xsl:if>
					<xsl:if test="source">
						<a href="{normalize-space(source)}">S</a>
					</xsl:if>
			</td>
			<td>
				<xsl:value-of select="description"/>
			</td>
			<td>
					<xsl:value-of select="size"/>
			</td>
		</tr>
		
	</xsl:template>
	
	<!--
	<xsl:template match="*">
	<tr>
	<td><xsl:value-of select="name()"/></td>
	<td><xsl:value-of select="."/></td>
	</tr>
	</xsl:template>
	-->
	<!--
	<xsl:template match="*">
	</xsl:template>
	-->
	
</xsl:stylesheet>
