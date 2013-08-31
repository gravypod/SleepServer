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
package sleep.engine.atoms;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

import sleep.engine.CallRequest;
import sleep.engine.ObjectUtilities;
import sleep.engine.Step;
import sleep.runtime.Scalar;
import sleep.runtime.ScriptEnvironment;
import sleep.runtime.SleepUtils;

public class ObjectAccess extends Step {
	
	/**
     * 
     */
	private static final long serialVersionUID = -7729782646560321434L;
	
	protected String name;
	
	protected Class classRef;
	
	public ObjectAccess(final String _name, final Class _classRef) {
	
		name = _name;
		classRef = _classRef;
	}
	
	@Override
	public String toString() {
	
		return "[Object Access]: " + classRef + "#" + name + "\n";
	}
	
	private static class MethodCallRequest extends CallRequest {
		
		protected Method theMethod;
		
		protected Scalar scalar;
		
		protected String name;
		
		protected Class theClass;
		
		public MethodCallRequest(final ScriptEnvironment e, final int lineNo, final Method method, final Scalar _scalar, final String _name, final Class _class) {
		
			super(e, lineNo);
			theMethod = method;
			scalar = _scalar;
			name = _name;
			theClass = _class;
		}
		
		@Override
		public String getFunctionName() {
		
			return theMethod.toString();
		}
		
		@Override
		public String getFrameDescription() {
		
			return theMethod.toString();
		}
		
		@Override
		public String formatCall(String args) {
		
			final StringBuffer trace = new StringBuffer("[");
			
			if (args != null && args.length() > 0) {
				args = ": " + args;
			}
			
			if (scalar == null) {
				trace.append(theClass.getName() + " " + name + args + "]");
			} else {
				trace.append(SleepUtils.describe(scalar) + " " + name + args + "]");
			}
			
			return trace.toString();
		}
		
		@Override
		protected Scalar execute() {
		
			final Object[] parameters = ObjectUtilities.buildArgumentArray(theMethod.getParameterTypes(), getScriptEnvironment().getCurrentFrame(), getScriptEnvironment().getScriptInstance());
			
			try {
				return ObjectUtilities.BuildScalar(true, theMethod.invoke(scalar != null ? scalar.objectValue() : null, parameters));
			} catch (final InvocationTargetException ite) {
				if (ite.getCause() != null) {
					getScriptEnvironment().flagError(ite.getCause());
				}
				
				throw new RuntimeException(ite);
			} catch (final IllegalArgumentException aex) {
				aex.printStackTrace();
				getScriptEnvironment().getScriptInstance().fireWarning(ObjectUtilities.buildArgumentErrorMessage(theClass, name, theMethod.getParameterTypes(), parameters), getLineNumber());
			} catch (final IllegalAccessException iax) {
				getScriptEnvironment().getScriptInstance().fireWarning("cannot access " + name + " in " + theClass + ": " + iax.getMessage(), getLineNumber());
			}
			
			return SleepUtils.getEmptyScalar();
		}
	}
	
	//
	// Pre Condition:
	//   object we're accessing is top item on current frame
	//   arguments consist of the rest of the current frame...
	//
	// Post Condition:
	//   current frame is dissolved
	//   result is top item on parent frame
	
	@Override
	public Scalar evaluate(final ScriptEnvironment e) {
	
		Object accessMe = null;
		Class theClass = null;
		Scalar scalar = null;
		
		if (classRef == null) {
			scalar = (Scalar) e.getCurrentFrame().pop();
			accessMe = scalar.objectValue();
			
			if (accessMe == null) {
				e.getScriptInstance().fireWarning("Attempted to call a non-static method on a null reference", getLineNumber());
				e.KillFrame();
				e.getCurrentFrame().push(SleepUtils.getEmptyScalar());
				
				return null;
			}
			
			theClass = accessMe.getClass();
		} else {
			theClass = classRef;
		}
		
		//
		// check if this is a closure, if it is, try to invoke stuff on it instead
		//
		
		if (scalar != null && SleepUtils.isFunctionScalar(scalar)) {
			final CallRequest.ClosureCallRequest request = new CallRequest.ClosureCallRequest(e, getLineNumber(), scalar, name);
			request.CallFunction();
			return null;
		}
		
		//
		// now we know we're not dealing with a closure; so before we go on the name field has to be non-null.
		//
		
		if (name == null) {
			e.getScriptInstance().fireWarning("Attempted to query an object with no method/field", getLineNumber());
			e.KillFrame();
			e.getCurrentFrame().push(SleepUtils.getEmptyScalar());
			
			return null;
		}
		
		Scalar result = SleepUtils.getEmptyScalar();
		
		//
		// try to invoke stuff on the object...
		//
		
		final Method theMethod = ObjectUtilities.findMethod(theClass, name, e.getCurrentFrame());
		
		if (theMethod != null && (classRef == null || (theMethod.getModifiers() & Modifier.STATIC) == Modifier.STATIC)) {
			try {
				theMethod.setAccessible(true);
			} catch (final Exception ex) {
			}
			
			final MethodCallRequest request = new MethodCallRequest(e, getLineNumber(), theMethod, scalar, name, theClass);
			request.CallFunction();
			return null;
		} else if (theMethod == null && !e.getCurrentFrame().isEmpty()) {
			e.getScriptInstance().fireWarning("there is no method that matches " + name + "(" + SleepUtils.describe(e.getCurrentFrame()) + ") in " + theClass.getName(), getLineNumber());
		} else {
			try {
				Field aField;
				
				try {
					aField = theClass.getDeclaredField(name);
				} catch (final NoSuchFieldException nsfe) {
					aField = theClass.getField(name);
				}
				
				if (aField != null) {
					try {
						aField.setAccessible(true);
					} catch (final Exception ex) {
					}
					
					result = ObjectUtilities.BuildScalar(true, aField.get(accessMe));
				} else {
					result = SleepUtils.getEmptyScalar();
				}
			} catch (final NoSuchFieldException fex) {
				e.getScriptInstance().fireWarning("no field/method named " + name + " in " + theClass, getLineNumber());
			} catch (final IllegalAccessException iax) {
				e.getScriptInstance().fireWarning("cannot access " + name + " in " + theClass + ": " + iax.getMessage(), getLineNumber());
			}
		}
		
		e.FrameResult(result);
		return null;
	}
}
