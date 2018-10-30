<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform">

	<xsl:output method="xml" />
	<xsl:param name="param1" />

	<xsl:template match="/">
		<xsl:if test="$param1='param'">
			<xsl:copy-of select="*" />
		</xsl:if>
	</xsl:template>

</xsl:stylesheet>