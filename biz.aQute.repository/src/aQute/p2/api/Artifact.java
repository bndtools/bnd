package aQute.p2.api;

import java.net.URI;

import org.osgi.framework.Version;

import aQute.bnd.util.dto.DTO;

/**
 * <pre>
 * <?xml version='1.0' encoding='UTF-8'?> <?artifactRepository version='1.1.0'?>
 * <repository name='Bndtools'
 * type='org.eclipse.equinox.p2.artifact.repository.simpleRepository'
 * version='1'> <properties size='2'> <property name='p2.timestamp'
 * value='1463781466748'/> <property name='p2.compressed' value='true'/>
 * </properties> <mappings size='3'> <rule filter='(&amp;
 * (classifier=osgi.bundle))' output='${repoUrl}/plugins/${id}_${version}.jar'/>
 * <rule filter='(&amp; (classifier=binary))'
 * output='${repoUrl}/binary/${id}_${version}'/> <rule filter='(&amp;
 * (classifier=org.eclipse.update.feature))'
 * output='${repoUrl}/features/${id}_${version}.jar'/> </mappings> <artifacts
 * size='22'> <artifact classifier='osgi.bundle'
 * id='org.bndtools.versioncontrol.ignores.plugin.git'
 * version='3.3.0.201605202157'> <properties size='3'> <property
 * name='artifact.size' value='9356'/> <property name='download.size'
 * value='9356'/> <property name='download.md5' value='745f389
 * a49189112a785848ad466097b'/> </properties> </artifact><artifact
 * classifier='osgi.bundle'id='bndtools.release'version='3.3.0.201605202156'><properties
 * size='3'><property name='artifact.size'value='215951'/><property
 * name='download.size'value='215951'/><property
 * name='download.md5'value='13273e976245329
 * c548c9e90a2181be1'/></properties></artifact><artifact
 * classifier='osgi.bundle'id='org.bndtools.embeddedrepo'
 * version='3.3.0.201605202156'><properties size='3'><property
 * name='artifact.size'value='818966'/><property
 * name='download.size'value='818966'/><property
 * name='download.md5'value='7b1d100516b88bf17ab59d235c66f237'/></properties></artifact><artifact
 * classifier='osgi.bundle'id='biz.aQute.repository'version='3.3.0.201605201950-SNAPSHOT'><properties
 * size='3'><property name='artifact.size'value='1454263'/><property
 * name='download.size'value='1454263'/><property
 * name='download.md5'value='fbb8a441ef09d818cb0e2f6067566d90'/></properties></artifact><artifact
 * classifier='osgi.bundle'id='bndtools.repository.base'
 * version='3.3.0.201605202156'><properties size='3'><property
 * name='artifact.size'value='2894453'/><property
 * name='download.size'value='2894453'/><property
 * name='download.md5'value='c017373214582041
 * cea25a5b92e79a8e'/></properties></artifact><artifact
 * classifier='osgi.bundle'id='javax.xml.stream'version='1.0.1.v201004272200'><properties
 * size='3'><property name='artifact.size'value='39695'/><property
 * name='download.size'value='39695'/><property
 * name='download.md5'value='dfb3dc47c90f4273c2036aab23ee4fe3'/></properties></artifact><artifact
 * classifier='osgi.bundle'id='org.bndtools.headless.build.plugin.ant'
 * version='3.3.0.201605202156'><properties size='3'><property
 * name='artifact.size'value='5007039'/><property
 * name='download.size'value='5007039'/><property
 * name='download.md5'value='aac4ed6377a95c9863817748c36b7b52'/></properties></artifact><artifact
 * classifier='osgi.bundle'id='org.bndtools.templating'
 * version='3.3.0.201605202156'><properties size='3'><property
 * name='artifact.size'value='2491044'/><property
 * name='download.size'value='2491044'/><property
 * name='download.md5'value='9af09948806b4fe9e1a3109d05e5a016'/></properties></artifact><artifact
 * classifier='osgi.bundle'id='org.bndtools.headless.build.manager'
 * version='3.3.0.201605202156'><properties size='3'><property
 * name='artifact.size'value='10713'/><property
 * name='download.size'value='10713'/><property
 * name='download.md5'value='7bd5d3b983c82b80322fec9d903f6f9c'/></properties></artifact><artifact
 * classifier='osgi.bundle'id='javax.xml'version='1.3.4.v201005080400'><properties
 * size='3'><property name='artifact.size'value='237996'/><property
 * name='download.size'value='237996'/><property
 * name='download.md5'value='9b2ff81e7d81cb418890070e0133fb5f'/></properties></artifact><artifact
 * classifier='osgi.bundle'id='biz.aQute.resolve'version='3.3.0.201605201950-SNAPSHOT'><properties
 * size='3'><property name='artifact.size'value='372900'/><property
 * name='download.size'value='372900'/><property
 * name='download.md5'value='543393716
 * b90a0d4be614f56e297ce08'/></properties></artifact><artifact
 * classifier='osgi.bundle'id='biz.aQute.bndlib'version='3.3.0.201605201949-SNAPSHOT'><properties
 * size='3'><property name='artifact.size'value='2374549'/><property
 * name='download.size'value='2374549'/><property
 * name='download.md5'value='dbbc5bbfb46576f33b69f4fb24b457d7'/></properties></artifact><artifact
 * classifier='osgi.bundle'id='bndtools.jareditor'version='3.3.0.201605202156'><properties
 * size='3'><property name='artifact.size'value='128844'/><property
 * name='download.size'value='128844'/><property
 * name='download.md5'value='dcb796f85340dce5193905e7cf3ffc63'/></properties></artifact><artifact
 * classifier='osgi.bundle'id='bndtools.api'version='3.3.0.201605202156'><properties
 * size='3'><property name='artifact.size'value='17758'/><property
 * name='download.size'value='17758'/><property
 * name='download.md5'value='ba2cdd0528fcb706350365f95ecfcfcd'/></properties></artifact><artifact
 * classifier='osgi.bundle'id='bndtools.core'version='3.3.0.201605202156'><properties
 * size='3'><property name='artifact.size'value='2421567'/><property
 * name='download.size'value='2421567'/><property
 * name='download.md5'value='1c65e37f4099dd09888b2040b0b3a380'/></properties></artifact><artifact
 * classifier='osgi.bundle'id='org.osgi.impl.bundle.repoindex.lib'
 * version='3.3.0.201605201950-SNAPSHOT'><properties size='3'><property
 * name='artifact.size'value='363001'/><property
 * name='download.size'value='363001'/><property
 * name='download.md5'value='133756
 * cf59beb03435f2a3573f02e1c3'/></properties></artifact><artifact
 * classifier='osgi.bundle'id='org.slf4j.api'version='1.7.2.v20121108-1250'><properties
 * size='3'><property name='artifact.size'value='35173'/><property
 * name='download.size'value='35173'/><property
 * name='download.md5'value='bdb1735bfea0fb2876c8aee53489654d'/></properties></artifact><artifact
 * classifier='osgi.bundle'id='bndtools.builder'version='3.3.0.201605202156'><properties
 * size='3'><property name='artifact.size'value='390697'/><property
 * name='download.size'value='390697'/><property name='download.md5'value='94
 * a98fb2ab47ca434eadef92535b8c3a'/></properties></artifact><artifact
 * classifier='osgi.bundle'id='org.bndtools.versioncontrol.ignores.manager'
 * version='3.3.0.201605202156'><properties size='3'><property
 * name='artifact.size'value='14280'/><property
 * name='download.size'value='14280'/><property
 * name='download.md5'value='5c22b08d7d67c86f2684a8490a563e98'/></properties></artifact><artifact
 * classifier='osgi.bundle'id='org.bndtools.headless.build.plugin.gradle'
 * version='3.3.0.201605202156'><properties size='3'><property
 * name='artifact.size'value='42342'/><property
 * name='download.size'value='42342'/><property name='download.md5'value='9086
 * a5c04ecc7836827226becbf7dbd8'/></properties></artifact><artifact
 * classifier='org.eclipse.update.feature' id='bndtools.main.feature'
 * version='3.3.0.DEV-20160520-175744-g656f2e2'><properties size='4'><property
 * name='artifact.size'value='5037'/><property
 * name='download.size'value='5037'/><property name='download.md5'value='b586
 * c53360f431d9ae7dbc3717b32e53'/><property
 * name='download.contentType'value='application/zip'/></properties></artifact><artifact
 * classifier='osgi.bundle'id='org.bndtools.templating.gitrepo'
 * version='3.3.0.201605202156'><properties size='3'><property
 * name='artifact.size'value='1986239'/><property
 * name='download.size'value='1986239'/><property
 * name='download.md5'value='050920369714
 * ab621019f10b321c615a'/></properties></artifact></artifacts></repository>
 * </pre>
 */
public class Artifact extends DTO {

	@Deprecated
	public String		type;
	public Classifier	classifier;
	public URI			uri;
	public String		id;
	public Version		version;
	public String		md5;
	public long			download_size;

}
