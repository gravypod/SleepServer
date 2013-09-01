package com.gravypod.SleepServer;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;
import java.util.Stack;

import sleep.engine.types.HashContainer;
import sleep.error.YourCodeSucksException;
import sleep.interfaces.Function;
import sleep.interfaces.Loadable;
import sleep.interfaces.Variable;
import sleep.runtime.Scalar;
import sleep.runtime.ScalarArray;
import sleep.runtime.ScriptInstance;
import sleep.runtime.ScriptLoader;
import sleep.runtime.SleepUtils;

public class SleepConfig implements Loadable {
	
	private final Scalar mimi = SleepUtils.getHashWrapper(new HashMap<String, String>());
	
	private final Scalar indexFiles = SleepUtils.getArrayScalar();
	
	private final Scalar sites = SleepUtils.getHashWrapper(new HashMap<String, HashContainer>());
	
	private final ScriptLoader loader;
	
	private final File configsRoot;
	
	public SleepConfig(ScriptLoader loader, File configsRoot) {
	
		this.loader = loader;
		this.configsRoot = configsRoot;
	}
	
	@Override
	public void scriptLoaded(ScriptInstance script) {
	
		final Variable globals = script.getScriptVariables().getGlobalVariables();
		// %-> array
		// $-> Value
		globals.putScalar("%__MIMI__", mimi);
		globals.putScalar("@__INDEXFILES__", indexFiles);
		globals.putScalar("%__SITES__", sites);
		@SuppressWarnings("unchecked")
        Hashtable<String, Object> enviroment = script.getScriptEnvironment().getEnvironment();
		enviroment.put("&require", new Require());
		
	}
	
	@Override
	public void scriptUnloaded(ScriptInstance script) {
	
	}
	
	public Site[] getSites() {
	
		@SuppressWarnings("unchecked")
        HashMap<String, HashMap<String, String>> map = (HashMap<String, HashMap<String, String>>) SleepUtils.getMapFromHash(this.sites);
		
		Site[] sites = new Site[map.values().size()];
		
		int i = 0;
		
		
		for (String name : map.keySet()) {
			HashMap<String, String> props = map.get(name);
			System.out.println(props.toString());
			Site site = new Site(props);
			sites[i] = site;
			
		}
		
		return sites;
	}
	
	public String[] getIndexFiles() {
	
		ScalarArray a = indexFiles.getArray();
		
		String[] indexfiles = new String[a.size()];
		
		for (int i = 0; i < a.size(); i++) {
			indexfiles[i] = a.getAt(i).stringValue();
		}
		
		return indexfiles;
	}
	
	@SuppressWarnings("unchecked")
	// Needed, datatypes not supplied
	public Map<String, String> getMimi() {
	
		return SleepUtils.getMapFromHash(mimi);
	}
	
	class Require implements Function {
		
		/**
         * 
         */
		private static final long serialVersionUID = -6566963352840246598L;
		
		@Override
		public Scalar evaluate(String functionName, ScriptInstance anInstance, Stack passedInLocals) {
		
			try {
				ScriptInstance instance = loader.loadScript(new File(configsRoot, passedInLocals.pop().toString()));
				
				instance.run();
				loader.unloadScript(instance);
			} catch (YourCodeSucksException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}
			return SleepUtils.getEmptyScalar();
		}
	}
	
}
