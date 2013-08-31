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
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Stack;

import sleep.engine.types.ObjectValue;
import sleep.interfaces.Variable;
import sleep.runtime.Scalar;
import sleep.runtime.ScalarArray;
import sleep.runtime.ScalarHash;
import sleep.runtime.ScriptInstance;
import sleep.runtime.ScriptVariables;
import sleep.runtime.SleepUtils;

/**
 * A bridge is a class that bridges your applications API and sleep. Bridges are
 * created using interfaces from the sleep.interfaces package. Arguments are
 * passed to bridges generally in a java.util.Stack form. The Stack of arguments
 * contains sleep Scalar objects. The BridgeUtilities makes it safer and easier
 * for you to extract Java types from arguments.
 * 
 * <pre>
 * 
 * 
 * 
 * 
 * // some code to execute an internal add function, not a complete example
 * 
 * public class MyAddFunction implements Function {
 * 	
 * 	public Scalar evaluate(String name, ScriptInstance script, Stack arguments) {
 * 	
 * 		if (name.equals(&quot;&amp;add&quot;)) {
 * 			int a = BridgeUtilities.getInt(arguments, 0);
 * 			int b = BridgeUtilities.getInt(arguments, 0);
 * 			
 * 			return SleepUtils.getScalar(a + b);
 * 		}
 * 		
 * 		return SleepUtils.getEmptyScalar();
 * 	}
 * }
 * </pre>
 * 
 */
public class BridgeUtilities {
	
	/**
	 * converts the specified string to an array of bytes (useful as Sleep
	 * stores byte arrays to strings)
	 */
	public static byte[] toByteArrayNoConversion(final String textz) {
	
		final byte[] data = new byte[textz.length()];
		
		for (int y = 0; y < data.length; y++) {
			data[y] = (byte) textz.charAt(y);
		}
		
		return data;
	}
	
	/** grab an integer. if the stack is empty 0 will be returned. */
	public static int getInt(final Stack arguments) {
	
		return BridgeUtilities.getInt(arguments, 0);
	}
	
	/**
	 * grab an integer, if the stack is empty the default value will be returned
	 */
	public static int getInt(final Stack arguments, final int defaultValue) {
	
		if (arguments.isEmpty()) {
			return defaultValue;
		}
		
		return ((Scalar) arguments.pop()).intValue();
	}
	
	/** grab a class, if the stack is empty the default value will be returned */
	public static Class getClass(final Stack arguments, final Class defaultValue) {
	
		final Object obj = BridgeUtilities.getObject(arguments);
		if (obj == null) {
			return defaultValue;
		}
		return (Class) obj;
	}
	
	/** grab a long. if the stack is empty 0 will be returned. */
	public static long getLong(final Stack arguments) {
	
		return BridgeUtilities.getLong(arguments, 0L);
	}
	
	/** grab a long, if the stack is empty the default value will be returned */
	public static long getLong(final Stack arguments, final long defaultValue) {
	
		if (arguments.isEmpty()) {
			return defaultValue;
		}
		
		return ((Scalar) arguments.pop()).longValue();
	}
	
	/** grab a double. if the stack is empty a 0 will be returned */
	public static double getDouble(final Stack arguments) {
	
		return BridgeUtilities.getDouble(arguments, 0.0);
	}
	
	/** grab a double, if the stack is empty the default value will be returned */
	public static double getDouble(final Stack arguments, final double defaultValue) {
	
		if (arguments.isEmpty()) {
			return defaultValue;
		}
		
		return ((Scalar) arguments.pop()).doubleValue();
	}
	
	/**
	 * extracts all named parameters from the argument stack. this method
	 * returns a Map whose keys are strings and values are Scalars.
	 */
	public static Map extractNamedParameters(final Stack args) {
	
		final Map rv = new HashMap();
		final Iterator i = args.iterator();
		while(i.hasNext()) {
			final Scalar temp = (Scalar) i.next();
			if (temp.objectValue() != null && temp.objectValue().getClass() == KeyValuePair.class) {
				i.remove();
				final KeyValuePair value = (KeyValuePair) temp.objectValue();
				rv.put(value.getKey().toString(), value.getValue());
			}
		}
		
		return rv;
	}
	
	/**
	 * grabs a scalar iterator, this can come from either an array or a closure
	 * called continuously until $null is returned.
	 */
	public static Iterator getIterator(final Stack arguments, final ScriptInstance script) {
	
		if (arguments.isEmpty()) {
			return BridgeUtilities.getArray(arguments).scalarIterator();
		}
		
		final Scalar temp = (Scalar) arguments.pop();
		return SleepUtils.getIterator(temp, script);
	}
	
	/**
	 * grab a sleep array, if the stack is empty a scalar array with no elements
	 * will be returned.
	 */
	public static ScalarArray getArray(final Stack arguments) {
	
		final Scalar s = BridgeUtilities.getScalar(arguments);
		if (s.getArray() == null) {
			return SleepUtils.getArrayScalar().getArray();
		}
		
		return s.getArray();
	}
	
	/**
	 * grab a sleep hash, if the stack is empty a scalar hash with no members
	 * will be returned.
	 */
	public static ScalarHash getHash(final Stack arguments) {
	
		if (arguments.isEmpty()) {
			return SleepUtils.getHashScalar().getHash();
		}
		
		return ((Scalar) arguments.pop()).getHash();
	}
	
	/**
	 * grab a sleep array, if the grabbed array is a readonly array, a copy is
	 * returned. if the stack is empty an array with no elements will be
	 * returned.
	 */
	public static ScalarArray getWorkableArray(final Stack arguments) {
	
		if (arguments.isEmpty()) {
			return SleepUtils.getArrayScalar().getArray();
		}
		
		final Scalar temp = (Scalar) arguments.pop();
		
		if (temp.getArray().getClass() == sleep.runtime.CollectionWrapper.class) {
			final ScalarArray array = SleepUtils.getArrayScalar().getArray();
			final Iterator i = temp.getArray().scalarIterator();
			while(i.hasNext()) {
				array.push((Scalar) i.next());
			}
			
			return array;
		}
		
		return temp.getArray();
	}
	
	/** grab an object, if the stack is empty then null will be returned. */
	public static Object getObject(final Stack arguments) {
	
		if (arguments.isEmpty()) {
			return null;
		}
		
		return ((Scalar) arguments.pop()).objectValue();
	}
	
	/**
	 * retrieves an executable Function object from the stack. Functions can be
	 * passed as closures or as a reference to a built-in Sleep subroutine i.e.
	 * &my_func.
	 */
	public static SleepClosure getFunction(final Stack arguments, final ScriptInstance script) {
	
		final Scalar temp = BridgeUtilities.getScalar(arguments);
		final SleepClosure func = SleepUtils.getFunctionFromScalar(temp, script);
		
		if (func == null) {
			throw new IllegalArgumentException("expected &closure--received: " + SleepUtils.describe(temp));
		}
		
		return func;
	}
	
	/**
	 * grab a scalar, if the stack is empty the empty/null scalar will be
	 * returned.
	 */
	public static Scalar getScalar(final Stack arguments) {
	
		if (arguments.isEmpty()) {
			return SleepUtils.getEmptyScalar();
		}
		
		return (Scalar) arguments.pop();
	}
	
	/**
	 * grab a string, if the stack is empty or if the value is null the default
	 * value will be returned.
	 */
	public static String getString(final Stack arguments, final String defaultValue) {
	
		if (arguments.isEmpty()) {
			return defaultValue;
		}
		
		final String temp = arguments.pop().toString();
		
		if (temp == null) {
			return defaultValue;
		}
		
		return temp;
	}
	
	private static final boolean doReplace = File.separatorChar != '/';
	
	/**
	 * adjusts the file argument to accomodate for the current working directory
	 */
	public static File toSleepFile(String text, final ScriptInstance i) {
	
		if (text == null) {
			return i.cwd();
		} else if (BridgeUtilities.doReplace) {
			text = text.replace('/', File.separatorChar);
		}
		
		final File f = new File(text);
		
		if (!f.isAbsolute() && text.length() > 0) {
			return new File(i.cwd(), text);
		} else {
			return f;
		}
	}
	
	/**
	 * returns a File object from a string argument, the path in the string
	 * argument is transformed such that the character / will refer to the
	 * correct path separator for the current OS. Returns null if no file is
	 * specified as an argument.
	 */
	public static File getFile(final Stack arguments, final ScriptInstance i) {
	
		return BridgeUtilities.toSleepFile(arguments.isEmpty() ? null : arguments.pop().toString(), i);
	}
	
	/**
	 * Pops a Key/Value pair object off of the argument stack. A Key/Value pair
	 * is created using the => operator within Sleep scripts. If the top
	 * argument on this stack was not created using =>, this function will try
	 * to parse a key/value pair using the pattern: [key]=[value]
	 */
	public static KeyValuePair getKeyValuePair(final Stack arguments) {
	
		final Scalar temps = BridgeUtilities.getScalar(arguments);
		
		if (temps.objectValue() != null && temps.objectValue().getClass() == sleep.bridges.KeyValuePair.class) {
			return (KeyValuePair) temps.objectValue();
		}
		
		if (temps.getActualValue() != null) {
			Scalar key, value;
			final String temp = temps.getActualValue().toString();
			
			if (temp.indexOf('=') > -1) {
				key = SleepUtils.getScalar(temp.substring(0, temp.indexOf('=')));
				value = SleepUtils.getScalar(temp.substring(temp.indexOf('=') + 1, temp.length()));
				return new KeyValuePair(key, value);
			}
		}
		
		throw new IllegalArgumentException("attempted to pass a malformed key value pair: " + temps);
	}
	
	/**
	 * Flattens the specified scalar array. The <var>toValue</var> field can be
	 * null.
	 */
	public static Scalar flattenArray(final Scalar fromValue, final Scalar toValue) {
	
		return BridgeUtilities.flattenIterator(fromValue.getArray().scalarIterator(), toValue);
	}
	
	/**
	 * Flattens the specified arrays within the specified iterator. The
	 * <var>toValue</var> field can be null.
	 */
	public static Scalar flattenIterator(final Iterator i, Scalar toValue) {
	
		if (toValue == null) {
			toValue = SleepUtils.getArrayScalar();
		}
		
		while(i.hasNext()) {
			final Scalar temp = (Scalar) i.next();
			
			if (temp.getArray() != null) {
				BridgeUtilities.flattenArray(temp, toValue);
			} else {
				toValue.getArray().push(temp);
			}
		}
		
		return toValue;
	}
	
	/** initializes local scope based on argument stack */
	public static int initLocalScope(final ScriptVariables vars, final Variable localLevel, final Stack locals) {
	
		int name = 1;
		
		final Scalar args = SleepUtils.getArrayScalar();
		
		while(!locals.isEmpty()) {
			final Scalar lvar = (Scalar) locals.pop();
			
			if (lvar.getActualValue() != null && lvar.getActualValue().getType() == ObjectValue.class && lvar.getActualValue().objectValue() != null && lvar.getActualValue().objectValue().getClass() == KeyValuePair.class) {
				final KeyValuePair kvp = (KeyValuePair) lvar.getActualValue().objectValue();
				
				if (!sleep.parser.Checkers.isVariable(kvp.getKey().toString())) {
					throw new IllegalArgumentException("unreachable named parameter: " + kvp.getKey());
				} else {
					vars.setScalarLevel(kvp.getKey().toString(), kvp.getValue(), localLevel);
				}
			} else {
				args.getArray().push(lvar);
				vars.setScalarLevel("$" + name, lvar, localLevel);
				name++;
			}
		}
		
		vars.setScalarLevel("@_", args, localLevel);
		return name;
	}
	
	/** normalizes the index value based on the specified length */
	public static final int normalize(final int value, final int length) {
	
		return value < 0 ? value + length : value;
	}
	
	/**
	 * returns true if value is an array or throws an appropriate exception if
	 * value is not an array.
	 * 
	 * @param n
	 *            the name of the &amp;function
	 * @param value
	 *            the scalar to check
	 */
	public static boolean expectArray(final String n, final Scalar value) {
	
		if (value.getArray() == null) {
			throw new IllegalArgumentException(n + ": expected array. received " + SleepUtils.describe(value));
		}
		
		return true;
	}
}
