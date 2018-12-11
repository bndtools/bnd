<?xml version="1.0" encoding="UTF-8"?>
<!--
	This XSL stylesheet transforms the output of a testsuite to an HTML
	file.	
	-->
<xsl:stylesheet version="1.0"
	xmlns:xsl="http://www.w3.org/1999/XSL/Transform">
	<xsl:output method="xml" indent='yes' />

	<xsl:template match="/">
		<html>
			<head>
				<title>Test</title>
				<link rel='stylesheet' href='https://bnd.bndtools.org/css/testresport.css'
					type='text/css' />
				<style type="text/css">
					.code { font-family: Courier,
					sans-serif; }
					.error,.ok, .info,
					.warning {
					background-position: 4 4;
					background-repeat:no-repeat;
					width:10px; }
					.ok {
					background-image:url("https://bnd.bndtools.org/img/greenball.png"); }
					.warning {
					background-image:url("https://bnd.bndtools.org/img/orangeball.png"); }
					.error {
					background-image:url("https://bnd.bndtools.org/img/redball.png"); }
					.info {
					background-image:url("https://bnd.bndtools.org/img/info.png"); }
					.class { padding-top:20px; padding-bottom: 10px; font-size:12;
					font-weight:bold; }

					h2 { margin-top : 20px; margin-bottom:10px; }
					table, th, td { border: 1px solid black; padding:5px; }
					table {
					border-collapse:collapse; width:100%; margin-top:20px;}
					th { height:20px; }
					} 
				</style>
				<script language="javascript">
					function toggle(name) {
					var el =
					document.getElementById(name);
					if ( el.style.display != 'none' ) {
					el.style.display = 'none';
					}
					else {
					el.style.display = '';
					}
					}				
				</script>
			</head>
			<body style="width:800px">
				<h2>Summary</h2>
				<p>The following table provides a summary of the test information.</p>
				<table>
					<tr>
						<th>Property Key</th>
						<th>Property Value</th>
					</tr>
					<tr>
						<td width="50%">Target</td>
						<td>
							<xsl:value-of select="testsuite/@target" />
						</td>
					</tr>
					<tr>
						<td width="50%">Framework</td>
						<td>
							<xsl:value-of select="testsuite/@framework" />
						</td>
					</tr>
					<tr>
						<td width="50%">Testrun</td>
						<td>
							<xsl:value-of select="testsuite/@time" />
						</td>
					</tr>
				    <tr>
                        <th colspan="2">Bundles</th>
					</tr>
					<xsl:for-each select="//bundle">
						<tr>
							<td>
								<xsl:value-of select="@bsn" />
							</td><td>
								<xsl:value-of select="@version" />
							</td>
							
						</tr>
					</xsl:for-each>
				</table>
				<h2>Testcases</h2>
				<p>The following table shows the results of each test. A red icon indicates that the 
					test failed or had an error. A green icon
					indicates success. Any errors or failure messages 
					will be displayed as a list beneath the test name. To see the 
					exception, click on the info icon on the right.</p>
				<table width="100%">
					<tr>
						<th width="15px"><img src="https://bnd.bndtools.org/img/colorball.png" title="Status. red=bad, orange=almost good, green is perfect"/></th>
						<th>Test</th>
						<th>Failures</th>
						<th>Error</th>
						<th>Info</th>
					</tr>
					<xsl:for-each select="/testsuite/testcase">
						<xsl:variable name="total" select="count(error) + count(failure)" />
						<xsl:variable name="class" select="@classname" />
						<xsl:if test="not(preceding-sibling::*[@classname=$class])">
							<tr>
							    <th/>
								<th colspan="4"><xsl:value-of select="$class"/></th>
							</tr>
						</xsl:if>
						<tr>
							<td width="15px">
								<xsl:attribute name="class">
                                <xsl:choose>
                                    <xsl:when test="$total = 0">
                                        ok
                                    </xsl:when>
                                    <xsl:when test="$total &lt; 2">
                                        warning
                                    </xsl:when>
                                    <xsl:otherwise>
                                        error
                                    </xsl:otherwise>                                        
                                </xsl:choose>           
                                </xsl:attribute>
							</td>
							<td class="code">
								<xsl:value-of select="@name" />
								<xsl:if test="failure or error">
									<xsl:if test="failure[not(.)] or failure[not(.)]">
										<ul>
											<xsl:for-each select="failure[not(.)]">
												<li><xsl:value-of select="@message" /></li>
											</xsl:for-each>
											<xsl:for-each select="error[not(.)]">
												<li><xsl:value-of select="@message" /></li>
											</xsl:for-each>
										</ul>
									</xsl:if>
									<pre id="{@name}" style="display:none">
										<xsl:for-each select="failure">
											<div class="code">
												<xsl:value-of select="." />
											</div>
										</xsl:for-each>
										<xsl:for-each select="error">
											<div class="code">
												<xsl:value-of select="." />
											</div>
										</xsl:for-each>
									</pre>
								</xsl:if>
							</td>
							<td>
								<xsl:value-of select="count(failure)" />
							</td>
							<td>
								<xsl:value-of select="count(error)" />
							</td>
							<td>
								<xsl:if test="failure or error">
									<img src="https://bnd.bndtools.org/img/info.png" onclick="toggle('{@name}')" title="Show Exceptions"/>
								</xsl:if>
							</td>
						</tr>

					</xsl:for-each>
				</table>
				<br />

				<xsl:if test="//coverage">
					<h2>Coverage</h2>
					<p>The following table provides a sumary of the coverage based on static analysis.
					A red icon indicates the method is never referred. An orange icon indicates there is
					only one method referring to the method and a green icon indicates there are 2 or more
					references. The references are shown by clicking on the info icon. This table is based on static analysis so it is not possible to see
					how often the method is called and with what parameters.</p>
					<table width="100%">
						<xsl:for-each select="//coverage/class">
							<tr>
								<th width="15px"></th>
								<th>
									<xsl:value-of select="@name" />
								</th>
								<th> </th>
								<th> </th>
							</tr>
							<xsl:for-each select="method">
								<xsl:variable name="count" select="count(ref)" />
								<tr>
									<td width="15px">
										<xsl:attribute name="class">
											<xsl:choose>
												<xsl:when test="$count &gt; 0">
													ok
												</xsl:when>
												<xsl:otherwise>
													error
												</xsl:otherwise>                                        
											</xsl:choose>			
										</xsl:attribute>
									</td>
									<td class="code">
										<xsl:value-of select="@pretty" />
										<xsl:if test="ref">
											<div class='code' style="display:none;margin:4;padding:8; background-color: #FFFFCC;" id="{@pretty}" title="Callers">
												<xsl:for-each select="ref">
													<xsl:value-of select="@pretty" />
													<br />
												</xsl:for-each>
											</div>
										</xsl:if>
									</td>
									<td>
										<xsl:value-of select="count(ref)"/>
									</td>
									<td>
										<xsl:if test="ref">
											<img src="https://bnd.bndtools.org/img/info.png" onclick="toggle('{@pretty}')" title="Show/Hide Callers"/>
										</xsl:if>
									</td>
								</tr>
							</xsl:for-each>
						</xsl:for-each>
					</table>					
				</xsl:if>
			</body>
		</html>
	</xsl:template>

</xsl:stylesheet>