//@formatter:off
package biz.aQute.bnd.reporter.codesnippet;

import biz.aQute.bnd.reporter.codesnippet.geneimport.MyClass;

/**
 *
 */
public class WithoutDeclarationMethod {

  /**
   * ${snippet includeDeclaration=false}
   */
  public void print() {
    // Comment
    final MyClass c = new MyClass();
    System.out.println(c.toString());
  }
}
