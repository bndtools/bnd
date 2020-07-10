// javac --enable-preview --release 15 Expr.java 

public sealed interface Expr
    permits ConstantExpr, PlusExpr, TimesExpr, NegExpr {}

 record ConstantExpr(int i)       implements Expr {}
 record PlusExpr(Expr a, Expr b)  implements Expr {}
 record TimesExpr(Expr a, Expr b) implements Expr {}
 record NegExpr(Expr e)           implements Expr {}
