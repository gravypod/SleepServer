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

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Stack;

import sleep.bridges.SleepClosure;
import sleep.interfaces.Function;
import sleep.runtime.Scalar;
import sleep.runtime.ScriptInstance;
import sleep.runtime.SleepUtils;

/**
 * This class is used to mock an instance of a class that implements a specified
 * Java interface using a Sleep function.
 */
public class ProxyInterface implements InvocationHandler {
	
	protected ScriptInstance script;
	
	protected Function func;
	
	public ProxyInterface(final Function _method, final ScriptInstance _script) {
	
		func = _method;
		script = _script;
	}
	
	/** Returns the script associated with this proxy interface. */
	public ScriptInstance getOwner() {
	
		return script;
	}
	
	/** Returns a string description of this proxy interface */
	@Override
	public String toString() {
	
		return func.toString();
	}
	
	/**
	 * Constructs a new instance of the specified class that uses the passed
	 * Sleep function to respond to all method calls on this instance.
	 */
	public static Object BuildInterface(final Class<?> className, final Function subroutine, final ScriptInstance script) {
	
		return ProxyInterface.BuildInterface(new Class[] { className }, subroutine, script);
	}
	
	/**
	 * Constructs a new instance of the specified class that uses the passed
	 * Sleep function to respond to all method calls on this instance.
	 */
	public static Object BuildInterface(final Class<?> classes[], final Function subroutine, final ScriptInstance script) {
	
		final InvocationHandler temp = new ProxyInterface(subroutine, script);
		return Proxy.newProxyInstance(classes[0].getClassLoader(), classes, temp);
	}
	
	/**
	 * Constructs a new instance of the specified class that uses the passed
	 * block to respond to all method calls on this instance.
	 */
	public static Object BuildInterface(final Class<?> className, final Block block, final ScriptInstance script) {
	
		return ProxyInterface.BuildInterface(className, new SleepClosure(script, block), script);
	}
	
	/**
	 * Constructs a new instance of the specified class that uses the passed
	 * block to respond to all method calls on this instance.
	 */
	public static Object BuildInterface(final Class<?> classes[], final Block block, final ScriptInstance script) {
	
		return ProxyInterface.BuildInterface(classes, new SleepClosure(script, block), script);
	}
	
	/**
	 * This function invokes the contained Sleep closure with the specified
	 * arguments
	 */
	@Override
	public Object invoke(final Object proxy, final Method method, final Object[] args) throws Throwable {
	
		synchronized(script.getScriptVariables()) {
			script.getScriptEnvironment().pushSource("<Java>");
			
			final Stack<Scalar> temp = new Stack<Scalar>();
			
			final boolean isTrace = (script.getDebugFlags() & ScriptInstance.DEBUG_TRACE_CALLS) == ScriptInstance.DEBUG_TRACE_CALLS;
			StringBuffer message = null;
			
			if (args != null) {
				for (int z = args.length - 1; z >= 0; z--) {
					temp.push(ObjectUtilities.BuildScalar(true, args[z]));
				}
			}
			
			Scalar value;
			
			script.getScriptEnvironment().installExceptionHandler(null, null, null);
			
			if (isTrace) {
				if (!script.isProfileOnly()) {
					message = new StringBuffer("[" + func + " " + method.getName());
					
					if (!temp.isEmpty()) {
						message.append(": " + SleepUtils.describe(temp));
					}
					
					message.append("]");
				}
				
				long stat = System.currentTimeMillis();
				value = func.evaluate(method.getName(), script, temp);
				stat = System.currentTimeMillis() - stat;
				
				if (func.getClass() == SleepClosure.class) {
					script.collect(((SleepClosure) func).toStringGeneric(), -1, stat);
				}
				
				if (message != null) {
					if (script.getScriptEnvironment().isThrownValue()) {
						message.append(" - FAILED!");
					} else {
						message.append(" = " + SleepUtils.describe(value));
					}
					
					script.fireWarning(message.toString(), -1, true);
				}
			} else {
				value = func.evaluate(method.getName(), script, temp);
			}
			script.getScriptEnvironment().popExceptionContext();
			script.getScriptEnvironment().clearReturn();
			script.getScriptEnvironment().popSource();
			
			if (script.getScriptEnvironment().isThrownValue()) {
				script.recordStackFrame(func + " as " + method.toString(), "<Java>", -1);
				
				final Object exvalue = script.getScriptEnvironment().getExceptionMessage().objectValue();
				
				if (exvalue instanceof Throwable) {
					throw (Throwable) exvalue;
				} else {
					throw new RuntimeException(exvalue.toString());
				}
			}
			
			if (value != null) {
				return ObjectUtilities.buildArgument(method.getReturnType(), value, script);
			}
			
			return null;
		}
	}
}
