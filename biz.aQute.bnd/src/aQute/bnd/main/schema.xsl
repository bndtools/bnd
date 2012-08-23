<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="1.0"
	xmlns:xsl="http://www.w3.org/1999/XSL/Transform">
	<xsl:output method="html" />
	<xsl:template match="/">
		<html>
			<head>
				<link rel="stylesheet"
					href="http://twitter.github.com/bootstrap/1.4.0/bootstrap.min.css" />
				<title>Package Overview</title>
			</head>
			<body>
				<div class="span16">
					<h1>Overview of Packages</h1>
					<h2>Included JARs</h2>
					<p>This page contains an overview of packages that were found in
						the following jars</p>
					<table>
						<tr>
							<th>Name</th>
							<th>Title</th>
							<th>JAR</th>
						</tr>
						<xsl:for-each select="//specification">
							<tr>
								<td>
									<a href="#{@id}">
										<xsl:value-of select="@name" />
										<xsl:value-of select="@version" />
									</a>
								</td>
								<td>
									<xsl:value-of select="@title" />
								</td>
								<td>
									<xsl:value-of select="@jar" />
								</td>
							</tr>
						</xsl:for-each>
					</table>
					<hr />

					<h2>Baselining</h2>
					<p>
						The following table contains the result of listing all found
						packages. If a package has a prior then it will be compared to
						this prior version. The version column will show the result:
						<table>
							<tr>
								<td>Δ</td>
								<td>Indicates there was a delta.</td>
							</tr>
							<tr>
								<td>Δ=</td>
								<td>Indicates that the package and version are the same</td>
							</tr>
							<tr>
								<td>
									<span class="label important">Δ!</span>
								</td>
								<td>There was a delta but a semantic version mismatch</td>
							</tr>
							<tr>
								<td>
									<span class="label warning">Δ=</span>
								</td>
								<td>There was no delta but the version differed</td>
							</tr>
						</table>
						The Δ is a link to the details of the baseline comparison.
					</p>
					<table>
						<thead>
							<tr>

								<th>Name</th>
								<th>Delta</th>
								<th>Version</th>
								<th>Specification</th>
								<th>Version</th>
								<th>JSR</th>
							</tr>
						</thead>
						<xsl:variable name="all" select="//package" />
						<xsl:for-each select="$all">
							<xsl:sort select="@name" />
							<xsl:sort select="@version" />
							<xsl:variable name="id" select="@spec" />
							<xsl:variable name="ref" select="//specification[@id=$id]" />

							<tr>
								<td>
									<xsl:choose>
										<xsl:when test="@name=preceding-sibling::node()/@name" />
										<xsl:otherwise>
											<xsl:value-of select="@name" />
										</xsl:otherwise>
									</xsl:choose>
								</td>
								<td>
									<xsl:if test="@delta">
										<a href="#{@delta}">
											<xsl:if
												test="normalize-space(//info[@id=current()/@delta]/@warning)">
												<xsl:attribute name="alt"><xsl:value-of
													select="//info[@id=current()/@delta]/@warning" /></xsl:attribute>
												<xsl:attribute name="class">label warning</xsl:attribute>
											</xsl:if>
											<xsl:if test="//info[@id=current()/@delta and @mismatch='true']">
												<xsl:attribute name="class">label important</xsl:attribute>
											</xsl:if>
											Δ
											<xsl:if test="//info[@id=current()/@delta and @equals]">
												=
											</xsl:if>
											<xsl:if test="//info[@id=current()/@delta and @mismatch='true']">
												!
											</xsl:if>
										</a>
									</xsl:if>
								</td>
								<td>
									<xsl:value-of select="@version" />
								</td>
								<td>
									<a href="#{$id}">
										<xsl:value-of select="$ref/@name" />
									</a>
								</td>
								<td>
									<xsl:value-of select="$ref/@version" />
								</td>
								<td>
									<a href="{$ref/@url}">
										<xsl:value-of select="$ref/@jsr" />
									</a>
								</td>
							</tr>
						</xsl:for-each>
					</table>
					<hr />
					<h2>Specification Releases</h2>
					<table>
						<thead>
							<tr>
								<th>Name</th>
								<th>Version</th>
								<th>JSR</th>
								<th>Description</th>
							</tr>
						</thead>
						<xsl:for-each select="//specification">
							<xsl:sort select="@name" />
							<xsl:sort select="@version" />
							<tr>
								<td>
									<a name="{@id}" />
									<xsl:value-of select="@name" />
								</td>
								<td>
									<xsl:value-of select="@version" />
								</td>
								<td>
									<a href="{@url}">
										<xsl:value-of select="@jsr" />
									</a>
								</td>
								<td>
									<b>
										<xsl:value-of select="@title" />
									</b>
									<br />
									<p>
										<xsl:value-of select="." />
									</p>

									<b>Packages</b>
									<br />
									<table class="100%">
										<xsl:for-each select="//package[@spec=current()/@id]">
											<tr>
												<td class="span3">
													<xsl:value-of select="@name" />
												</td>
												<td class="span3">
													<xsl:value-of select="@version" />
												</td>
												<td>
												</td>
											</tr>
										</xsl:for-each>
									</table>

									<xsl:variable name="imports"
										select="//package[current()/@id=@spec]/import" />
									<xsl:if test="$imports">

										<b>XRef</b>
										<br />
										<table class="100%">
											<xsl:for-each select="$imports">
												<tr>
													<td class="span3">
														<xsl:value-of select="@name" />
													</td>
													<td class="span3">
														<xsl:value-of select="@version" />
													</td>
													<td>
														<xsl:for-each select="//package[@name=current()/@name]">
															<xsl:if test="not(position()=1)">
																,
															</xsl:if>
															<a href="#{@spec}">
																<xsl:value-of
																	select="//specification[@id=current()/@spec]/@name" />
																-
																<xsl:value-of
																	select="//specification[@id=current()/@spec]/@version" />
															</a>
														</xsl:for-each>
													</td>
												</tr>
											</xsl:for-each>
										</table>
									</xsl:if>
								</td>
							</tr>
						</xsl:for-each>
					</table>
					<hr />
					<h2>Deltas</h2>
					<p>The following table details the delta. The version of the newer
						(left) package can be marked in different colors:
					</p>
					<table>
						<tr>
							<td>1.0.0</td>
							<td>Ok, no issues</td>
						</tr>
						<tr>
							<td><span class="label important">1.0.0</span></td>
							<td>Semantic version mismatch</td>
						</tr>
						<tr>
							<td>
								<span class="label important">Δ!</span>
							</td>
							<td>There was a delta but a semantic version mismatch</td>
						</tr>
						<tr>
							<td>
								<span class="label warning">Δ=</span>
							</td>
							<td>There was no delta but the version differed</td>
						</tr>
					</table>
					<br/>
					<table>
						<thead>
							<tr>
								<th>Package</th>
								<th>Newer</th>
								<th>Older</th>
								<th>Delta</th>
							</tr>
						</thead>
						<xsl:for-each select="//info">
							<xsl:sort select="@name" />
							<tr>
								<td>
									<a name="{@id}" />
									<xsl:value-of select="@name" />
								</td>
								<td>
									<span>
										<xsl:if test="@mismatch='true'">
											<xsl:attribute name="class">label important</xsl:attribute>
										</xsl:if>
										<xsl:value-of select="@newerVersion" />
									</span>
									<xsl:if test="not(@newerVersion=@suggestedVersion)">
										<br />
										<span class="label success">
											<xsl:value-of select="@suggestedVersion" />
										</span>
										<xsl:if
											test="@suggestedIfProviders and not(@suggestedIfProviders=@suggestedVersion)">
											<br />
											<span class="label success">
												<xsl:value-of select="@suggestedIfProviders" />
											</span>

											<ul>
												<xsl:for-each select="provider">
													<li>
														<xsl:value-of select="@provider" />
													</li>
												</xsl:for-each>
											</ul>
										</xsl:if>
									</xsl:if>
								</td>
								<td>
									<xsl:call-template name="specref">
										<xsl:with-param name="id" select="@newerSpec" />
									</xsl:call-template>
								</td>
								<td>
									<xsl:value-of select="@olderVersion" />
								</td>
								<td>
									<xsl:call-template name="specref">
										<xsl:with-param name="id" select="@olderSpec" />
									</xsl:call-template>
								</td>
								<td>
									<pre style="font-size:10px;">
										<xsl:value-of select="diff" />
									</pre>
									<pre>
										<xsl:for-each select="provider">
											<xsl:value-of select="@provider" />

										</xsl:for-each>
									</pre>
								</td>
							</tr>
						</xsl:for-each>
					</table>
				</div>
			</body>
		</html>
	</xsl:template>

	<xsl:template name="specref">
		<xsl:param name="id" />
		<xsl:variable name="ns" select="//specification[@id=$id]" />
		<a href="#{$id}">
			<xsl:value-of select="$ns/@name" />
			-
			<xsl:value-of select="$ns/@version" />
		</a>
	</xsl:template>
</xsl:stylesheet>

