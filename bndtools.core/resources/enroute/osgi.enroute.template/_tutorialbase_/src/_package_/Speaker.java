package _package_;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;

public class Speaker implements AutoCloseable {
	private String name="";

	public Speaker() {
		say("Hello");
	}

	public void close() {
		say("Goodbye");
	}

	public boolean say(String message) {
		String speak = name + " " + message;
		try {
			ScriptEngine	engine	= new ScriptEngineManager().getEngineByName("AppleScript");
			engine.eval("say \"" + speak + "\"");
			return true;
		} catch (Exception e) {
			// try next
		}
		
		try {
			Process exec = Runtime.getRuntime().exec("espeak " + speak);
			return exec.exitValue() == 0;
		} catch (Exception e) {
			// try next
		}
		
		// No Text to Speech found ...
		
		System.out.println(speak);
		return false;
	}

	private void setName(String name) {
		this.name = name;
	}

	public static void main(String args[]) throws IOException {
		try (Speaker speaker = new Speaker()) {
			BufferedReader r = new BufferedReader( new InputStreamReader(System.in));
			
			while(true) {
				String line = r.readLine();
				if ( line== null || "quit".equals(line) || "exit".equals(line))
					return;
				
				String arguments[] = line.split(" ");
				for (int i = 0; i < arguments.length; i++) {
					if ("--name".equals(arguments[i]))
						speaker.setName(arguments[++i]);
					else
						speaker.say(arguments[i]);
				}
			}
		}
	}

}
