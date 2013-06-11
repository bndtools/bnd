
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.codehaus.plexus.util.ReaderFactory;
import org.codehaus.plexus.util.StringUtils;
import org.codehaus.plexus.util.xml.Xpp3Dom;
import org.codehaus.plexus.util.xml.Xpp3DomBuilder;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Display help information on bnd-maven-plugin.<br/>
 * Call <code>mvn bnd:help -Ddetail=true -Dgoal=&lt;goal-name&gt;</code> to display parameter details.
 * @author
 * @version
 * @goal help
 * @requiresProject false
 * @threadSafe
 */
public class HelpMojo
    extends AbstractMojo
{
    /**
     * If <code>true</code>, display all settable properties for each goal.
     *
     * @parameter property="detail" default-value="false"
     */
    //@Parameter( property = "detail", defaultValue = "false" )
    private boolean detail;

    /**
     * The name of the goal for which to show help. If unspecified, all goals will be displayed.
     *
     * @parameter property="goal"
     */
    //@Parameter( property = "goal" )
    private java.lang.String goal;

    /**
     * The maximum length of a display line, should be positive.
     *
     * @parameter property="lineLength" default-value="80"
     */
    //@Parameter( property = "lineLength", defaultValue = "80" )
    private int lineLength;

    /**
     * The number of spaces per indentation level, should be positive.
     *
     * @parameter property="indentSize" default-value="2"
     */
    //@Parameter( property = "indentSize", defaultValue = "2" )
    private int indentSize;

    // groupId/artifactId/plugin-help.xml
    private static final String PLUGIN_HELP_PATH = "/META-INF/maven/biz.aQute.bnd/bnd-maven-plugin/plugin-help.xml";

    private Xpp3Dom build()
        throws MojoExecutionException
    {
        getLog().debug( "load plugin-help.xml: " + PLUGIN_HELP_PATH );
        InputStream is = getClass().getResourceAsStream( PLUGIN_HELP_PATH );
        try
        {
            return Xpp3DomBuilder.build( ReaderFactory.newXmlReader( is ) );
        }
        catch ( XmlPullParserException e )
        {
            throw new MojoExecutionException( e.getMessage(), e );
        }
        catch ( IOException e )
        {
            throw new MojoExecutionException( e.getMessage(), e );
        }
    }

    /**
     * {@inheritDoc}
     */
    public void execute()
        throws MojoExecutionException
    {
        if ( lineLength <= 0 )
        {
            getLog().warn( "The parameter 'lineLength' should be positive, using '80' as default." );
            lineLength = 80;
        }
        if ( indentSize <= 0 )
        {
            getLog().warn( "The parameter 'indentSize' should be positive, using '2' as default." );
            indentSize = 2;
        }

        Xpp3Dom pluginElement = build();

        StringBuilder sb = new StringBuilder();
        String name = pluginElement.getChild( "name" ).getValue();
        String version = pluginElement.getChild( "version" ).getValue();
        String id = pluginElement.getChild( "groupId" ).getValue() + ":" + pluginElement.getChild( "artifactId" ).getValue()
                    + ":" + version;
        if ( StringUtils.isNotEmpty( name ) && !name.contains( id ) )
        {
            append( sb, name + " " + version, 0 );
        }
        else
        {
            if ( StringUtils.isNotEmpty( name ) )
            {
                append( sb, name, 0 );
            }
            else
            {
                append( sb, id, 0 );
            }
        }
        append( sb, pluginElement.getChild( "description" ).getValue(), 1 );
        append( sb, "", 0 );

        //<goalPrefix>plugin</goalPrefix>
        String goalPrefix = pluginElement.getChild( "goalPrefix" ).getValue();

        Xpp3Dom[] mojos = pluginElement.getChild( "mojos" ).getChildren( "mojo" );

        if ( goal == null || goal.length() <= 0 )
        {
            append( sb, "This plugin has " + mojos.length + ( mojos.length > 1 ? " goals:" : " goal:" ) , 0 );
            append( sb, "", 0 );
        }

        for ( Xpp3Dom mojo : mojos )
        {
            writeGoal( sb, goalPrefix, mojo );
        }

        if ( getLog().isInfoEnabled() )
        {
            getLog().info( sb.toString() );
        }
    }

    private String getValue( Xpp3Dom mojo, String child )
    {
        Xpp3Dom elt = mojo.getChild( child );
        return ( elt == null ) ? "" : elt.getValue();
    }

    private void writeGoal( StringBuilder sb, String goalPrefix, Xpp3Dom mojo )
    {
        String mojoGoal = mojo.getChild( "goal" ).getValue();
        Xpp3Dom configurationElement = mojo.getChild( "configuration" );

        if ( goal == null || goal.length() <= 0 || mojoGoal.equals( goal ) )
        {
            append( sb, goalPrefix + ":" + mojoGoal, 0 );
            Xpp3Dom deprecated = mojo.getChild( "deprecated" );
            if ( ( deprecated != null ) && StringUtils.isNotEmpty( deprecated.getValue() ) )
            {
                append( sb, "Deprecated. " + deprecated, 1 );
                if ( detail )
                {
                    append( sb, "", 0 );
                    append( sb, getValue( mojo, "description" ), 1 );
                }
            }
            else
            {
                append( sb, getValue( mojo, "description" ), 1 );
            }
            append( sb, "", 0 );

            if ( detail )
            {
                Xpp3Dom[] parameters = mojo.getChild( "parameters" ).getChildren( "parameter" );
                append( sb, "Available parameters:", 1 );
                append( sb, "", 0 );

                for ( Xpp3Dom parameter : parameters )
                {
                    writeParameter( sb, parameter, configurationElement );
                }
            }
        }
    }

    private void writeParameter( StringBuilder sb, Xpp3Dom parameter, Xpp3Dom configurationElement )
    {
        String parameterName = parameter.getChild( "name" ).getValue();
        String parameterDescription = parameter.getChild( "description" ).getValue();

        Xpp3Dom fieldConfigurationElement = configurationElement.getChild( parameterName );

        String parameterDefaultValue = "";
        if ( fieldConfigurationElement != null && fieldConfigurationElement.getValue() != null )
        {
            parameterDefaultValue = " (Default: " + fieldConfigurationElement.getAttribute( "default-value" ) + ")";
        }
        append( sb, parameterName + parameterDefaultValue, 2 );
        Xpp3Dom deprecated = parameter.getChild( "deprecated" );
        if ( ( deprecated != null ) && StringUtils.isNotEmpty( deprecated.getValue() ) )
        {
            append( sb, "Deprecated. " + deprecated.getValue(), 3 );
            append( sb, "", 0 );
        }
        append( sb, parameterDescription, 3 );
        if ( "true".equals( parameter.getChild( "required" ).getValue() ) )
        {
            append( sb, "Required: Yes", 3 );
        }
        Xpp3Dom expression = parameter.getChild( "expression" );
        if ( ( expression != null ) && StringUtils.isNotEmpty( expression.getValue() ) )
        {
            append( sb, "Expression: " + expression.getValue(), 3 );
        }

        append( sb, "", 0 );
    }

    /**
     * <p>Repeat a String <code>n</code> times to form a new string.</p>
     *
     * @param str    String to repeat
     * @param repeat number of times to repeat str
     * @return String with repeated String
     * @throws NegativeArraySizeException if <code>repeat < 0</code>
     * @throws NullPointerException       if str is <code>null</code>
     */
    private static String repeat( String str, int repeat )
    {
        StringBuilder buffer = new StringBuilder( repeat * str.length() );

        for ( int i = 0; i < repeat; i++ )
        {
            buffer.append( str );
        }

        return buffer.toString();
    }

    /**
     * Append a description to the buffer by respecting the indentSize and lineLength parameters.
     * <b>Note</b>: The last character is always a new line.
     *
     * @param sb          The buffer to append the description, not <code>null</code>.
     * @param description The description, not <code>null</code>.
     * @param indent      The base indentation level of each line, must not be negative.
     */
    private void append( StringBuilder sb, String description, int indent )
    {
        for ( String line : toLines( description, indent, indentSize, lineLength ) )
        {
            sb.append( line ).append( '\n' );
        }
    }

    /**
     * Splits the specified text into lines of convenient display length.
     *
     * @param text       The text to split into lines, must not be <code>null</code>.
     * @param indent     The base indentation level of each line, must not be negative.
     * @param indentSize The size of each indentation, must not be negative.
     * @param lineLength The length of the line, must not be negative.
     * @return The sequence of display lines, never <code>null</code>.
     * @throws NegativeArraySizeException if <code>indent < 0</code>
     */
    private static List<String> toLines( String text, int indent, int indentSize, int lineLength )
    {
        List<String> lines = new ArrayList<String>();

        String ind = repeat( "\t", indent );

        String[] plainLines = text.split( "(\r\n)|(\r)|(\n)" );

        for ( String plainLine : plainLines )
        {
            toLines( lines, ind + plainLine, indentSize, lineLength );
        }

        return lines;
    }

    /**
     * Adds the specified line to the output sequence, performing line wrapping if necessary.
     *
     * @param lines      The sequence of display lines, must not be <code>null</code>.
     * @param line       The line to add, must not be <code>null</code>.
     * @param indentSize The size of each indentation, must not be negative.
     * @param lineLength The length of the line, must not be negative.
     */
    private static void toLines( List<String> lines, String line, int indentSize, int lineLength )
    {
        int lineIndent = getIndentLevel( line );
        StringBuilder buf = new StringBuilder( 256 );

        String[] tokens = line.split( " +" );

        for ( String token : tokens )
        {
            if ( buf.length() > 0 )
            {
                if ( buf.length() + token.length() >= lineLength )
                {
                    lines.add( buf.toString() );
                    buf.setLength( 0 );
                    buf.append( repeat( " ", lineIndent * indentSize ) );
                }
                else
                {
                    buf.append( ' ' );
                }
            }

            for ( int j = 0; j < token.length(); j++ )
            {
                char c = token.charAt( j );
                if ( c == '\t' )
                {
                    buf.append( repeat( " ", indentSize - buf.length() % indentSize ) );
                }
                else if ( c == '\u00A0' )
                {
                    buf.append( ' ' );
                }
                else
                {
                    buf.append( c );
                }
            }
        }
        lines.add( buf.toString() );
    }

    /**
     * Gets the indentation level of the specified line.
     *
     * @param line The line whose indentation level should be retrieved, must not be <code>null</code>.
     * @return The indentation level of the line.
     */
    private static int getIndentLevel( String line )
    {
        int level = 0;
        for ( int i = 0; i < line.length() && line.charAt( i ) == '\t'; i++ )
        {
            level++;
        }
        for ( int i = level + 1; i <= level + 4 && i < line.length(); i++ )
        {
            if ( line.charAt( i ) == '\t' )
            {
                level++;
                break;
            }
        }
        return level;
    }
}
