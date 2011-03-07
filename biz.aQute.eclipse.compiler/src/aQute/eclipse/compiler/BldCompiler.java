package aQute.eclipse.compiler;




//public class BldCompiler implements INameEnvironment,
//		IErrorHandlingPolicy, ICompilerRequestor {
//	private static final ICompilationUnit[]	EMPTY_ICOMPILATION_UNITS	= new ICompilationUnit[0];
//	Project									project;
//	Jar										output;
//	Set<String>								packageCache				= new HashSet<String>();
//	List<Contribution>								buildpath;
//	
//	public BldCompiler(Project project) {
//		this.project = project;
//	}
//
//	public void compile(Jar output, Jar sources, List<Contribution> buildpath) throws Exception {
//		this.output = output;
//		this.buildpath = buildpath;
//		buildPackageCache(buildpath);
//
//		String encoding = project.getProperty("-encoding", "UTF-8");
//		Compiler compiler = new org.eclipse.jdt.internal.compiler.Compiler(
//				this, this, new CompilerOptions(project), this,
//				new DefaultProblemFactory(Locale.getDefault()));
//
//		List<ICompilationUnit> iunits = new ArrayList<ICompilationUnit>();
//
//		for (Map.Entry<String, Resource> entry : (Set<Map.Entry<String, Resource>>) sources
//				.getResources().entrySet()) {
//			CompilationUnit cu = new CompilationUnit(getContent(entry
//					.getValue(), encoding), entry.getKey(), encoding);
//			iunits.add(cu);
//		}
//		ICompilationUnit units[] = iunits.toArray(EMPTY_ICOMPILATION_UNITS);
//		compiler.compile(units);
//	}
//
//	private void buildPackageCache(List<Contribution> buildpath) throws Exception {
//		for (Contribution contribution : buildpath) {
//			packageCache.addAll( contribution.getJar().getDirectories().keySet());
//		}
//	}
//
//	private char[] getContent(Resource value, String encoding)
//			throws IOException {
//		InputStreamReader rdr = new InputStreamReader(value.openInputStream(),
//				encoding);
//		return read(rdr, 0);
//	}
//
//	private char[] read(InputStreamReader rdr, int offset) throws IOException {
//		char[] tmp = new char[4096];
//		int size = rdr.read(tmp);
//		if (size > 0) {
//			char[] buffer = read(rdr, offset + size);
//			System.arraycopy(tmp, 0, buffer, offset, size);
//			return buffer;
//		} else
//			return new char[offset];
//	}
//
//	public void cleanup() {
//	}
//
//	public NameEnvironmentAnswer findType(char[][] compoundTypeName) {
//		String path = getPath(null, compoundTypeName, ".class");
//		return find(path);
//	}
//
//	NameEnvironmentAnswer find(String path) {
//		try {
//			for (Contribution contribution : buildpath ) {
//				Resource resource = contribution.getJar().getResource(path);
//				if (resource != null) {
//					InputStream in = resource.openInputStream();
//					ClassFileReader cfr = ClassFileReader.read(in, path);
//					in.close();
//					return new NameEnvironmentAnswer(cfr, null);
//				}
//			}
//		} catch (Exception e) {
//			project.error("Can not process class file: " + path);
//		}
//		return null;
//	}
//
//	public NameEnvironmentAnswer findType(char[] typeName, char[][] packageName) {
//		String path = getPath(typeName, packageName, ".class");
//		return find(path);
//	}
//
//	String getPath(char[] typeName, char[][] packageName, String suffix) {
//		StringBuffer sb = new StringBuffer();
//		String del = "";
//		for (int i = 0; packageName != null && i < packageName.length; i++) {
//			sb.append(del);
//			sb.append(packageName[i]);
//			del = "/";
//		}
//		if (typeName!=null) {
//			sb.append(del);
//			sb.append(typeName);
//		}
//		sb.append(suffix);
//		return sb.toString();
//	}
//
//	String getPackagePath(char[] typeName, char[][] packageName) {
//		StringBuffer sb = new StringBuffer();
//		String del = "";
//		for (int i = 0; packageName != null && i < packageName.length; i++) {
//			sb.append(del);
//			sb.append(packageName[i]);
//			del = "/";
//		}
//		sb.append(del);
//		sb.append(typeName);
//		return sb.toString();
//	}
//
//	public boolean isPackage(char[][] parentPackageName, char[] packageName) {
//		String path = getPackagePath(packageName, parentPackageName);
//		boolean result = packageCache.contains(path);
//		return result;
//	}
//
//	public boolean proceedOnErrors() {
//		return true;
//	}
//
//	public boolean stopOnFirstError() {
//		return false;
//	}
//
//	public void acceptResult(CompilationResult result) {
// 		if ( !result.hasErrors() ) {
// 			ClassFile [] files = result.getClassFiles();
// 			for ( ClassFile cf : files ) {
// 				project.info("Compiled: " + getPath(null,cf.getCompoundName(),""));
// 				String path = getPath( null, cf.getCompoundName(), ".class");
// 				byte [] bytes = cf.getBytes();
// 				Resource r = new EmbeddedResource(bytes, System.currentTimeMillis());
// 				output.putResource(path, r);
// 			}
// 		} else {
// 			CategorizedProblem problems[] = result.getAllProblems();
// 			String fileName = null;
// 			for ( CategorizedProblem problem : problems ) {
//				StringBuilder sb = new StringBuilder();
// 				String fname = new String(problem.getOriginatingFileName());
// 				if ( !fname.equals(fileName)) {
// 					fileName = fname;
// 					sb.append(fname);
// 					sb.append(" ");
// 				}
//				sb.append( problem.getSourceLineNumber());
//				sb.append( ": ");
//				sb.append( problem.getMessage());
//				if ( problem.isError())
//					project.error( sb.toString());
//				else 
//					project.warning( sb.toString()); 				
// 			}
// 		}
//	}
//}
