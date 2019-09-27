//@formatter:off
package biz.aQute.bnd.reporter.codesnippet;

import biz.aQute.bnd.reporter.codesnippet.geneimport.MyClass;

public class WithoutImportMethod {

  /**
   * ${snippet includeImports=false}
   *
   * My Comment.
   */
  public void print() {
    final MyClass c = new MyClass();

    System.out.println(c.toString());
  }
}
