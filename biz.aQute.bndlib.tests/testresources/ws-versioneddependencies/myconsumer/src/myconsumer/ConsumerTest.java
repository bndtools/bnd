package myconsumer;

import mydependency.MyService;

public class ConsumerTest {
	MyService myService;
	
	public void doSomething() {
		myService.a();
	}
	
}
