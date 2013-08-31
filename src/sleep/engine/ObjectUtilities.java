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
package sleep.engine;

import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.Iterator;
import java.util.Stack;

import sleep.bridges.BridgeUtilities;
import sleep.engine.types.DoubleValue;
import sleep.engine.types.IntValue;
import sleep.engine.types.LongValue;
import sleep.engine.types.ObjectValue;
import sleep.engine.types.StringValue;
import sleep.runtime.Scalar;
import sleep.runtime.ScalarArray;
import sleep.runtime.ScalarHash;
import sleep.runtime.ScriptEnvironment;
import sleep.runtime.ScriptInstance;
import sleep.runtime.SleepUtils;
import sleep.runtime.WatchScalar;

/**
 * This class is sort of the center of the HOES universe containing several
 * methods for mapping between Sleep and Java and resolving which mappings make
 * sense.
 */
public class ObjectUtilities {
	
	private static Class<StringValue> STRING_SCALAR;
	
	private static Class<IntValue> INT_SCALAR;
	
	private static Class<DoubleValue> DOUBLE_SCALAR;
	
	private static Class<LongValue> LONG_SCALAR;
	
	private static Class<ObjectValue> OBJECT_SCALAR;
	
	static {
		ObjectUtilities.STRING_SCALAR = sleep.engine.types.StringValue.class;
		ObjectUtilities.INT_SCALAR = sleep.engine.types.IntValue.class;
		ObjectUtilities.DOUBLE_SCALAR = sleep.engine.types.DoubleValue.class;
		ObjectUtilities.LONG_SCALAR = sleep.engine.types.LongValue.class;
		ObjectUtilities.OBJECT_SCALAR = sleep.engine.types.ObjectValue.class;
	}
	
	/**
	 * when looking for a Java method that matches the sleep args, we use a Yes
	 * match immediately
	 */
	public static final int ARG_MATCH_YES = 3;
	
	/**
	 * when looking for a Java method that matches the sleep args, we
	 * immediately drop all of the no answers.
	 */
	public static final int ARG_MATCH_NO = 0;
	
	/**
	 * when looking for a Java method that matches the sleep args, we save the
	 * maybes and use them as a last resort if no yes match is found
	 */
	public static final int ARG_MATCH_MAYBE = 1;
	
	/**
	 * convienence method to determine wether or not the stack of values is a
	 * safe match for the specified method signature
	 */
	public static int isArgMatch(final Class<?>[] check, final Stack<Scalar> arguments) {
	
		int value = ObjectUtilities.ARG_MATCH_YES;
		
		for (int z = 0; z < check.length; z++) {
			final Scalar scalar = arguments.get(check.length - z - 1);
			
			value = value & ObjectUtilities.isArgMatch(check[z], scalar);
			
			//         System.out.println("Matching: " + scalar + "(" + scalar.getValue().getClass() + "): to " + check[z] + ": " + value);
			
			if (value == ObjectUtilities.ARG_MATCH_NO) {
				return ObjectUtilities.ARG_MATCH_NO;
			}
		}
		
		return value;
	}
	
	/**
	 * converts the primitive version of the specified class to a regular usable
	 * version
	 */
	private static Class<?> normalizePrimitive(Class<?> check) {
	
		if (check == Integer.TYPE) {
			check = Integer.class;
		} else if (check == Double.TYPE) {
			check = Double.class;
		} else if (check == Long.TYPE) {
			check = Long.class;
		} else if (check == Float.TYPE) {
			check = Float.class;
		} else if (check == Boolean.TYPE) {
			check = Boolean.class;
		} else if (check == Byte.TYPE) {
			check = Byte.class;
		} else if (check == Character.TYPE) {
			check = Character.class;
		} else if (check == Short.TYPE) {
			check = Short.class;
		}
		
		return check;
	}
	
	/**
	 * determined if the specified scalar can be rightfully cast to the
	 * specified class
	 */
	public static int isArgMatch(Class<?> check, final Scalar scalar) {
	
		if (SleepUtils.isEmptyScalar(scalar)) {
			return ObjectUtilities.ARG_MATCH_YES;
		} else if (scalar.getArray() != null) {
			if (check.isArray()) {
				Class<?> compType = check.getComponentType(); /* find the actual nuts and bolts component type so we can work with it */
				while(compType.isArray()) {
					compType = compType.getComponentType();
				}
				
				final Class<?> mytype = ObjectUtilities.getArrayType(scalar, null);
				
				if (mytype != null && compType.isAssignableFrom(mytype)) {
					return ObjectUtilities.ARG_MATCH_YES;
				} else {
					return ObjectUtilities.ARG_MATCH_NO;
				}
			} else if (check == java.util.List.class || check == java.util.Collection.class) {
				// would a java.util.List or java.util.Collection satisfy the argument?
				return ObjectUtilities.ARG_MATCH_YES;
			} else if (check == ScalarArray.class) {
				return ObjectUtilities.ARG_MATCH_YES;
			} else if (check == java.lang.Object.class) {
				return ObjectUtilities.ARG_MATCH_MAYBE;
			} else {
				return ObjectUtilities.ARG_MATCH_NO;
			}
		} else if (scalar.getHash() != null) {
			if (check == java.util.Map.class) {
				// would a java.util.Map or java.util.Collection satisfy the argument?
				return ObjectUtilities.ARG_MATCH_YES;
			} else if (check == ScalarHash.class) {
				return ObjectUtilities.ARG_MATCH_YES;
			} else if (check == java.lang.Object.class) {
				return ObjectUtilities.ARG_MATCH_MAYBE;
			} else {
				return ObjectUtilities.ARG_MATCH_NO;
			}
		} else if (check.isPrimitive()) {
			final Class<?> stemp = scalar.getActualValue().getType(); /* at this point we know scalar is not null, not a hash, and not an array */
			
			if (stemp == ObjectUtilities.INT_SCALAR && check == Integer.TYPE) {
				return ObjectUtilities.ARG_MATCH_YES;
			} else if (stemp == ObjectUtilities.DOUBLE_SCALAR && check == Double.TYPE) {
				return ObjectUtilities.ARG_MATCH_YES;
			} else if (stemp == ObjectUtilities.LONG_SCALAR && check == Long.TYPE) {
				return ObjectUtilities.ARG_MATCH_YES;
			} else if (check == Character.TYPE && stemp == ObjectUtilities.STRING_SCALAR && scalar.getActualValue().toString().length() == 1) {
				return ObjectUtilities.ARG_MATCH_YES;
			} else if (stemp == ObjectUtilities.OBJECT_SCALAR) {
				check = ObjectUtilities.normalizePrimitive(check);
				return scalar.objectValue().getClass() == check ? ObjectUtilities.ARG_MATCH_YES : ObjectUtilities.ARG_MATCH_NO;
			} else {
				/* this is my lazy way of saying allow Long, Int, and Double scalar types to be considered
				   maybes... */
				return stemp == ObjectUtilities.STRING_SCALAR ? ObjectUtilities.ARG_MATCH_NO : ObjectUtilities.ARG_MATCH_MAYBE;
			}
		} else if (check.isInterface()) {
			if (SleepUtils.isFunctionScalar(scalar) || check.isInstance(scalar.objectValue())) {
				return ObjectUtilities.ARG_MATCH_YES;
			} else {
				return ObjectUtilities.ARG_MATCH_NO;
			}
		} else if (check == String.class) {
			final Class<?> stemp = scalar.getActualValue().getType();
			return stemp == ObjectUtilities.STRING_SCALAR ? ObjectUtilities.ARG_MATCH_YES : ObjectUtilities.ARG_MATCH_MAYBE;
		} else if (check == Object.class) {
			return ObjectUtilities.ARG_MATCH_MAYBE; /* we're vying for anything and this will match anything */
		} else if (check.isInstance(scalar.objectValue())) {
			final Class<?> stemp = scalar.getActualValue().getType();
			return stemp == ObjectUtilities.OBJECT_SCALAR ? ObjectUtilities.ARG_MATCH_YES : ObjectUtilities.ARG_MATCH_MAYBE;
		} else if (check.isArray()) {
			final Class<?> stemp = scalar.getActualValue().getType();
			if (stemp == ObjectUtilities.STRING_SCALAR && (check.getComponentType() == Character.TYPE || check.getComponentType() == Byte.TYPE)) {
				return ObjectUtilities.ARG_MATCH_MAYBE;
			} else {
				return ObjectUtilities.ARG_MATCH_NO;
			}
		} else {
			return ObjectUtilities.ARG_MATCH_NO;
		}
	}
	
	/**
	 * attempts to find the method that is the closest match to the specified
	 * arguments
	 */
	public static Method findMethod(final Class<?> theClass, final String method, final Stack arguments) {
	
		final int size = arguments.size();
		
		Method temp = null;
		final Method[] methods = theClass.getMethods();
		
		for (final Method method2 : methods) {
			if (method2.getName().equals(method) && method2.getParameterTypes().length == size) {
				if (size == 0) {
					return method2;
				}
				
				final int value = ObjectUtilities.isArgMatch(method2.getParameterTypes(), arguments);
				if (value == ObjectUtilities.ARG_MATCH_YES) {
					return method2;
				}
				
				if (value == ObjectUtilities.ARG_MATCH_MAYBE) {
					temp = method2;
				}
			}
		}
		
		return temp;
	}
	
	/**
	 * attempts to find the constructor that is the closest match to the
	 * arguments
	 */
	public static Constructor findConstructor(final Class theClass, final Stack arguments) {
	
		final int size = arguments.size();
		
		Constructor temp = null;
		final Constructor[] constructors = theClass.getConstructors();
		
		for (final Constructor constructor : constructors) {
			if (constructor.getParameterTypes().length == size) {
				if (size == 0) {
					return constructor;
				}
				
				final int value = ObjectUtilities.isArgMatch(constructor.getParameterTypes(), arguments);
				if (value == ObjectUtilities.ARG_MATCH_YES) {
					return constructor;
				}
				
				if (value == ObjectUtilities.ARG_MATCH_MAYBE) {
					temp = constructor;
				}
			}
		}
		
		return temp;
	}
	
	/**
	 * this function checks if the specified scalar is a Class literal and uses
	 * that if it is, otherwise description is converted to a string and the
	 * convertDescriptionToClass method is used
	 */
	public static Class convertScalarDescriptionToClass(final Scalar description) {
	
		if (description.objectValue() instanceof Class) {
			return (Class) description.objectValue();
		}
		
		return ObjectUtilities.convertDescriptionToClass(description.toString());
	}
	
	/**
	 * converts the one character class description to the specified Class type,
	 * i.e. z = boolean, c = char, b = byte, i = integer, etc..
	 */
	public static Class convertDescriptionToClass(final String description) {
	
		if (description.length() != 1) {
			return null;
		}
		
		Class atype = null;
		
		switch(description.charAt(0)) {
			case 'z':
				atype = Boolean.TYPE;
				break;
			case 'c':
				atype = Character.TYPE;
				break;
			case 'b':
				atype = Byte.TYPE;
				break;
			case 'h':
				atype = Short.TYPE;
				break;
			case 'i':
				atype = Integer.TYPE;
				break;
			case 'l':
				atype = Long.TYPE;
				break;
			case 'f':
				atype = Float.TYPE;
				break;
			case 'd':
				atype = Double.TYPE;
				break;
			case 'o':
				atype = Object.class;
				break;
			case '*':
				atype = null;
				break;
		}
		
		return atype;
	}
	
	/** marshalls the Sleep value into a Java value of the specified type. */
	public static Object buildArgument(final Class type, final Scalar value, final ScriptInstance script) {
	
		if (type == String.class) {
			return SleepUtils.isEmptyScalar(value) ? null : value.toString();
		} else if (value.getArray() != null) {
			if (type.isArray()) {
				final Class atype = ObjectUtilities.getArrayType(value, type.getComponentType());
				
				final Object arrayV = Array.newInstance(atype, value.getArray().size());
				final Iterator i = value.getArray().scalarIterator();
				int x = 0;
				while(i.hasNext()) {
					final Scalar temp = (Scalar) i.next();
					final Object blah = ObjectUtilities.buildArgument(atype, temp, script);
					
					if (blah == null && !atype.isPrimitive() || atype.isInstance(blah) || atype.isPrimitive()) {
						Array.set(arrayV, x, blah);
					} else {
						if (atype.isArray()) {
							throw new RuntimeException("incorrect dimensions for conversion to " + type);
						} else {
							throw new RuntimeException(SleepUtils.describe(temp) + " at " + x + " is not compatible with " + atype.getName());
						}
					}
					x++;
				}
				
				return arrayV;
			} else if (type == ScalarArray.class) {
				return value.objectValue();
			} else {
				return SleepUtils.getListFromArray(value);
			}
		} else if (value.getHash() != null) {
			if (type == ScalarHash.class) {
				return value.objectValue();
			} else {
				return SleepUtils.getMapFromHash(value);
			}
		} else if (type.isPrimitive()) {
			if (type == Boolean.TYPE) {
				return Boolean.valueOf(value.intValue() != 0);
			} else if (type == Byte.TYPE) {
				return new Byte((byte) value.intValue());
			} else if (type == Character.TYPE) {
				return new Character(value.toString().charAt(0));
			} else if (type == Double.TYPE) {
				return new Double(value.doubleValue());
			} else if (type == Float.TYPE) {
				return new Float((float) value.doubleValue());
			} else if (type == Integer.TYPE) {
				return new Integer(value.intValue());
			} else if (type == Short.TYPE) {
				return new Short((short) value.intValue());
			} else if (type == Long.TYPE) {
				return new Long(value.longValue());
			}
		} else if (SleepUtils.isEmptyScalar(value)) {
			return null;
		} else if (type.isArray() && value.getActualValue().getType() == sleep.engine.types.StringValue.class) {
			if (type.getComponentType() == Byte.TYPE || type.getComponentType() == Byte.class) {
				return BridgeUtilities.toByteArrayNoConversion(value.toString());
			} else if (type.getComponentType() == Character.TYPE || type.getComponentType() == Character.class) {
				return value.toString().toCharArray();
			}
		} else if (type.isInterface() && SleepUtils.isFunctionScalar(value)) {
			return ProxyInterface.BuildInterface(type, SleepUtils.getFunctionFromScalar(value, script), script);
		}
		
		return value.objectValue();
	}
	
	/**
	 * utility to create a string representation of an incompatible argument
	 * choice
	 */
	public static String buildArgumentErrorMessage(final Class theClass, final String method, final Class[] expected, final Object[] parameters) {
	
		final StringBuffer tempa = new StringBuffer(method + "(");
		
		for (int x = 0; x < expected.length; x++) {
			tempa.append(expected[x].getName());
			
			if (x + 1 < expected.length) {
				tempa.append(", ");
			}
		}
		tempa.append(")");
		
		final StringBuffer tempb = new StringBuffer("(");
		for (int x = 0; x < parameters.length; x++) {
			if (parameters[x] != null) {
				tempb.append(parameters[x].getClass().getName());
			} else {
				tempb.append("null");
			}
			
			if (x + 1 < parameters.length) {
				tempb.append(", ");
			}
		}
		tempb.append(")");
		
		return "bad arguments " + tempb.toString() + " for " + tempa.toString() + " in " + theClass;
	}
	
	/**
	 * populates a Java array with Sleep values marshalled into values of the
	 * specified types.
	 */
	public static Object[] buildArgumentArray(final Class[] types, final Stack arguments, final ScriptInstance script) {
	
		final Object[] parameters = new Object[types.length];
		
		for (int x = 0; x < parameters.length; x++) {
			final Scalar temp = (Scalar) arguments.pop();
			parameters[x] = ObjectUtilities.buildArgument(types[x], temp, script);
		}
		
		return parameters;
	}
	
	/**
	 * marshalls a Java type into the appropriate Sleep scalar. The primitives
	 * value will force this method to also check if the Java type could map to
	 * an int, long, double, etc. Use true when in doubt.
	 */
	public static Scalar BuildScalar(final boolean primitives, final Object value) {
	
		if (value == null) {
			return SleepUtils.getEmptyScalar();
		}
		
		Class check = value.getClass();
		
		if (check.isArray()) {
			if (check.getComponentType() == Byte.TYPE || check.getComponentType() == Byte.class) {
				return SleepUtils.getScalar((byte[]) value);
			} else if (check.getComponentType() == Character.TYPE || check.getComponentType() == Character.class) {
				return SleepUtils.getScalar(new String((char[]) value));
			} else {
				final Scalar array = SleepUtils.getArrayScalar();
				for (int x = 0; x < Array.getLength(value); x++) {
					array.getArray().push(ObjectUtilities.BuildScalar(true, Array.get(value, x)));
				}
				
				return array;
			}
		}
		
		if (primitives) {
			if (check.isPrimitive()) {
				check = ObjectUtilities.normalizePrimitive(check); /* just in case, shouldn't be needed typically */
			}
			
			if (check == Boolean.class) {
				return SleepUtils.getScalar(((Boolean) value).booleanValue() ? 1 : 0);
			} else if (check == Byte.class) {
				return SleepUtils.getScalar((int) ((Byte) value).byteValue());
			} else if (check == Character.class) {
				return SleepUtils.getScalar(value.toString());
			} else if (check == Double.class) {
				return SleepUtils.getScalar(((Double) value).doubleValue());
			} else if (check == Float.class) {
				return SleepUtils.getScalar((double) ((Float) value).floatValue());
			} else if (check == Integer.class) {
				return SleepUtils.getScalar(((Integer) value).intValue());
			} else if (check == Long.class) {
				return SleepUtils.getScalar(((Long) value).longValue());
			}
		}
		
		if (check == String.class) {
			return SleepUtils.getScalar(value.toString());
		} else if (check == Scalar.class || check == WatchScalar.class) {
			return (Scalar) value;
		} else {
			return SleepUtils.getScalar(value);
		}
	}
	
	/**
	 * Determines the primitive type of the specified array. Primitive Sleep
	 * values (int, long, double) will return the appropriate Number.TYPE class.
	 * This is an important distinction as Double.TYPE != new
	 * Double().getClass()
	 */
	public static Class getArrayType(final Scalar value, final Class defaultc) {
	
		if (value.getArray() != null && value.getArray().size() > 0 && (defaultc == null || defaultc == Object.class)) {
			for (int x = 0; x < value.getArray().size(); x++) {
				if (value.getArray().getAt(x).getArray() != null) {
					return ObjectUtilities.getArrayType(value.getArray().getAt(x), defaultc);
				}
				
				final Class elem = value.getArray().getAt(x).getValue().getClass();
				final Object tempo = value.getArray().getAt(x).objectValue();
				
				if (elem == ObjectUtilities.DOUBLE_SCALAR) {
					return Double.TYPE;
				} else if (elem == ObjectUtilities.INT_SCALAR) {
					return Integer.TYPE;
				} else if (elem == ObjectUtilities.LONG_SCALAR) {
					return Long.TYPE;
				} else if (tempo != null) {
					return tempo.getClass();
				}
			}
		}
		
		return defaultc;
	}
	
	/**
	 * Standard method to handle a Java exception from a HOES call. Basically
	 * this places the exception into Sleep's throw mechanism and collects the
	 * stack frame.
	 */
	public static void handleExceptionFromJava(final Throwable ex, final ScriptEnvironment env, final String description, final int lineNumber) {
	
		if (ex != null) {
			env.flagError(ex);
			
			if (env.isThrownValue() && description != null && description.length() > 0) {
				env.getScriptInstance().recordStackFrame(description, lineNumber);
			}
		}
	}
}
