package test.jpms.k;

import com.google.gson.Gson;

public class Foo {

	public void serialize() {
		Gson gson = new Gson();
		String json = gson.toJson(new MyObj());
	}

	public class MyObj {
		public String	a;
		public int		b;
	}
}
