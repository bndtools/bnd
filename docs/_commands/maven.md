---
layout: default
title:     maven ( 'settings' | 'bundle'
summary:  Special maven commands
---

## Description

{{page.summary}}

## Synopsis

	maven 
	[-temp <dir>]            use as temp directory
	settings                 show maven settings
	bundle                   turn a bundle into a maven bundle
		[-properties <file>]   provide properties, properties starting with javadoc are options for javadoc, like javadoc-tag=...
		[-javadoc <file|url>]  where to find the javadoc (zip/dir), otherwise generated
		[-source <file|url>]   where to find the source (zip/dir), otherwise from OSGI-OPT/src
		[-scm <url>]           required scm in pom, otherwise from Bundle-SCM
		[-url <url>]           required project url in pom
		[-bsn bsn]             overrides bsn
		[-version <version>]   overrides version
		[-developer <email>]   developer email
		[-nodelete]            do not delete temp files
		[-passphrase <gpgp passphrase>] signer password
			<file|url>

## Options

## Examples

	biz.aQute.bnd (master)$ bnd maven settings
	<?xml version="1.0" encoding="UTF-8"?>
	<settings xmlns="http://maven.apache.org/settings/1.0.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:schemaLocation="http://maven.apache.org/SETTINGS/1.0.0 http://maven.apache.org/xsd/settings-1.0.0.xsd">
		<!-- <localRepository>C:/Users/franklan/.m2/repository</localRepository> -->
		<pluginGroups>
			<!-- pluginGroup
			| Specifies a further group identifier to use for plugin lookup.
		<pluginGroup>com.your.plugins</pluginGroup>
		-->
		</pluginGroups>
		<servers>
			<server>
				<id>deploymentRepo</id>
				<username>deployment</username>
				<password>deployment123</password>
		<!-- <privateKey>c://config//Frank_Langel.p12</privateKey>    -->
			</server>
		</servers>
		<mirrors>
			<mirror>
			<id>nexus</id>
				<mirrorOf>*</mirrorOf>
				<url>https://svn.myfarm365.de/nexus/content/groups/public</url>
		<privateKey>/Users/aqute/Desktop/Peter_Kriens.p12</privateKey>
			</mirror>
		</mirrors>
		<profiles>
			<profile>
					<id>nexus</id>
					<!--Enable snapshots for the built in central repo to direct -->
					<!--all requests to nexus via the mirror -->
					<repositories>
						<repository>
							<id>central</id>
							<url>http://central</url> <!-- URL wird nicht verwendet, da mirror alles zu nexus weiterleitet -->
							<releases>
								<enabled>true</enabled>
								<updatePolicy>always</updatePolicy>
							</releases>
							<snapshots>
								<enabled>true</enabled>
								<updatePolicy>always</updatePolicy>
							</snapshots>
						</repository>
					</repositories>
					<pluginRepositories>
						<pluginRepository>
							<url>http://central</url> <!-- URL wird nicht verwendet, da mirror alles zu nexus weiterleitet -->
							<id>central</id>
							<releases>
								<enabled>true</enabled>
								<updatePolicy>always</updatePolicy>
							</releases>
							<snapshots>
								<enabled>true</enabled>
								<updatePolicy>always</updatePolicy>
							</snapshots>
						</pluginRepository>
					</pluginRepositories>
				</profile>

			</profiles>
			<activeProfiles>
					<activeProfile>nexus</activeProfile>
			</activeProfiles>
	</settings>