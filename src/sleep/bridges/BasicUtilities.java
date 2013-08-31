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
package sleep.bridges;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.ListIterator;
import java.util.Map;
import java.util.Stack;

import sleep.engine.Block;
import sleep.engine.ObjectUtilities;
import sleep.engine.ProxyInterface;
import sleep.engine.types.OrderedHashContainer;
import sleep.error.YourCodeSucksException;
import sleep.interfaces.Function;
import sleep.interfaces.Loadable;
import sleep.interfaces.Operator;
import sleep.interfaces.Predicate;
import sleep.interfaces.Variable;
import sleep.parser.ParserConfig;
import sleep.runtime.Scalar;
import sleep.runtime.ScalarArray;
import sleep.runtime.ScriptEnvironment;
import sleep.runtime.ScriptInstance;
import sleep.runtime.ScriptLoader;
import sleep.runtime.ScriptVariables;
import sleep.runtime.SleepUtils;
import sleep.runtime.WatchScalar;
import sleep.taint.TaintUtils;

/** implementation of basic utility functions */
public class BasicUtilities implements Function, Loadable, Predicate {
	
	/**
     * 
     */
	private static final long serialVersionUID = -6181467109886642434L;
	
	static {
		ParserConfig.addKeyword("isa");
		ParserConfig.addKeyword("in");
		ParserConfig.addKeyword("=~");
	}
	
	@Override
	public void scriptUnloaded(final ScriptInstance i) {
	
	}
	
	@Override
	public void scriptLoaded(final ScriptInstance i) {
	
		final Hashtable temp = i.getScriptEnvironment().getEnvironment();
		//
		// functions
		//
		
		final Function f_array = new array();
		final Function f_hash = new hash();
		
		temp.put("&array", f_array);
		temp.put("&hash", f_hash);
		temp.put("&ohash", f_hash);
		temp.put("&ohasha", f_hash);
		temp.put("&@", f_array);
		temp.put("&%", f_hash);
		
		// array & hashtable related
		temp.put("&concat", this);
		
		temp.put("&keys", this); // &keys(%hash) = @array
		temp.put("&size", this); // &size(@array) = <int>
		temp.put("&push", this); // &push(@array, $value) = $scalar
		temp.put("&pop", this); // &pop(@array) = $scalar
		temp.put("&add", this); // &pop(@array) = $scalar
		temp.put("&flatten", this); // &pop(@array) = $scalar
		temp.put("&clear", this);
		temp.put("&splice", this);
		temp.put("&subarray", this);
		temp.put("&sublist", this);
		temp.put("&copy", new copy());
		temp.put("&setRemovalPolicy", this);
		temp.put("&setMissPolicy", this);
		
		temp.put("&untaint", TaintUtils.Sanitizer(this));
		temp.put("&taint", TaintUtils.Tainter(this));
		
		final map map_f = new map();
		
		temp.put("&map", map_f);
		temp.put("&filter", map_f);
		
		final Function f_cast = new f_cast();
		temp.put("&cast", f_cast);
		temp.put("&casti", f_cast);
		
		temp.put("&putAll", this);
		
		temp.put("&addAll", this);
		temp.put("&removeAll", this);
		temp.put("&retainAll", this);
		
		temp.put("&pushl", this);
		temp.put("&popl", this);
		
		temp.put("&search", this);
		temp.put("&reduce", this);
		temp.put("&values", this);
		temp.put("&remove", this); // not safe within foreach loops (since they use an iterator, and remove throws an exception)
		temp.put("-istrue", this); // predicate -istrue <Scalar>, determine wether or not the scalar is null or not.
		temp.put("-isarray", this);
		temp.put("-ishash", this);
		temp.put("-isfunction", this);
		temp.put("-istainted", this);
		temp.put("isa", this);
		temp.put("in", this);
		temp.put("=~", this);
		temp.put("&setField", this);
		temp.put("&typeOf", this);
		temp.put("&newInstance", this);
		temp.put("&scalar", this);
		
		temp.put("&exit", this);
		
		final SetScope scopeFunctions = new SetScope();
		
		temp.put("&local", scopeFunctions);
		temp.put("&this", scopeFunctions);
		temp.put("&global", scopeFunctions);
		
		temp.put("&watch", this);
		
		temp.put("&debug", this);
		temp.put("&warn", this);
		temp.put("&profile", this);
		temp.put("&getStackTrace", this);
		
		temp.put("&reverse", new reverse()); // @array2 = &reverse(@array) 
		temp.put("&removeAt", new removeAt()); // not safe within foreach loops yada yada yada...
		temp.put("&shift", new shift()); // not safe within foreach loops yada yada yada...
		
		temp.put("&systemProperties", new systemProperties());
		temp.put("&use", TaintUtils.Sensitive(new f_use()));
		temp.put("&include", TaintUtils.Sensitive(temp.get("&use")));
		temp.put("&checkError", this);
		
		// closure / function handle type stuff
		temp.put("&lambda", new lambda());
		temp.put("&compile_closure", TaintUtils.Sensitive(temp.get("&lambda")));
		temp.put("&let", temp.get("&lambda"));
		
		final function funcs = new function();
		temp.put("&function", TaintUtils.Sensitive(funcs));
		temp.put("function", temp.get("&function")); /* special form used by the compiler */
		temp.put("&setf", funcs);
		temp.put("&eval", TaintUtils.Sensitive(new eval()));
		temp.put("&expr", TaintUtils.Sensitive(temp.get("&eval")));
		
		// synchronization primitives...
		final SyncPrimitives sync = new SyncPrimitives();
		temp.put("&semaphore", sync);
		temp.put("&acquire", sync);
		temp.put("&release", sync);
		
		temp.put("&invoke", this);
		temp.put("&inline", this);
		
		temp.put("=>", new HashKeyValueOp());
	}
	
	private static class SyncPrimitives implements Function {
		
		/**
         * 
         */
		private static final long serialVersionUID = 7594523469968133134L;
		
		@Override
		public Scalar evaluate(final String n, final ScriptInstance si, final Stack l) {
		
			if (n.equals("&semaphore")) {
				final int initial = BridgeUtilities.getInt(l, 1);
				return SleepUtils.getScalar(new Semaphore(initial));
			} else if (n.equals("&acquire")) {
				final Semaphore sem = (Semaphore) BridgeUtilities.getObject(l);
				sem.P();
			} else if (n.equals("&release")) {
				final Semaphore sem = (Semaphore) BridgeUtilities.getObject(l);
				sem.V();
			}
			
			return SleepUtils.getEmptyScalar();
		}
	}
	
	private static class HashKeyValueOp implements Operator {
		
		@Override
		public Scalar operate(final String name, final ScriptInstance script, final Stack locals) {
		
			final Scalar identifier = (Scalar) locals.pop();
			final Scalar value = (Scalar) locals.pop();
			
			return SleepUtils.getScalar(new KeyValuePair(identifier, value));
		}
	}
	
	@Override
	public boolean decide(final String predName, final ScriptInstance anInstance, final Stack terms) {
	
		if (predName.equals("isa")) {
			final Class blah = BridgeUtilities.getClass(terms, null);
			final Object bleh = BridgeUtilities.getObject(terms);
			return blah != null && blah.isInstance(bleh);
		} else if (predName.equals("=~")) {
			final Scalar right = BridgeUtilities.getScalar(terms);
			final Scalar left = BridgeUtilities.getScalar(terms);
			
			return left.sameAs(right);
		} else if (predName.equals("in")) {
			final Scalar temp = BridgeUtilities.getScalar(terms);
			
			if (temp.getHash() != null) {
				final String key = BridgeUtilities.getString(terms, "");
				return temp.getHash().getData().containsKey(key) && !SleepUtils.isEmptyScalar((Scalar) temp.getHash().getData().get(key));
			} else {
				final Iterator iter = SleepUtils.getIterator(temp, anInstance);
				final Scalar left = BridgeUtilities.getScalar(terms);
				
				while(iter.hasNext()) {
					final Scalar right = (Scalar) iter.next();
					
					if (left.sameAs(right)) {
						return true;
					}
				}
				
				return false;
			}
		}
		
		final Scalar value = (Scalar) terms.pop();
		
		// Times when a scalar is considered true:
		// - its value is not equal to 0
		// - its not null (string value is not "")
		//
		// Scalar - String intValue
		//   0       "0"      0         - false
		//  null     ""       0         - false
		//  "blah"   "blah"   0         - true
		//  "3"      "3"      3         - true
		//   
		if (predName.equals("-istrue")) {
			return SleepUtils.isTrueScalar(value);
		}
		
		if (predName.equals("-isfunction")) {
			return SleepUtils.isFunctionScalar(value);
		}
		
		if (predName.equals("-istainted")) {
			return TaintUtils.isTainted(value);
		}
		
		if (predName.equals("-isarray")) {
			return value.getArray() != null;
		}
		
		if (predName.equals("-ishash")) {
			return value.getHash() != null;
		}
		
		return false;
	}
	
	private static class f_use implements Function {
		
		/**
         * 
         */
		private static final long serialVersionUID = -7949918561033165628L;
		
		private final HashMap bridges = new HashMap();
		
		@Override
		public Scalar evaluate(final String n, final ScriptInstance si, final Stack l) {
		
			File parent = null;
			String className = "";
			Class bridge = null;
			
			if (l.size() == 2) {
				parent = sleep.parser.ParserConfig.findJarFile(l.pop().toString());
				className = BridgeUtilities.getString(l, "");
			} else {
				final Scalar obj = (Scalar) l.pop();
				if (obj.objectValue() instanceof Class && n.equals("&use")) {
					bridge = (Class) obj.objectValue();
				} else {
					final File a = sleep.parser.ParserConfig.findJarFile(obj.toString());
					
					parent = a.getParentFile();
					className = a.getName();
				}
			}
			
			if (parent != null && !parent.exists()) {
				throw new IllegalArgumentException(n + ": could not locate source '" + parent + "'");
			}
			
			try {
				if (n.equals("&use")) {
					if (bridge == null) {
						if (parent != null) {
							final URLClassLoader loader = new URLClassLoader(new URL[] { parent.toURL() });
							bridge = Class.forName(className, true, loader);
						} else {
							bridge = Class.forName(className);
						}
					}
					
					Loadable temp;
					
					if (bridges.get(bridge) == null) {
						temp = (Loadable) bridge.newInstance();
						bridges.put(bridge, temp);
					} else {
						temp = (Loadable) bridges.get(bridge);
					}
					
					temp.scriptLoaded(si);
				} else {
					Block script;
					final ScriptLoader sloader = (ScriptLoader) si.getScriptEnvironment().getEnvironment().get("(isloaded)");
					InputStream istream;
					
					Scalar incz = si.getScriptVariables().getScalar("$__INCLUDE__");
					if (incz == null) {
						incz = SleepUtils.getEmptyScalar();
						si.getScriptVariables().getGlobalVariables().putScalar("$__INCLUDE__", incz);
					}
					
					if (parent != null) {
						final File theFile = parent.isDirectory() ? new File(parent, className) : parent;
						
						final URLClassLoader loader = new URLClassLoader(new URL[] { parent.toURL() });
						sloader.touch(className, theFile.lastModified());
						si.associateFile(theFile); /* associate this included script with the current script instance */
						
						istream = loader.getResourceAsStream(className);
						incz.setValue(SleepUtils.getScalar(theFile));
					} else {
						final File tempf = BridgeUtilities.toSleepFile(className, si);
						sloader.touch(className, tempf.lastModified());
						si.associateFile(tempf); /* associate this included script with the current script instance */
						
						istream = new FileInputStream(tempf);
						incz.setValue(SleepUtils.getScalar(tempf));
					}
					
					if (istream != null) {
						script = sloader.compileScript(className, istream);
						SleepUtils.runCode(script, si.getScriptEnvironment());
					} else {
						throw new IOException("unable to locate " + className + " from: " + parent);
					}
				}
			} catch (final YourCodeSucksException yex) {
				si.getScriptEnvironment().flagError(yex);
			} catch (final Exception ex) {
				si.getScriptEnvironment().flagError(ex);
			}
			
			return SleepUtils.getEmptyScalar();
		}
	}
	
	private static class array implements Function {
		
		/**
         * 
         */
		private static final long serialVersionUID = 5703909141112674068L;
		
		@Override
		public Scalar evaluate(final String n, final ScriptInstance si, final Stack l) {
		
			final Scalar value = SleepUtils.getArrayScalar();
			
			while(!l.isEmpty()) {
				value.getArray().push(SleepUtils.getScalar(BridgeUtilities.getScalar(l)));
			}
			
			return value;
		}
	}
	
	private static class f_cast implements Function {
		
		/**
         * 
         */
		private static final long serialVersionUID = 1725535655725049496L;
		
		@Override
		public Scalar evaluate(final String n, final ScriptInstance si, final Stack l) {
		
			final Scalar value = BridgeUtilities.getScalar(l);
			final Scalar type = BridgeUtilities.getScalar(l);
			
			if (n.equals("&casti")) {
				final Class atype = ObjectUtilities.convertScalarDescriptionToClass(type);
				
				if (atype != null) {
					final Object tempo = ObjectUtilities.buildArgument(atype, value, si);
					return SleepUtils.getScalar(tempo);
				} else {
					throw new RuntimeException("&casti: '" + type + "' is an invalid primitive cast identifier");
				}
			}
			
			if (value.getArray() == null) {
				if (type.toString().charAt(0) == 'c') {
					return SleepUtils.getScalar(value.toString().toCharArray());
				} else if (type.toString().charAt(0) == 'b') {
					return SleepUtils.getScalar((Object) BridgeUtilities.toByteArrayNoConversion(value.toString()));
				}
				
				return SleepUtils.getEmptyScalar();
			}
			
			if (l.size() == 0) {
				l.push(SleepUtils.getScalar(value.getArray().size()));
			}
			
			final int dimensions[] = new int[l.size()];
			int totaldim = 1;
			
			for (int x = 0; !l.isEmpty(); x++) {
				dimensions[x] = BridgeUtilities.getInt(l, 0);
				
				totaldim *= dimensions[x];
			}
			
			Object rv;
			
			Class atype = ObjectUtilities.convertScalarDescriptionToClass(type);
			
			if (atype == null) {
				atype = ObjectUtilities.getArrayType(value, Object.class);
			}
			
			final Scalar flat = BridgeUtilities.flattenArray(value, null);
			
			if (totaldim != flat.getArray().size()) {
				throw new RuntimeException("&cast: specified dimensions " + totaldim + " is not equal to total array elements " + flat.getArray().size());
			}
			
			rv = Array.newInstance(atype, dimensions);
			
			final int current[] = new int[dimensions.length]; // defaults at 0, 0, 0
			
			/* special case, we're casting an empty array */
			if (flat.getArray().size() == 0) {
				return SleepUtils.getScalar(rv);
			}
			
			for (int x = 0; true; x++) {
				Object tempa = rv;
				
				//
				// find our index
				//
				for (int z = 0; z < current.length - 1; z++) {
					tempa = Array.get(tempa, current[z]);
				}
				
				//
				// set our value
				//
				final Object tempo = ObjectUtilities.buildArgument(atype, flat.getArray().getAt(x), si);
				Array.set(tempa, current[current.length - 1], tempo);
				
				//
				// increment our index step...
				//
				current[current.length - 1] += 1;
				
				for (int y = current.length - 1; current[y] >= dimensions[y]; y--) {
					if (y == 0) {
						return SleepUtils.getScalar(rv); // we're done building the array at this point...
					}
					
					current[y] = 0;
					current[y - 1] += 1;
				}
			}
			
		}
	}
	
	private static class function implements Function {
		
		/**
         * 
         */
		private static final long serialVersionUID = -542597086183730497L;
		
		@Override
		public Scalar evaluate(final String n, final ScriptInstance si, final Stack l) {
		
			if (n.equals("&function") || n.equals("function")) {
				final String temp = BridgeUtilities.getString(l, "");
				
				if (temp.length() == 0 || temp.charAt(0) != '&') {
					throw new IllegalArgumentException(n + ": requested function name must begin with '&'");
				}
				
				return SleepUtils.getScalar(si.getScriptEnvironment().getFunction(temp));
			} else if (n.equals("&setf")) {
				final String temp = BridgeUtilities.getString(l, "&eh");
				final Object o = BridgeUtilities.getObject(l);
				
				if (temp.charAt(0) == '&' && (o == null || o instanceof Function)) {
					if (o == null) {
						si.getScriptEnvironment().getEnvironment().remove(temp);
					} else {
						si.getScriptEnvironment().getEnvironment().put(temp, o);
					}
				} else if (temp.charAt(0) != '&') {
					throw new IllegalArgumentException("&setf: invalid function name '" + temp + "'");
				} else if (o != null) {
					throw new IllegalArgumentException("&setf: can not set function " + temp + " to a " + o.getClass());
				}
			}
			
			return SleepUtils.getEmptyScalar();
		}
	}
	
	private static class hash implements Function {
		
		/**
         * 
         */
		private static final long serialVersionUID = 3045589984373699622L;
		
		@Override
		public Scalar evaluate(final String n, final ScriptInstance si, final Stack l) {
		
			Scalar value = null;
			if (n.equals("&ohash")) {
				value = SleepUtils.getOrderedHashScalar();
			} else if (n.equals("&ohasha")) {
				value = SleepUtils.getAccessOrderedHashScalar();
			} else {
				value = SleepUtils.getHashScalar();
			}
			
			while(!l.isEmpty()) {
				final KeyValuePair kvp = BridgeUtilities.getKeyValuePair(l);
				
				final Scalar blah = value.getHash().getAt(kvp.getKey());
				blah.setValue(kvp.getValue());
			}
			
			return value;
		}
	}
	
	private static class lambda implements Function {
		
		/**
         * 
         */
		private static final long serialVersionUID = 7141437913366360548L;
		
		@Override
		public Scalar evaluate(final String n, final ScriptInstance si, final Stack l) {
		
			SleepClosure value;
			SleepClosure temp;
			
			if (n.equals("&lambda")) {
				temp = BridgeUtilities.getFunction(l, si);
				value = new SleepClosure(si, temp.getRunnableCode());
			} else if (n.equals("&compile_closure")) {
				final String code = l.pop().toString();
				
				try {
					temp = new SleepClosure(si, SleepUtils.ParseCode(code));
					value = temp;
				} catch (final YourCodeSucksException ex) {
					si.getScriptEnvironment().flagError(ex);
					return SleepUtils.getEmptyScalar();
				}
			} else {
				temp = BridgeUtilities.getFunction(l, si);
				value = temp;
			}
			
			Variable vars = value.getVariables();
			
			while(!l.isEmpty()) {
				final KeyValuePair kvp = BridgeUtilities.getKeyValuePair(l);
				
				if (kvp.getKey().toString().equals("$this")) {
					final SleepClosure c = (SleepClosure) kvp.getValue().objectValue();
					value.setVariables(c.getVariables());
					vars = c.getVariables();
				} else {
					vars.putScalar(kvp.getKey().toString(), SleepUtils.getScalar(kvp.getValue()));
				}
			}
			
			return SleepUtils.getScalar(value);
		}
	}
	
	private static class map implements Function {
		
		/**
         * 
         */
		private static final long serialVersionUID = -2435734531998826827L;
		
		@Override
		public Scalar evaluate(final String n, final ScriptInstance si, final Stack l) {
		
			final SleepClosure temp = BridgeUtilities.getFunction(l, si);
			final Iterator i = BridgeUtilities.getIterator(l, si);
			
			final Scalar rv = SleepUtils.getArrayScalar();
			final Stack locals = new Stack();
			
			while(i.hasNext()) {
				locals.push(i.next());
				
				final Scalar val = temp.callClosure("eval", si, locals);
				
				if (!SleepUtils.isEmptyScalar(val) || n.equals("&map")) {
					rv.getArray().push(SleepUtils.getScalar(val));
				}
				
				locals.clear();
			}
			
			return rv;
		}
	}
	
	private static class copy implements Function {
		
		/**
         * 
         */
		private static final long serialVersionUID = -2633113315423102646L;
		
		@Override
		public Scalar evaluate(final String n, final ScriptInstance si, final Stack l) {
		
			final Scalar doit = BridgeUtilities.getScalar(l);
			
			if (doit.getArray() != null || SleepUtils.isFunctionScalar(doit)) {
				final Scalar value = SleepUtils.getArrayScalar();
				final Iterator i = doit.getArray() == null ? SleepUtils.getFunctionFromScalar(doit, si).scalarIterator() : doit.getArray().scalarIterator();
				
				while(i.hasNext()) {
					value.getArray().push(SleepUtils.getScalar((Scalar) i.next()));
				}
				
				return value;
			} else if (doit.getHash() != null) {
				final Scalar value = SleepUtils.getHashScalar();
				final Iterator i = doit.getHash().keys().scalarIterator();
				while(i.hasNext()) {
					final Scalar key = (Scalar) i.next();
					final Scalar temp = value.getHash().getAt(key);
					temp.setValue(doit.getHash().getAt(key));
				}
				
				return value;
			} else {
				return SleepUtils.getScalar(doit);
			}
		}
	}
	
	private static class removeAt implements Function {
		
		/**
         * 
         */
		private static final long serialVersionUID = 5997209206417094017L;
		
		@Override
		public Scalar evaluate(final String n, final ScriptInstance si, final Stack l) {
		
			final Scalar value = (Scalar) l.pop();
			
			if (value.getArray() != null) {
				while(!l.isEmpty()) {
					value.getArray().remove(BridgeUtilities.normalize(BridgeUtilities.getInt(l, 0), value.getArray().size()));
				}
			} else if (value.getHash() != null) {
				while(!l.isEmpty()) {
					final Scalar remove = value.getHash().getAt((Scalar) l.pop()); /* set each key to null to remove */
					remove.setValue(SleepUtils.getEmptyScalar());
				}
			}
			
			return SleepUtils.getEmptyScalar();
		}
	}
	
	private static class shift implements Function {
		
		/**
         * 
         */
		private static final long serialVersionUID = -7654442484621083617L;
		
		@Override
		public Scalar evaluate(final String n, final ScriptInstance si, final Stack l) {
		
			final ScalarArray value = BridgeUtilities.getArray(l);
			return value.remove(0);
		}
	}
	
	private static class reverse implements Function {
		
		/**
         * 
         */
		private static final long serialVersionUID = -8332221772525706092L;
		
		@Override
		public Scalar evaluate(final String n, final ScriptInstance si, final Stack l) {
		
			final Scalar value = SleepUtils.getArrayScalar();
			final Iterator i = BridgeUtilities.getIterator(l, si);
			
			while(i.hasNext()) {
				value.getArray().add(SleepUtils.getScalar((Scalar) i.next()), 0);
			}
			
			return value;
		}
	}
	
	private static class SetScope implements Function {
		
		/**
         * 
         */
		private static final long serialVersionUID = -6544469117444583011L;
		
		private final java.util.regex.Pattern splitter = java.util.regex.Pattern.compile("\\s+");
		
		@Override
		public Scalar evaluate(final String n, final ScriptInstance i, final Stack l) {
		
			Variable level = null;
			
			if (n.equals("&local")) {
				level = i.getScriptVariables().getLocalVariables();
			} else if (n.equals("&this")) {
				level = i.getScriptVariables().getClosureVariables();
			} else if (n.equals("&global")) {
				level = i.getScriptVariables().getGlobalVariables();
			}
			
			final String temp = l.pop().toString();
			
			if (level == null) {
				return SleepUtils.getEmptyScalar();
			}
			
			final String vars[] = splitter.split(temp);
			for (final String var : vars) {
				if (level.scalarExists(var)) {
					// do nothing...
				} else if (var.charAt(0) == '$') {
					i.getScriptVariables().setScalarLevel(var, SleepUtils.getEmptyScalar(), level);
				} else if (var.charAt(0) == '@') {
					i.getScriptVariables().setScalarLevel(var, SleepUtils.getArrayScalar(), level);
				} else if (var.charAt(0) == '%') {
					i.getScriptVariables().setScalarLevel(var, SleepUtils.getHashScalar(), level);
				} else {
					throw new IllegalArgumentException(n + ": malformed variable name '" + var + "' from '" + temp + "'");
				}
			}
			
			return SleepUtils.getEmptyScalar();
		}
	}
	
	private static class systemProperties implements Function {
		
		/**
         * 
         */
		private static final long serialVersionUID = -7964126981899463888L;
		
		@Override
		public Scalar evaluate(final String n, final ScriptInstance i, final Stack l) {
		
			return SleepUtils.getHashWrapper(System.getProperties());
		}
	}
	
	private static class eval implements Function {
		
		/**
         * 
         */
		private static final long serialVersionUID = -1640732929543789194L;
		
		@Override
		public Scalar evaluate(final String n, final ScriptInstance i, final Stack l) {
		
			final String code = l.pop().toString();
			
			try {
				if (n.equals("&eval")) {
					final Scalar temp = SleepUtils.getScalar(i.getScriptEnvironment().evaluateStatement(code));
					return temp;
				} else {
					final Scalar temp = SleepUtils.getScalar(i.getScriptEnvironment().evaluateExpression(code));
					return temp;
				}
			} catch (final YourCodeSucksException ex) {
				i.getScriptEnvironment().flagError(ex);
				return SleepUtils.getEmptyScalar();
			}
		}
	}
	
	@Override
	public Scalar evaluate(final String n, final ScriptInstance i, final Stack l) {
	
		if (l.isEmpty() && n.equals("&remove")) {
			final Stack iterators = (Stack) i.getScriptEnvironment().getContextMetadata("iterators");
			
			if (iterators == null || iterators.isEmpty()) {
				throw new RuntimeException("&remove: no active foreach loop to remove element from");
			} else {
				final sleep.engine.atoms.Iterate.IteratorData d = (sleep.engine.atoms.Iterate.IteratorData) iterators.peek();
				d.iterator.remove();
				d.count = d.count - 1;
				return d.source;
			}
		} else if (n.equals("&watch")) {
			Variable level;
			final String temp = BridgeUtilities.getString(l, "");
			final String vars[] = temp.split(" ");
			for (final String var : vars) {
				level = i.getScriptVariables().getScalarLevel(var, i);
				if (level != null) {
					final WatchScalar watch = new WatchScalar(var, i.getScriptEnvironment());
					watch.setValue(level.getScalar(var));
					i.getScriptVariables().setScalarLevel(var, watch, level);
				} else {
					throw new IllegalArgumentException(var + " must already exist in a scope prior to watching");
				}
			}
		} else if (n.equals("&scalar")) {
			return ObjectUtilities.BuildScalar(true, BridgeUtilities.getObject(l));
		} else if (n.equals("&untaint") || n.equals("&taint")) {
			/* the actual tainting / untaing of this value takes place in the wrapper specified in the bridge itself */
			return !l.isEmpty() ? (Scalar) l.pop() : SleepUtils.getEmptyScalar();
		} else if (n.equals("&newInstance")) {
			final Scalar top = BridgeUtilities.getScalar(l);
			
			if (top.getArray() != null) {
				final Class clz[] = (Class[]) ObjectUtilities.buildArgument(Class[].class, top, i);
				final SleepClosure closure = (SleepClosure) BridgeUtilities.getObject(l);
				
				return SleepUtils.getScalar(ProxyInterface.BuildInterface(clz, closure, i));
			} else {
				final Class clz = (Class) top.objectValue();
				final SleepClosure closure = (SleepClosure) BridgeUtilities.getObject(l);
				
				return SleepUtils.getScalar(SleepUtils.newInstance(clz, closure, i));
			}
		} else if (n.equals("&typeOf")) {
			final Scalar s = BridgeUtilities.getScalar(l);
			if (s.getArray() != null) {
				return SleepUtils.getScalar(s.getArray().getClass());
			}
			if (s.getHash() != null) {
				return SleepUtils.getScalar(s.getHash().getClass());
			}
			return SleepUtils.getScalar(s.getActualValue().getType());
		} else if (n.equals("&inline")) {
			final SleepClosure c = BridgeUtilities.getFunction(l, i);
			c.getRunnableCode().evaluate(i.getScriptEnvironment());
			return SleepUtils.getEmptyScalar();
		} else if (n.equals("&invoke")) {
			final Map params = BridgeUtilities.extractNamedParameters(l);
			
			final SleepClosure c = BridgeUtilities.getFunction(l, i);
			final Stack args = new Stack();
			final Iterator iter = BridgeUtilities.getIterator(l, i);
			while(iter.hasNext()) {
				args.add(0, iter.next());
			}
			
			String message = BridgeUtilities.getString(l, null);
			
			/* parameters option */
			if (params.containsKey("parameters")) {
				final Scalar h = (Scalar) params.get("parameters");
				
				final Iterator it = h.getHash().keys().scalarIterator();
				while(it.hasNext()) {
					final Scalar key = (Scalar) it.next();
					final KeyValuePair temp = new KeyValuePair(key, h.getHash().getAt(key));
					args.add(0, SleepUtils.getScalar(temp));
				}
			}
			
			/* message option */
			if (params.containsKey("message")) {
				message = params.get("message").toString();
			}
			
			final Variable old = c.getVariables();
			
			/* environment option */
			if (params.containsKey("$this")) {
				final SleepClosure t = (SleepClosure) ((Scalar) params.get("$this")).objectValue();
				c.setVariables(t.getVariables());
			}
			
			final Scalar rv = c.callClosure(message, i, args);
			c.setVariables(old);
			return rv;
		} else if (n.equals("&checkError")) {
			final Scalar value = BridgeUtilities.getScalar(l);
			value.setValue(i.getScriptEnvironment().checkError());
			return value;
		} else if (n.equals("&profile")) {
			return SleepUtils.getArrayWrapper(i.getProfilerStatistics());
		} else if (n.equals("&getStackTrace")) {
			return SleepUtils.getArrayWrapper(i.getStackTrace());
		} else if (n.equals("&warn")) {
			/* for those looking at how to read current line number from an executing function, you can't.  this function
			   is a special case.  the parser looks for &warn and adds an extra argument containing the current line number */
			i.fireWarning(BridgeUtilities.getString(l, "warning requested"), BridgeUtilities.getInt(l, -1));
			return SleepUtils.getEmptyScalar();
		} else if (n.equals("&debug")) {
			/* allow the script to programatically set the debug level */
			if (!l.isEmpty()) {
				final int flag = BridgeUtilities.getInt(l, 0);
				i.setDebugFlags(flag);
			}
			
			return SleepUtils.getScalar(i.getDebugFlags());
		} else if (n.equals("&flatten")) {
			return BridgeUtilities.flattenIterator(BridgeUtilities.getIterator(l, i), null);
		} else if (n.equals("&pushl") || n.equals("&popl")) {
			final ScriptVariables vars = i.getScriptVariables();
			
			if (n.equals("&pushl")) {
				vars.pushLocalLevel();
			} else if (n.equals("&popl")) {
				if (vars.haveMoreLocals()) {
					vars.popLocalLevel();
				} else {
					throw new RuntimeException("&popl: no more local frames exist");
				}
			}
			
			if (!l.isEmpty()) {
				BridgeUtilities.initLocalScope(vars, vars.getLocalVariables(), l);
			}
			
			return SleepUtils.getEmptyScalar();
		} else if (n.equals("&concat")) {
			final Scalar value = SleepUtils.getArrayScalar();
			
			while(!l.isEmpty()) {
				final Scalar temp = (Scalar) l.pop();
				
				if (temp.getArray() != null) {
					final Iterator iter = temp.getArray().scalarIterator();
					while(iter.hasNext()) {
						value.getArray().push(SleepUtils.getScalar((Scalar) iter.next()));
					}
				} else {
					value.getArray().push(SleepUtils.getScalar(temp));
				}
			}
			
			return value;
		}
		
		/** Start of many array functions */
		
		final Scalar value = BridgeUtilities.getScalar(l);
		
		if (n.equals("&push") && BridgeUtilities.expectArray(n, value)) {
			Scalar pushed = null;
			while(!l.isEmpty()) {
				pushed = (Scalar) l.pop();
				value.getArray().push(SleepUtils.getScalar(pushed));
			}
			
			return pushed == null ? SleepUtils.getEmptyScalar() : pushed;
		} else if ((n.equals("&retainAll") || n.equals("&removeAll")) && BridgeUtilities.expectArray(n, value)) {
			final ScalarArray a = value.getArray();
			final ScalarArray b = BridgeUtilities.getArray(l);
			Scalar temp;
			
			final HashSet s = new HashSet();
			Iterator iter = b.scalarIterator();
			while(iter.hasNext()) {
				temp = (Scalar) iter.next();
				s.add(temp.identity());
			}
			
			iter = a.scalarIterator();
			while(iter.hasNext()) {
				temp = (Scalar) iter.next();
				
				if (!s.contains(temp.identity())) {
					if (n.equals("&retainAll")) {
						iter.remove();
					}
				} else {
					if (n.equals("&removeAll")) {
						iter.remove();
					}
				}
			}
			
			return SleepUtils.getArrayScalar(a);
		} else if (n.equals("&addAll") && BridgeUtilities.expectArray(n, value)) {
			final ScalarArray a = value.getArray();
			final ScalarArray b = BridgeUtilities.getArray(l);
			
			final HashSet s = new HashSet();
			Iterator iter = a.scalarIterator();
			Scalar temp;
			
			while(iter.hasNext()) {
				temp = (Scalar) iter.next();
				s.add(temp.identity());
			}
			
			iter = b.scalarIterator();
			while(iter.hasNext()) {
				temp = (Scalar) iter.next();
				
				if (!s.contains(temp.identity())) {
					a.push(SleepUtils.getScalar(temp));
				}
			}
			
			return SleepUtils.getArrayScalar(a);
		} else if (n.equals("&add") && value.getArray() != null) {
			final Scalar item = BridgeUtilities.getScalar(l);
			final int index = BridgeUtilities.normalize(BridgeUtilities.getInt(l, 0), value.getArray().size() + 1);
			value.getArray().add(SleepUtils.getScalar(item), index);
			return value;
		} else if (n.equals("&add") && value.getHash() != null) {
			while(!l.isEmpty()) {
				final KeyValuePair kvp = BridgeUtilities.getKeyValuePair(l);
				
				final Scalar blah = value.getHash().getAt(kvp.getKey());
				blah.setValue(kvp.getValue());
			}
			
			return value;
		} else if (n.equals("&splice") && BridgeUtilities.expectArray(n, value)) {
			// splice(@old, @stuff, start, n to remove)
			/* normalize all of the parameters please */
			
			final ScalarArray insert = BridgeUtilities.getArray(l);
			final int start = BridgeUtilities.normalize(BridgeUtilities.getInt(l, 0), value.getArray().size());
			final int torem = BridgeUtilities.getInt(l, insert.size()) + start;
			
			/* remove the specified elements please */
			
			int y = start;
			
			final Iterator iter = value.getArray().scalarIterator();
			for (int x = 0; x < start && iter.hasNext(); x++) {
				iter.next();
			}
			
			while(y < torem) {
				if (iter.hasNext()) {
					iter.next();
					iter.remove();
				}
				
				y++;
			}
			
			/* insert some elements */
			
			final ListIterator liter = (ListIterator) value.getArray().scalarIterator();
			for (int x = 0; x < start && liter.hasNext(); x++) {
				liter.next();
			}
			
			final Iterator j = insert.scalarIterator();
			while(j.hasNext()) {
				final Scalar ins = (Scalar) j.next();
				liter.add(ins);
			}
			
			return value;
		} else if (n.equals("&pop") && BridgeUtilities.expectArray(n, value)) {
			return value.getArray().pop();
		} else if (n.equals("&size") && value.getArray() != null) // &size(@array)
		{
			return SleepUtils.getScalar(value.getArray().size());
		} else if (n.equals("&size") && value.getHash() != null) // &size(@array)
		{
			return SleepUtils.getScalar(value.getHash().keys().size());
		} else if (n.equals("&clear")) {
			if (value.getArray() != null) {
				final Iterator iter = value.getArray().scalarIterator();
				while(iter.hasNext()) {
					iter.next();
					iter.remove();
				}
			} else if (value.getHash() != null) {
				value.setValue(SleepUtils.getHashScalar());
			} else {
				value.setValue(SleepUtils.getEmptyScalar());
			}
		} else if (n.equals("&search") && BridgeUtilities.expectArray(n, value)) {
			final SleepClosure f = BridgeUtilities.getFunction(l, i);
			int start = BridgeUtilities.normalize(BridgeUtilities.getInt(l, 0), value.getArray().size());
			int count = 0;
			final Stack locals = new Stack();
			
			final Iterator iter = value.getArray().scalarIterator();
			while(iter.hasNext()) {
				final Scalar temp = (Scalar) iter.next();
				
				if (start > 0) {
					start--;
					count++;
					continue;
				}
				
				locals.push(SleepUtils.getScalar(count));
				locals.push(temp);
				final Scalar val = f.callClosure("eval", i, locals);
				
				if (!SleepUtils.isEmptyScalar(val)) {
					return val;
				}
				
				locals.clear();
				count++;
			}
		} else if (n.equals("&reduce") && SleepUtils.isFunctionScalar(value)) {
			final SleepClosure f = SleepUtils.getFunctionFromScalar(value, i);
			final Stack locals = new Stack();
			
			final Iterator iter = BridgeUtilities.getIterator(l, i);
			
			Scalar a = iter.hasNext() ? (Scalar) iter.next() : SleepUtils.getEmptyScalar();
			Scalar b = iter.hasNext() ? (Scalar) iter.next() : SleepUtils.getEmptyScalar();
			final Scalar temp = null;
			
			locals.push(a);
			locals.push(b);
			
			a = f.callClosure("eval", i, locals);
			
			locals.clear();
			
			while(iter.hasNext()) {
				b = (Scalar) iter.next();
				
				locals.push(b);
				locals.push(a);
				a = f.callClosure("eval", i, locals);
				
				locals.clear();
			}
			
			return a;
		} else if ((n.equals("&subarray") || n.equals("&sublist")) && BridgeUtilities.expectArray(n, value)) {
			return BasicUtilities.sublist(value, BridgeUtilities.getInt(l, 0), BridgeUtilities.getInt(l, value.getArray().size()));
		} else if (n.equals("&remove")) {
			while(!l.isEmpty()) {
				final Scalar scalar = (Scalar) l.pop();
				
				if (value.getArray() != null) {
					value.getArray().remove(scalar);
				} else if (value.getHash() != null) {
					value.getHash().remove(scalar);
				}
			}
			
			return value;
		} else if (n.equals("&keys")) // &keys(%hash)
		{
			if (value.getHash() != null) {
				final Scalar temp = SleepUtils.getEmptyScalar();
				temp.setValue(value.getHash().keys());
				return temp;
			}
		} else if (n.equals("&setRemovalPolicy") || n.equals("&setMissPolicy")) {
			if (value.getHash() == null || !(value.getHash() instanceof OrderedHashContainer)) {
				throw new IllegalArgumentException(n + ": expected an ordered hash, received: " + SleepUtils.describe(value));
			}
			
			final SleepClosure function = BridgeUtilities.getFunction(l, i);
			final OrderedHashContainer blah = (OrderedHashContainer) value.getHash();
			if (n.equals("&setMissPolicy")) {
				blah.setMissPolicy(function);
			} else {
				blah.setRemovalPolicy(function);
			}
		} else if (n.equals("&putAll")) {
			if (value.getHash() != null) {
				final Iterator keys = BridgeUtilities.getIterator(l, i);
				final Iterator values = l.isEmpty() ? keys : BridgeUtilities.getIterator(l, i);
				
				while(keys.hasNext()) {
					final Scalar blah = value.getHash().getAt((Scalar) keys.next());
					if (values.hasNext()) {
						blah.setValue((Scalar) values.next());
					} else {
						blah.setValue(SleepUtils.getEmptyScalar());
					}
				}
			} else if (value.getArray() != null) {
				final Iterator temp = BridgeUtilities.getIterator(l, i);
				while(temp.hasNext()) {
					final Scalar next = (Scalar) temp.next();
					value.getArray().push(SleepUtils.getScalar(next));
				}
			}
			
			return value;
		} else if (n.equals("&values")) // &values(%hash)
		{
			if (value.getHash() != null) {
				final Scalar temp = SleepUtils.getArrayScalar();
				
				if (l.isEmpty()) {
					final Iterator iter = value.getHash().getData().values().iterator();
					while(iter.hasNext()) {
						final Scalar next = (Scalar) iter.next();
						
						if (!SleepUtils.isEmptyScalar(next)) {
							temp.getArray().push(SleepUtils.getScalar(next));
						}
					}
				} else {
					final Iterator iter = BridgeUtilities.getIterator(l, i);
					while(iter.hasNext()) {
						final Scalar key = (Scalar) iter.next();
						temp.getArray().push(SleepUtils.getScalar(value.getHash().getAt(key)));
					}
				}
				return temp;
			}
		} else if (n.equals("&exit")) {
			i.getScriptEnvironment().flagReturn(null, ScriptEnvironment.FLOW_CONTROL_THROW); /* a null throw will exit the interpreter */
			if (!SleepUtils.isEmptyScalar(value)) {
				throw new RuntimeException(value.toString());
			}
		} else if (n.equals("&setField")) {
			// setField(class/object, "field", "value")
			
			Field setMe = null;
			Class aClass = null;
			Object inst = null;
			
			if (value.objectValue() == null) {
				throw new IllegalArgumentException("&setField: can not set field on a null object");
			} else if (value.objectValue() instanceof Class) {
				aClass = (Class) value.objectValue();
				inst = null;
			} else {
				inst = value.objectValue();
				aClass = inst.getClass();
			}
			
			while(!l.isEmpty()) {
				final KeyValuePair pair = BridgeUtilities.getKeyValuePair(l);
				
				final String name = pair.getKey().toString();
				final Scalar arg = pair.getValue();
				
				try {
					try {
						setMe = aClass.getDeclaredField(name);
					} catch (final NoSuchFieldException nsfe) {
						setMe = aClass.getField(name);
					}
					
					if (ObjectUtilities.isArgMatch(setMe.getType(), arg) != 0) {
						setMe.setAccessible(true);
						setMe.set(inst, ObjectUtilities.buildArgument(setMe.getType(), arg, i));
					} else {
						throw new RuntimeException("unable to convert " + SleepUtils.describe(arg) + " to a " + setMe.getType());
					}
				} catch (final NoSuchFieldException fex) {
					throw new RuntimeException("no field named " + name + " in " + aClass);
				} catch (final RuntimeException rex) {
					throw rex;
				} catch (final Exception ex) {
					throw new RuntimeException("cannot set " + name + " in " + aClass + ": " + ex.getMessage());
				}
			}
		}
		
		return SleepUtils.getEmptyScalar();
	}
	
	private static Scalar sublist(final Scalar value, final int _start, final int _end) {
	
		final int length = value.getArray().size();
		int start, end;
		
		start = BridgeUtilities.normalize(_start, length);
		end = _end < 0 ? _end + length : _end;
		end = end <= length ? end : length;
		
		if (start > end) {
			throw new IllegalArgumentException("illegal subarray(" + SleepUtils.describe(value) + ", " + _start + " -> " + start + ", " + _end + " -> " + end + ")");
		}
		
		return SleepUtils.getArrayScalar(value.getArray().sublist(start, end));
	}
}
