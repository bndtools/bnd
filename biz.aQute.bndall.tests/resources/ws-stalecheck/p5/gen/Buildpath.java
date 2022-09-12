package foo.bar;


public class Buildpath {
	public final static String VERSION = "${def;Bundle-Version;1.0.0}"; 
    public final static String[] BUILDPATH = {
        ${template;-buildpath;"${@}"}
    };
}