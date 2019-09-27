//@formatter:off
package biz.aQute.bnd.reporter.codesnippet;

import biz.aQute.bnd.reporter.codesnippet.geneimport.MyClass;

/**
 * ${snippet includeImports=false}
 */
public class WithoutImportType {

  String test = "test";

  public void print() {
    // Comment
    final MyClass c = new MyClass();

    System.out.println(c.toString());
  }
}
