package com.gravypod.SleepServer;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

import fi.iki.elonen.SimpleWebServer;

import sleep.error.YourCodeSucksException;
import sleep.runtime.ScriptInstance;
import sleep.runtime.ScriptLoader;

public class SleepServer {
	
	public static void main(final String[] args) {
	
		SleepServer.parseArguments(args);
		
		ScriptLoader loader = new ScriptLoader();
		
		Site[] sites = null;
		
		File configsRoot = new File("./configs/");
		SleepConfig config = new SleepConfig(loader, configsRoot);
		try {
			System.out.println("Loading config");
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
		
		String[] indexFiles = config.getIndexFiles();
		Map<String, String> mimiTypes = config.getMimi();
		
		List<Integer> ports = new ArrayList<Integer>();
		
		for (Site s : sites) {
			
			if (!ports.contains(s.getPort())) {
				ports.add(s.getPort());
			}
			
		}
		Integer[] portsInArray = Arrays.copyOf(ports.<Integer>toArray(new Integer[1]), ports.size());
		int[] p = new int[portsInArray.length];
		for (int i = 0; i < p.length; i++) {
			p[i] = (int) portsInArray[i];
		}
		SimpleWebServer server = new SimpleWebServer(sites, p, mimiTypes, indexFiles);
		
		server.run();
		
		
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
