//@formatter:off
package biz.aQute.bnd.reporter.codesnippet;

import biz.aQute.bnd.reporter.codesnippet.geneimport.MyClass;

public class FullOnMethod {

  /**
   * ${snippet id=myPrint,title=Test, description="test
   *
   * test."}
   */
  public void print() {
    final MyClass c = new MyClass();
    System.out.println(c.toString());
  }
}
