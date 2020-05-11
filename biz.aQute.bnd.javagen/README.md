# JavaGen

JavaGen is a simple example _external plugin_ for the [`-generate`](https://bnd.bndtools.org/instructions/generate.html) instruction. It takes a set of input files, preprocesses them, and places them in the output directory. Although this works for any syntax, the primary idea was to generate Java files using the bnd preprocessor, and the build properties as the template information domain.

The `javagen` command takes the following command line:

    javagen [-o dir] FILESET...
    
For example, the following line will read `.java` files from the `input/` directory and pre-process each file to the `src-gen/` directory.   

    javagen -o src-gen input/**.java

The output file will be placed in the directory, relative to `src-gen`, as specified in the package, even if the input file is in another directory. This is only done when the outout contains a `package` statement. The `src-gen` directory should be place on the `src` path for bnd, you can do this in the `bnd.bnd` file with:

    src = ${^src},src-gen

## Input Format

The input files must have the same name as the output file name. However, an input file can start with a _header_. A header is signalled with the `---` characters. The text between the `---` and next `---` (on its own line) is treated as properties in the bnd format. You can use the `-include` instruction to include other properties and handling of the `./` is similar. These properties inherit from the project (and thus `cnf/build.bnd`) and will override them. 

For example:

    ---
    foo=1
    -include foo.bnd
    ---
    package foo.bar;
    
    public class Foo {
        public static int foo = ${foo};
        
        public static String[] BUILDPATH = {
            ${template;-buildpath;"${@}"}
        };
    }


