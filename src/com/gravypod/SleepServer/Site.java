package com.gravypod.SleepServer;

import java.util.Map;

public class Site {
	
	private int port = 80;
	
	private String name = "";
	
	private String host = null;
	
	private String rootDir = "./sites/";
	
	public Site(Map<String, String> props) {
	
		String sitename = props.get("sitename");
		if (!(sitename == null)) {
			this.name = sitename;
		}
		String serveraddress = props.get("serveraddress");
		if (!(serveraddress == null))
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
		System.out.println("Starting host: " + host);
	}
	
	public String matchingHost() {
	
		return host != null ? getHost().replace("?", ".?").replace("*", ".*?") : host;
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
