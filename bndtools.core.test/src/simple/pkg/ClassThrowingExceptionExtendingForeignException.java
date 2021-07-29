package simple.pkg;

public class ClassThrowingExceptionExtendingForeignException {

	public ClassThrowingExceptionExtendingForeignException()
		throws ExceptionIndirectlyExtendingForeignException {}

	public void test() throws ExceptionIndirectlyExtendingForeignException {}
}
