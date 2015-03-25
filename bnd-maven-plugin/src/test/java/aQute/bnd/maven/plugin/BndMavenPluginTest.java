package aQute.bnd.maven.plugin;

import java.io.*;
import org.apache.maven.plugin.testing.AbstractMojoTestCase;

public class BndMavenPluginTest
    extends AbstractMojoTestCase
{
    /** {@inheritDoc} */
    protected void setUp()
        throws Exception
    {
        // required
        super.setUp();
    }

    /** {@inheritDoc} */
    protected void tearDown()
        throws Exception
    {
        // required
        super.tearDown();
    }

    /**
     * @throws Exception if any
     */
    public void testSomething()
        throws Exception
    {
        File pom = getTestFile( "src/test/resources/unit/test-api-bundle/pom.xml" );
        assertNotNull( pom );
        assertTrue( pom.exists() );

        BndMavenPlugin myMojo = (BndMavenPlugin) lookupMojo( "bnd-process", pom );
        assertNotNull( myMojo );
       // myMojo.execute();


    }
}
