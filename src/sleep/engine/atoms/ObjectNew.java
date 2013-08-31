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

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

import sleep.engine.CallRequest;
import sleep.engine.ObjectUtilities;
import sleep.engine.Step;
import sleep.runtime.Scalar;
import sleep.runtime.ScriptEnvironment;
import sleep.runtime.SleepUtils;

public class ObjectNew extends Step {
	
	/**
     * 
     */
	private static final long serialVersionUID = -3546671562778485616L;
	
	protected Class name;
	
	public ObjectNew(final Class _name) {
	
		name = _name;
	}
	
	@Override
	public String toString() {
	
		return "[Object New]: " + name + "\n";
	}
	
	private static class ConstructorCallRequest extends CallRequest {
		
		protected Constructor theConstructor;
		
		protected Class name;
		
		public ConstructorCallRequest(final ScriptEnvironment e, final int lineNo, final Constructor cont, final Class _name) {
		
			super(e, lineNo);
			theConstructor = cont;
			name = _name;
		}
		
		@Override
		public String getFunctionName() {
		
			return name.toString();
		}
		
		@Override
		public String getFrameDescription() {
		
			return name.toString();
		}
		
		@Override
		public String formatCall(String args) {
		
			if (args != null && args.length() > 0) {
				args = ": " + args;
			}
			final StringBuffer trace = new StringBuffer("[new " + name.getName() + args + "]");
			
			return trace.toString();
		}
		
		@Override
		protected Scalar execute() {
		
			final Object[] parameters = ObjectUtilities.buildArgumentArray(theConstructor.getParameterTypes(), getScriptEnvironment().getCurrentFrame(), getScriptEnvironment().getScriptInstance());
			
			try {
				return ObjectUtilities.BuildScalar(false, theConstructor.newInstance(parameters));
			} catch (final InvocationTargetException ite) {
				if (ite.getCause() != null) {
					getScriptEnvironment().flagError(ite.getCause());
				}
				
				throw new RuntimeException(ite);
			} catch (final IllegalArgumentException aex) {
				aex.printStackTrace();
				getScriptEnvironment().getScriptInstance().fireWarning(ObjectUtilities.buildArgumentErrorMessage(name, name.getName(), theConstructor.getParameterTypes(), parameters), getLineNumber());
			} catch (final InstantiationException iex) {
				getScriptEnvironment().getScriptInstance().fireWarning("unable to instantiate abstract class " + name.getName(), getLineNumber());
			} catch (final IllegalAccessException iax) {
				getScriptEnvironment().getScriptInstance().fireWarning("cannot access constructor in " + name.getName() + ": " + iax.getMessage(), getLineNumber());
			}
			
			return SleepUtils.getEmptyScalar();
		}
	}
	
	//
	// Pre Condition:
	//   arguments are on the current frame
	//
	// Post Condition:
	//   current frame dissolved
	//   new object is placed on parent frame
	
	@Override
	public Scalar evaluate(final ScriptEnvironment e) {
	
		Scalar result;
		final Constructor theConstructor = ObjectUtilities.findConstructor(name, e.getCurrentFrame());
		
		if (theConstructor != null) {
			try {
				theConstructor.setAccessible(true);
			} catch (final Exception ex) {
			}
			final ConstructorCallRequest request = new ConstructorCallRequest(e, getLineNumber(), theConstructor, name);
			request.CallFunction();
			return null;
		} else {
			e.getScriptInstance().fireWarning("no constructor matching " + name.getName() + "(" + SleepUtils.describe(e.getCurrentFrame()) + ")", getLineNumber());
			result = SleepUtils.getEmptyScalar();
			e.FrameResult(result);
		}
		
		return null;
	}
}
