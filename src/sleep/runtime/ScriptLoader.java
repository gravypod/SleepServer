/* 
 * Copyright (C) 2002-2012 Raphael Mudge (rsmudge@gmail.com)
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of 
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301  USA
 */
package sleep.runtime;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CoderResult;
import java.util.Collections;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.Map;
import java.util.Set;

import sleep.bridges.BasicIO;
import sleep.bridges.BasicNumbers;
import sleep.bridges.BasicStrings;
import sleep.bridges.BasicUtilities;
import sleep.bridges.DefaultEnvironment;
import sleep.bridges.DefaultVariable;
import sleep.bridges.FileSystemBridge;
import sleep.bridges.RegexBridge;
import sleep.bridges.TimeDateBridge;
import sleep.engine.Block;
import sleep.error.YourCodeSucksException;
import sleep.interfaces.Loadable;
import sleep.parser.Parser;
import sleep.taint.TaintModeGeneratedSteps;
import sleep.taint.TaintUtils;

/**
 * <p>
 * The ScriptLoader is a convienence container for instantiating and managing
 * ScriptInstances.
 * </p>
 * 
 * <h3>To load a script from a file and run it:</h3>
 * 
 * <pre>
 * ScriptLoader loader = new ScriptLoader();
 * ScriptInstance script = loader.loadScript(&quot;script.sl&quot;);
 * 
 * script.runScript();
 * </pre>
 * 
 * <p>
 * The above will load the file script.sl and then execute it immediately.
 * </p>
 * 
 * <p>
 * Installation of loadable bridges you create can also be managed by the
 * ScriptLoader.
 * </p>
 * 
 * <p>
 * A loadable bridge is installed into the language by adding it to a script
 * loader class. There are two types of bridges. The two types are specific and
 * global bridges.
 * </p>
 * 
 * <p>
 * The load and unload methods for a <b>specific bridge</b> are executed for
 * every script load and unload, no matter what.
 * </p>
 * 
 * <p>
 * A <b>global bridge</b> is installed once for each script environment. If
 * scripts are sharing an environment there is no sense in loading stuff into
 * the environment more than once. This is why global bridges exist.
 * </p>
 * 
 * <p>
 * An example of adding a loadable bridge to a script loader:
 * </p>
 * 
 * <pre>
 * ScriptLoader loader = new ScriptLoader()
 * loader.addSpecificBridge(new MyLoadableBridge());
 * </pre>
 * 
 * <h3>There is a difference between "loading" and "compiling" a script:</h3>
 * 
 * <p>
 * This class contains several methods to either load or compile a script.
 * Loading a script instantiates a script environment, registers the script with
 * the script loader, and registers all of the appropriate bridges with the
 * script on top of compiling the script.
 * </p>
 * 
 * <p>
 * To compile a script means to produce a runnable Block of code. On its own a
 * Block is not really runnable as a script environment is needed. For functions
 * eval(), include(), etc.. it makes sense to compile a script as you may want
 * to run the block of code within the environment of the calling script. Using
 * the compile method saves on the overhead of unnecessary script environment
 * creation and bridge registration.
 * </p>
 * 
 * <h3>Management of Script Reloading</h3>
 * 
 * <p>
 * The ScriptInstance class has a an associateFile method to associate a source
 * File object with a script. The &amp;include function calls this method when a
 * file is included into the current script context. To check if any of the
 * associated files has changed call hasChanged on the appropriate
 * ScriptInstance.
 * </P>
 * 
 * <p>
 * The ScriptLoader will automatically associate a source file with a
 * ScriptInstance when a File object is passed to loadScript. If you choose to
 * do some voodoo compiling scripts and managing your own cache (not necessary
 * btw) then you will have to call associateFile against any ScriptInstance you
 * construct
 * </p>
 * 
 * <h3>Script Cache</h3>
 * 
 * <p>
 * The ScriptLoader mantains a cache of Blocks. These are indexed by name and a
 * timestamp of when they were created. You may call the touch method with the
 * name and a timestamp to allow the ScriptLoader to invalidate the cache entry.
 * If you just load scripts from files then the script cache will just work. To
 * disable the cache use <code>loader.setGlobalCache(false)</code>.
 * </p>
 * 
 * <p>
 * Hopefully this helped to clarify things. :)
 * </p>
 */
public class ScriptLoader {
	
	/**
	 * cache for parsed scripts mantained (optionally) by the script loader.
	 */
	protected static Map<String, Object[]> BLOCK_CACHE = null;
	
	private Block retrieveCacheEntry(final String name) {
	
		if (ScriptLoader.BLOCK_CACHE != null && ScriptLoader.BLOCK_CACHE.containsKey(name)) {
			final Object[] temp = ScriptLoader.BLOCK_CACHE.get(name);
			
			return (Block) temp[0];
		}
		
		return null;
	}
	
	private static boolean isCacheHit(final String name) {
	
		return ScriptLoader.BLOCK_CACHE != null && ScriptLoader.BLOCK_CACHE.containsKey(name);
	}
	
	/**
	 * nudge the cache with the last modified time of the specified script. this
	 * call will delete the script from the cache if the lastModifiedTime >
	 * lastLoadTime
	 */
	public void touch(final String name, final long lastModifiedTime) {
	
		if (ScriptLoader.BLOCK_CACHE != null && ScriptLoader.BLOCK_CACHE.containsKey(name)) {
			final Object[] temp = ScriptLoader.BLOCK_CACHE.get(name);
			final long loaded = ((Long) temp[1]).longValue();
			
			if (lastModifiedTime > loaded) {
				ScriptLoader.BLOCK_CACHE.remove(name);
			}
		}
	}
	
	/**
	 * loaded scripts
	 */
	protected LinkedList<ScriptInstance> loadedScripts;
	
	/**
	 * loaded scripts except referable by key
	 */
	protected Map<String, ScriptInstance> scripts;
	
	/**
	 * global bridges
	 */
	protected LinkedList<Loadable> bridgesg;
	
	/**
	 * specific bridges
	 */
	protected LinkedList<Loadable> bridgess;
	
	/**
	 * path to search for jar files imported using [import * from: *] syntax
	 */
	protected LinkedList paths;
	
	/**
	 * initializes the script loader
	 */
	public ScriptLoader() {
	
		loadedScripts = new LinkedList<ScriptInstance>();
		scripts = new HashMap<String, ScriptInstance>();
		bridgesg = new LinkedList<Loadable>();
		bridgess = new LinkedList<Loadable>();
		
		initDefaultBridges();
	}
	
	/**
	 * The Sleep script loader can optionally cache parsed script files once
	 * they are loaded. This is useful if you will have several script loader
	 * instances loading the same script files in isolated objects.
	 */
	public Map setGlobalCache(final boolean setting) {
	
		if (setting && ScriptLoader.BLOCK_CACHE == null) {
			ScriptLoader.BLOCK_CACHE = Collections.synchronizedMap(new HashMap());
		}
		
		if (!setting) {
			ScriptLoader.BLOCK_CACHE = null;
		}
		
		return ScriptLoader.BLOCK_CACHE;
	}
	
	/**
	 * method call to initialize the default bridges, if you want to change the
	 * default bridges subclass this class and override this method
	 */
	protected void initDefaultBridges() {
	
		addGlobalBridge(new BasicNumbers());
		addGlobalBridge(new BasicStrings());
		addGlobalBridge(new BasicUtilities());
		addGlobalBridge(new BasicIO());
		addGlobalBridge(new FileSystemBridge());
		addGlobalBridge(new DefaultEnvironment());
		addGlobalBridge(new DefaultVariable());
		addGlobalBridge(new RegexBridge());
		addGlobalBridge(new TimeDateBridge());
	}
	
	/**
	 * A global bridge is loaded into an environment once and only once. This
	 * way if the environment is shared among multiple script instances this
	 * will save on both memory and script load time
	 */
	public void addGlobalBridge(final Loadable l) {
	
		bridgesg.add(l);
	}
	
	/**
	 * A specific bridge is loaded into *every* script regardless of wether or
	 * not the environment is shared. Useful for modifying the script instance
	 * while it is being in processed. Specific bridges are the first thing that
	 * happens after the script code is parsed
	 */
	public void addSpecificBridge(final Loadable l) {
	
		bridgess.add(l);
	}
	
	/**
	 * Returns a HashMap with all loaded scripts, the key is a string which is
	 * just the filename, the value is a ScriptInstance object
	 */
	public Map getScriptsByKey() {
	
		return scripts;
	}
	
	/**
	 * Determines wether or not the script is loaded by checking if the
	 * specified key exists in the script db.
	 */
	public boolean isLoaded(final String name) {
	
		return scripts.containsKey(name);
	}
	
	/**
	 * Convienence method to return the script environment of the first script
	 * tht was loaded, returns null if no scripts are loaded
	 */
	public ScriptEnvironment getFirstScriptEnvironment() {
	
		if (loadedScripts.size() > 0) {
			final ScriptInstance si = loadedScripts.getFirst();
			return si.getScriptEnvironment();
		}
		
		return null;
	}
	
	/**
	 * Returns a linked list of all loaded ScriptInstance objects
	 */
	public LinkedList getScripts() {
	
		return loadedScripts;
	}
	
	/**
	 * Process the newly loaded script. Setup its name and load the bridges into
	 * the environment assuming this hasn't been done before.
	 */
	protected void inProcessScript(final String name, final ScriptInstance si) {
	
		si.setName(name);
		
		Iterator i = bridgess.iterator();
		while(i.hasNext()) {
			((Loadable) i.next()).scriptLoaded(si);
		}
		
		// load the "global" bridges iff they need to be loaded again...
		if (si.getScriptEnvironment().getEnvironment().get("(isloaded)") != this) {
			i = bridgesg.iterator();
			while(i.hasNext()) {
				((Loadable) i.next()).scriptLoaded(si);
			}
			si.getScriptEnvironment().getEnvironment().put("(isloaded)", this);
		}
	}
	
	/**
	 * Load a serialized version of the script iff a serialized version exists,
	 * and its modification time is greater than the modification time of the
	 * script. Also handles the muss and fuss of reserializing the script if it
	 * has to reload the script. Personally I didn't find much of a startup time
	 * decrease when loading the scripts serialized versus parsing them each
	 * time. Theres a command 'bload' in the console to benchmark loading a
	 * script normally versus serialized. Try it.
	 * 
	 * @param script
	 *            a file object pointing to the script file...
	 */
	public ScriptInstance loadSerialized(final File script, final Hashtable env) throws IOException, ClassNotFoundException {
	
		final File bin = new File(script.getAbsolutePath() + ".bin");
		
		if (bin.exists() && (!script.exists() || script.lastModified() < bin.lastModified())) {
			return loadSerialized(script.getName(), new FileInputStream(bin), env);
		}
		
		final ScriptInstance si = loadScript(script, env);
		ScriptLoader.saveSerialized(si);
		return si;
	}
	
	/**
	 * Loads a serialized script from the specified input stream with the
	 * specified name
	 */
	public ScriptInstance loadSerialized(final String name, final InputStream stream, final Hashtable env) throws IOException, ClassNotFoundException {
	
		final ObjectInputStream p = new ObjectInputStream(stream);
		final Block block = (Block) p.readObject();
		return loadScript(name, block, env);
	}
	
	/**
	 * Saves a serialized version of the compiled script to scriptname.bin.
	 */
	public static void saveSerialized(final ScriptInstance si) throws IOException {
	
		ScriptLoader.saveSerialized(si, new FileOutputStream(si.getName() + ".bin"));
	}
	
	/**
	 * Saves a serialized version of the ScriptInstance si to the specified
	 * output stream
	 */
	public static void saveSerialized(final ScriptInstance si, final OutputStream stream) throws IOException {
	
		final ObjectOutputStream o = new ObjectOutputStream(stream);
		o.writeObject(si.getRunnableBlock());
	}
	
	/**
	 * creates a Sleep script instance using the precompiled code, name, and
	 * shared environment. This function also processes the script using the
	 * global and specific bridges registered with this script loader. No
	 * reference to the newly created script is kept by the script loader
	 */
	public ScriptInstance loadScriptNoReference(final String name, final Block code, final Hashtable env) {
	
		final ScriptInstance si = new ScriptInstance(env);
		si.installBlock(code);
		inProcessScript(name, si);
		
		return si;
	}
	
	/**
	 * creates a Sleep script instance using the precompiled code, name, and
	 * shared environment. This function also processes the script using the
	 * global and specific bridges registered with this script loader. The
	 * script is also referened by this loader so it can be processed again
	 * (during the unload phase) when unloadScript is called.
	 */
	public ScriptInstance loadScript(final String name, final Block code, final Hashtable env) {
	
		final ScriptInstance si = loadScriptNoReference(name, code, env);
		
		// add script to our loaded scripts data structure
		
		if (!name.equals("<interact mode>")) {
			loadedScripts.add(si);
			scripts.put(name, si);
		}
		
		return si;
	}
	
	/** loads the specified script */
	public ScriptInstance loadScript(final String name, final String code, final Hashtable env) throws YourCodeSucksException {
	
		return loadScript(name, compileScript(name, code), env);
	}
	
	/** compiles a script using the specified stream as a source */
	public Block compileScript(final String name, final InputStream stream) throws YourCodeSucksException, IOException {
	
		if (ScriptLoader.isCacheHit(name)) {
			stream.close();
			return retrieveCacheEntry(name);
		}
		
		final StringBuffer code = new StringBuffer(8192);
		
		final BufferedReader in = new BufferedReader(getInputStreamReader(stream));
		String s = in.readLine();
		while(s != null) {
			code.append("\n");
			code.append(s);
			s = in.readLine();
		}
		
		in.close();
		stream.close();
		
		return compileScript(name, code.toString());
	}
	
	/**
	 * compiles the specified script file
	 */
	public Block compileScript(final File file) throws IOException, YourCodeSucksException {
	
		touch(file.getAbsolutePath(), file.lastModified());
		return compileScript(file.getAbsolutePath(), new FileInputStream(file));
	}
	
	/**
	 * compiles the specified script file
	 */
	public Block compileScript(final String fileName) throws IOException, YourCodeSucksException {
	
		return compileScript(new File(fileName));
	}
	
	/** compiles the specified script into a runnable block */
	public Block compileScript(final String name, final String code) throws YourCodeSucksException {
	
		if (ScriptLoader.isCacheHit(name)) {
			return retrieveCacheEntry(name);
		} else {
			final Parser temp = new Parser(name, code);
			
			if (TaintUtils.isTaintMode()) {
				temp.setCodeFactory(new TaintModeGeneratedSteps());
			}
			
			temp.parse();
			
			if (ScriptLoader.BLOCK_CACHE != null) {
				ScriptLoader.BLOCK_CACHE.put(name, new Object[] { temp.getRunnableBlock(), new Long(System.currentTimeMillis()) });
			}
			
			return temp.getRunnableBlock();
		}
	}
	
	/** loads a script from the specified inputstream */
	public ScriptInstance loadScript(final String name, final InputStream stream) throws YourCodeSucksException, IOException {
	
		return loadScript(name, stream, null);
	}
	
	/**
	 * loads a script from the specified input stream using the specified
	 * hashtable as a shared environment
	 */
	public ScriptInstance loadScript(final String name, final InputStream stream, final Hashtable env) throws YourCodeSucksException, IOException {
	
		return loadScript(name, compileScript(name, stream), env);
	}
	
	/**
	 * Loads the specified script file
	 */
	public ScriptInstance loadScript(final String fileName) throws IOException, YourCodeSucksException {
	
		return loadScript(new File(fileName), null);
	}
	
	/**
	 * Loads the specified script file, uses the specified hashtable for the
	 * environment
	 */
	public ScriptInstance loadScript(final String fileName, final Hashtable env) throws IOException, YourCodeSucksException {
	
		return loadScript(new File(fileName), env);
	}
	
	/**
	 * Loads the specified script file, uses the specified hashtable for the
	 * environment
	 */
	public ScriptInstance loadScript(final File file, final Hashtable env) throws IOException, YourCodeSucksException {
	
		final ScriptInstance script = loadScript(file.getAbsolutePath(), new FileInputStream(file), env);
		script.associateFile(file);
		return script;
	}
	
	/**
	 * Loads the specified script file
	 */
	public ScriptInstance loadScript(final File file) throws IOException, YourCodeSucksException {
	
		return loadScript(file, null);
	}
	
	/**
	 * unload a script
	 */
	public void unloadScript(final String filename) {
	
		unloadScript(scripts.get(filename));
	}
	
	/**
	 * unload a script
	 */
	public void unloadScript(final ScriptInstance script) {
	
		// clear the block cache of this script...
		if (ScriptLoader.BLOCK_CACHE != null) {
			ScriptLoader.BLOCK_CACHE.remove(script.getName());
		}
		
		//
		// remove script from our loaded scripts data structure
		//
		loadedScripts.remove(script);
		scripts.remove(script.getName());
		
		//
		// the script must always be set to unloaded first and foremost!
		//
		script.setUnloaded();
		
		//
		// tell bridges script is going bye bye
		//
		Iterator i = bridgess.iterator();
		while(i.hasNext()) {
			final Loadable temp = (Loadable) i.next();
			temp.scriptUnloaded(script);
		}
		
		i = bridgesg.iterator();
		while(i.hasNext()) {
			final Loadable temp = (Loadable) i.next();
			temp.scriptUnloaded(script);
		}
	}
	
	/**
	 * A convienence method to determine the set of scripts to "unload" based on
	 * a passed in set of scripts that are currently configured. The configured
	 * scripts are compared to the loaded scripts. Scripts that are loaded but
	 * not configured are determined to be in need of unloading. The return Set
	 * contains String objects of the script names. The passed in Set is
	 * expected to be the same thing (a bunch of Strings).
	 */
	public Set getScriptsToUnload(final Set configured) {
	
		Set unload, loaded;
		unload = new LinkedHashSet();
		
		// scripts that are currently loaded and active...
		loaded = scripts.keySet();
		
		// scripts that need to be unloaded...
		unload.addAll(loaded);
		unload.removeAll(configured);
		
		return unload;
	}
	
	/**
	 * A convienence method to determine the set of scripts to "load" based on a
	 * passed in set of scripts that are currently configured. The configured
	 * scripts are compared to the loaded scripts. Scripts that are configured
	 * but not loaded are determined to be in need of loading. The return Set
	 * contains String objects of the script names. The passed in Set is
	 * expected to be the same thing (a bunch of Strings).
	 */
	public Set getScriptsToLoad(final Set configured) {
	
		Set load, loaded;
		load = new LinkedHashSet();
		
		// scripts that are currently loaded and active...
		loaded = scripts.keySet();
		
		// scripts that need to be unloaded...
		load.addAll(configured);
		load.removeAll(loaded);
		
		return load;
	}
	
	/**
	 * Java by default maps characters from an 8bit ascii file to an internal
	 * 32bit unicode representation. How this mapping is done is called a
	 * character set encoding. Sometimes this conversion can frustrate scripters
	 * making them say "hey, I didn't put that character in my script". You can
	 * use this option to ensure sleep disables charset conversions for scripts
	 * loaded with this script loader
	 */
	public void setCharsetConversion(final boolean b) {
	
		disableConversions = !b;
	}
	
	public boolean isCharsetConversions() {
	
		return !disableConversions;
	}
	
	protected boolean disableConversions = false;
	
	private static CharsetDecoder decoder = null;
	
	private String charset = null;
	
	public String getCharset() {
	
		return charset;
	}
	
	/**
	 * If charset conversion is enabled and charset is set, then the stream will
	 * be read using specified charset.
	 * 
	 * @param charset
	 *            The name of a supported {@link java.nio.charset.Charset
	 *            </code>charset<code>}
	 */
	public void setCharset(final String charset) {
	
		this.charset = charset;
	}
	
	private InputStreamReader getInputStreamReader(final InputStream in) {
	
		if (disableConversions) {
			if (ScriptLoader.decoder == null) {
				ScriptLoader.decoder = new NoConversion();
			}
			
			return new InputStreamReader(in, ScriptLoader.decoder);
		}
		
		if (charset != null) {
			try {
				return new InputStreamReader(in, charset);
			} catch (final UnsupportedEncodingException e) {
				e.printStackTrace();
			}
		}
		
		return new InputStreamReader(in);
	}
	
	/**
	 * Java likes to convert characters inside of a loaded script into something
	 * else. This prevents that if the app developer chooses to flag that option
	 */
	private static class NoConversion extends CharsetDecoder {
		
		public NoConversion() {
		
			super(null, 1.0f, 1.0f);
		}
		
		@Override
		protected CoderResult decodeLoop(final ByteBuffer in, final CharBuffer out) {
		
			int mark = in.position();
			try {
				while(in.hasRemaining()) {
					if (!out.hasRemaining()) {
						return CoderResult.OVERFLOW;
					}
					
					int index = in.get();
					if (index >= 0) {
						out.put((char) index);
					} else {
						index = 256 + index;
						out.put((char) index);
					}
					mark++;
				}
				return CoderResult.UNDERFLOW;
			} finally {
				in.position(mark);
			}
		}
	}
}
