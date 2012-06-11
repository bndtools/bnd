package foo;

import foo2.*;
import foo3.*;
import aQute.bnd.annotation.component.Activate;
import aQute.bnd.annotation.component.Component;

@Component
public class TestLevelOne {
  @Activate
  public void start() {
    System.out.println("TestLevelOne started!");
  }
  public TestLevelTwo get2() {
    return null;
  };
  public TestLevelThree get3() {
    return null;
  };
}
