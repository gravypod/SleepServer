package com.gravypod.SleepServer;

import java.io.File;
import java.io.IOException;
import java.util.Scanner;

import sleep.error.YourCodeSucksException;
import sleep.runtime.ScriptInstance;
import sleep.runtime.ScriptLoader;

public class SleepServer {
	
	public static void main(final String[] args) {
	
		SleepServer.parseArguments(args);
		
		ScriptLoader loader = new ScriptLoader();
		
		Site[] sites = null;
		
		File configsRoot = new File("./configs/");
		try {
			System.out.println("Loading config");
			SleepConfig config = new SleepConfig(loader, configsRoot);
			loader.addGlobalBridge(config);
			ScriptInstance instance = loader.loadScript("./configs/config.sl");
			System.out.println("Config loaded. Parsing");
			instance.run();
			System.out.println("Config parsed");
			loader.unloadScript(instance);
			sites = config.getSites();
			System.out.println("Sites all located, " + sites.length + " in total");
		} catch (YourCodeSucksException e1) {
			e1.printStackTrace();
			System.out.println("Config could not be parsed");
			System.exit(1);
		} catch (IOException e1) {
			e1.printStackTrace();
			System.out.println("Config could not be parsed");
			System.exit(1);
		}
		
		for (Site s : sites) {
			s.start();
		}

		Scanner sc = new Scanner(System.in);
		
		while (sc.hasNext()) {
			String next = sc.nextLine();
			switch (next) {
				case "kill":
				case "shutdown":
				case "stop":
					System.exit(0);
			}
		}
		
		
		
	}
	
	public static void parseArguments(final String[] args) {
	
		for (int i = 0; i < args.length; i++) {
			
			final String element = args[i];
			if (element.equalsIgnoreCase("-h") || element.equalsIgnoreCase("--h") || element.equalsIgnoreCase("--help") || element.equalsIgnoreCase("-help") || element.equalsIgnoreCase("help")) {
				System.out.println("--help, -help, help: this help printout");
				System.out.println("-h (host), --host (host): set the hostname to bind to");
				System.out.println("-p (port), --port (port): set the port to use to");
				System.out.println("-p (dir), --port (dir): set the root directory");
				System.out.println("-i (file deliminated by ';'), --index (files deliminated by ';'): set the default file");
				
				System.exit(0);
				
			} else if (element.equalsIgnoreCase("-p") || element.equalsIgnoreCase("--port")) {
				
			}
			
		}
		
	}
	
}
