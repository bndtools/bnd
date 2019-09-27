//@formatter:off
package biz.aQute.bnd.reporter.codesnippet;

import biz.aQute.bnd.reporter.codesnippet.geneimport.MyClass;

/**
 * ${snippet includeDeclaration=false,includeImports=false}
 */
public abstract class OnlyContentType {

  String test = "test";

  public void print() {
    // Comment
    final MyClass c = new MyClass();

    System.out.println(c.toString());
  }
}
