package com.gravypod.SleepServer;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;
import java.util.Stack;
import java.util.regex.Matcher;

import sleep.bridges.io.BufferObject;
import sleep.bridges.io.IOObject;
import sleep.interfaces.Function;
import sleep.interfaces.Loadable;
import sleep.interfaces.Variable;
import sleep.runtime.Scalar;
import sleep.runtime.ScriptInstance;
import sleep.runtime.ScriptLoader;
import sleep.runtime.ScriptVariables;
import sleep.runtime.SleepUtils;
import sleep.taint.TaintUtils;

public class SleepCodeHandler {
	
	private final String fileContents;
	
	private final String classID;
	
	private String mimeType;
	
	private final Map<String, String> headers = new HashMap<String, String>();
	
	private final Map<String, String> data;
	
	private ScriptVariables lastVariables = null;
	
	private final Map<String, String> header;
	
	private final String method;
	
	private final ScriptLoader loader;
	
	public SleepCodeHandler(final File f, final String mime, final Map<String, String> parms, final Map<String, String> header, final String method, final ScriptLoader loader) throws IOException {
	
		loader.addGlobalBridge(new SleepCodeHandler.HTTPCommands());
		
		final BufferedReader br = new BufferedReader(new FileReader(f));
		
		final StringBuffer str = new StringBuffer();
		
		String line = br.readLine();
		
		while(line != null) {
			str.append(line);
			line = br.readLine();
		}
		
		classID = f.getName();
		
		mimeType = mime;
		
		fileContents = str.toString();
		
		data = parms;
		
		SleepUtils.getHashWrapper(data);
		
		this.header = header;
		
		this.method = method;
		
		this.loader = loader;
		
	}
	
	public String parseSleep() {
	
		return findScripts(fileContents);
	}
	
	private String findScripts(final String str) {
	
		final Matcher matcher = Constants.TAG_REGEX.matcher(str);
		
		final StringBuffer builder = new StringBuffer();
		
		int i = 0;
		
		while(matcher.find()) {
			
			final String s = matcher.group(1);
			
			String scriptOutput;
			
			try {
				final ScriptInstance instance = loader.loadScript(classID + i++, s.trim(), new Hashtable<String, Function>());
				instance.chdir(new File("./configs/"));
				final BufferObject buffer = new BufferObject();
				
				buffer.allocate(2048);
				
				IOObject.setConsole(instance.getScriptEnvironment(), buffer);
				
				instance.runScript();
				
				lastVariables = instance.getScriptVariables();
				
				scriptOutput = new String(buffer.getSource().toByteArray());
				
				loader.unloadScript(instance);
				
			} catch (final Exception e) {
				
				scriptOutput = Arrays.toString(e.getStackTrace());
			}
			
			matcher.appendReplacement(builder, scriptOutput);
			
		}
		
		matcher.appendTail(builder);
		
		return builder.toString();
		
	}
	
	class HTTPCommands implements Loadable {
		
		@Override
		public void scriptLoaded(final ScriptInstance script) {
		
			@SuppressWarnings("unchecked")
			final Hashtable<String, Function> temp = script.getScriptEnvironment().getEnvironment();
			final Variable globals = script.getScriptVariables().getGlobalVariables();
			globals.putScalar("%__HEADERS__", TaintUtils.taint(SleepUtils.getHashWrapper(header)));
			globals.putScalar("%__DATA__", TaintUtils.taint(SleepUtils.getHashWrapper(data)));
			globals.putScalar("$__METHOD__", TaintUtils.taint(SleepUtils.getScalar(method)));
			temp.put("&header", new HeaderManager());
			temp.put("&mime", new Mime());
			
			if (lastVariables != null && script.getName().startsWith(classID)) {
				script.setScriptVariables(lastVariables);
			}
		}
		
		@Override
		public void scriptUnloaded(final ScriptInstance script) {
		
		}
		
	}
	
	class HeaderManager implements Function {
		
		/**
         * 
         */
		private static final long serialVersionUID = 4343364118163085112L;
		
		@Override
		@SuppressWarnings(value = "rawtypes")
		public Scalar evaluate(final String functionName, final ScriptInstance anInstance, final Stack passedInLocals) {
		
			final String key = passedInLocals.pop().toString();
			final String val = passedInLocals.pop().toString();
			headers.put(key, val);
			return SleepUtils.getEmptyScalar();
		}
		
	}
	
	class Mime implements Function {
		
		/**
         * 
         */
		private static final long serialVersionUID = -5648490699909518586L;
		
		@Override
		@SuppressWarnings(value = "rawtypes")
		public Scalar evaluate(final String functionName, final ScriptInstance anInstance, final Stack passedInLocals) {
		
			mimeType = passedInLocals.pop().toString();
			
			return SleepUtils.getEmptyScalar();
		}
		
	}
	
	public String getFileContents() {
	
		return fileContents;
	}
	
	public Map<String, String> getHeaders() {
	
		return headers;
	}
	
	public void setMimeType(final String mimeType) {
	
		this.mimeType = mimeType;
	}
	
	public String getMimeType() {
	
		return mimeType;
	}
	
}
