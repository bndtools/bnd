//@formatter:off
package biz.aQute.bnd.reporter.codesnippet;

import biz.aQute.bnd.reporter.codesnippet.geneimport.MyClass;

/**
 * ${snippet id=myFullOnType,title=Test, description="test {}."}
 */
public abstract class FullOnType {

  String test = "test";

  public void print() {
    final MyClass c = new MyClass();

    System.out.println(c.toString());
  }
}
