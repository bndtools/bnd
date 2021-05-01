// javac --enable-preview --release 16 Expr.java 

public sealed interface Expr
    permits ConstantExpr, PlusExpr, TimesExpr, NegExpr, OtherExpr, SubExpr {}

record ConstantExpr(int i)       implements Expr {}
record PlusExpr(Expr a, Expr b)  implements Expr {}
record TimesExpr(Expr a, Expr b) implements Expr {}
record NegExpr(Expr e)           implements Expr {}

non-sealed class OtherExpr       implements Expr {}

sealed class SubExpr             implements Expr
    permits SubExpr1, SubExpr2 {}

final class SubExpr1 extends SubExpr {}
final class SubExpr2 extends SubExpr {}
