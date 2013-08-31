package com.gravypod.SleepServer;

import java.io.File;
import java.io.IOException;
import java.util.Map;

import fi.iki.elonen.SimpleWebServer;

public class Site extends Thread {
	
	private int port = 80;
	
	private String name = "";
	
	private String host = null;
	
	private String rootDir = "./sites/";
	
	private Map<String, String> mimiTypes;
	
	private String[] indexFiles;
	
	public Site(String[] indexFiles, Map<String, String> mimi, Map<String, String> props) {
	
		String sitename = props.get("sitename");
		if (!(sitename == null)) {
			this.name = sitename;
		}
		String serveraddress = props.get("severaddress");
		if (!(host == null))
			this.host = serveraddress;
		String siteport = props.get("siteport");
		try {
			this.port = Integer.parseInt(siteport);
		} catch (Exception e) {
			System.out.println("Invalid port for site: " + sitename + ". Using port 80");
			this.port = 80;
		}
		String rootdirectory = props.get("rootdirectory");
		this.rootDir = rootdirectory;
		this.mimiTypes = mimi;
		this.indexFiles = indexFiles;
		System.out.println(mimi.toString());
	}
	
	
	
	public SimpleWebServer init() {
	
		SimpleWebServer server = new SimpleWebServer(/*host, */port, new File(rootDir), mimiTypes, indexFiles);
		
		return server;
		
	}
	
	@Override
	public void run() {
	
		SimpleWebServer server = init();
		System.out.println("Starting server " + this.name);
		try {
			server.start();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public String getHost() {
	
		return host;
	}
	
	public String getSiteName() {
	
		return name;
	}
	
	public int getPort() {
	
		return port;
	}
	
	public String getRootDir() {
	
		return rootDir;
	}
}
