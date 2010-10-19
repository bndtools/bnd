<?xml version="1.0" encoding="UTF-8"?>
<xs:stylesheet xmlns:xs="http://www.w3.org/1999/XSL/Transform"
	xmlns='http://www.osgi.org/xmlns/scr/v1.1.0' version="1.0">

	<xs:output method="text" />

	<xs:template match="server">
		<xs:value-of select="id"/>	<xs:value-of select="username"/>
	</xs:template>

</xs:stylesheet>