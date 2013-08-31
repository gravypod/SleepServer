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

import java.util.Iterator;

import sleep.bridges.SleepClosure;
import sleep.interfaces.Function;
import sleep.interfaces.Variable;
import sleep.runtime.Scalar;
import sleep.runtime.ScriptEnvironment;
import sleep.runtime.ScriptInstance;
import sleep.runtime.ScriptVariables;
import sleep.runtime.SleepUtils;

/**
 * This class encapsulates a function call request. Sleep has too many reasons,
 * places, and ways to call functions. This class helps to avoid duplicate code
 * and manage the complexity of Sleep's myriad of profiling, tracing, and error
 * reporting options.
 * 
 * This functionality is encapsulated (along with necessary setup/teardown that
 * you don't want to touch) within
 * {@linkplain sleep.runtime.SleepUtils#runCode(sleep.engine.Block, sleep.runtime.ScriptEnvironment)
 * SleepUtils.runCode()}.
 * 
 * @see sleep.runtime.SleepUtils
 */
public abstract class CallRequest {
	
	protected ScriptEnvironment environment;
	
	protected int lineNumber;
	
	/** initialize a new call request */
	public CallRequest(final ScriptEnvironment e, final int lineNo) {
	
		environment = e;
		lineNumber = lineNo;
	}
	
	/** returns the script environment... pHEAR */
	protected ScriptEnvironment getScriptEnvironment() {
	
		return environment;
	}
	
	/** returns the line number this function call is occuring from */
	public int getLineNumber() {
	
		return lineNumber;
	}
	
	/** return the name of the function (for use in profiler statistics) */
	public abstract String getFunctionName();
	
	/**
	 * return the description of this current stack frame in the event of an
	 * exception
	 */
	public abstract String getFrameDescription();
	
	/** execute the function call contained here */
	protected abstract Scalar execute();
	
	/**
	 * return a string view of this function call for trace messages; arguments
	 * are captured as comma separated descriptions of all args
	 */
	protected abstract String formatCall(final String args);
	
	/**
	 * return true if debug trace is enabled. override this to add/change
	 * criteria for trace activiation
	 */
	public boolean isDebug() {
	
		return (getScriptEnvironment().getScriptInstance().getDebugFlags() & ScriptInstance.DEBUG_TRACE_CALLS) == ScriptInstance.DEBUG_TRACE_CALLS;
	}
	
	/** actually execute the function call */
	public void CallFunction() {
	
		Scalar temp = null;
		final ScriptEnvironment e = getScriptEnvironment();
		final int mark = getScriptEnvironment().markFrame();
		
		if (isDebug() && getLineNumber() != Integer.MIN_VALUE) {
			if (e.getScriptInstance().isProfileOnly()) {
				try {
					final long total = e.getScriptInstance().total();
					long stat = System.currentTimeMillis();
					temp = execute();
					stat = System.currentTimeMillis() - stat - (e.getScriptInstance().total() - total);
					e.getScriptInstance().collect(getFunctionName(), getLineNumber(), stat);
				} catch (final RuntimeException rex) {
					if (rex.getCause() == null || !java.lang.reflect.InvocationTargetException.class.isInstance(rex.getCause())) {
						/* swallow invocation target exceptions please */
						
						e.cleanFrame(mark);
						e.KillFrame();
						throw rex;
					}
				}
			} else {
				final String args = SleepUtils.describe(e.getCurrentFrame());
				
				try {
					final long total = e.getScriptInstance().total();
					long stat = System.currentTimeMillis();
					temp = execute();
					stat = System.currentTimeMillis() - stat - (e.getScriptInstance().total() - total);
					e.getScriptInstance().collect(getFunctionName(), getLineNumber(), stat);
					
					if (e.isThrownValue()) {
						e.getScriptInstance().fireWarning(formatCall(args) + " - FAILED!", getLineNumber(), true);
					} else if (e.isPassControl()) {
						e.getScriptInstance().fireWarning(formatCall(args) + " -goto- " + SleepUtils.describe(temp), getLineNumber(), true);
					} else if (SleepUtils.isEmptyScalar(temp)) {
						e.getScriptInstance().fireWarning(formatCall(args), getLineNumber(), true);
					} else {
						e.getScriptInstance().fireWarning(formatCall(args) + " = " + SleepUtils.describe(temp), getLineNumber(), true);
					}
				} catch (final RuntimeException rex) {
					e.getScriptInstance().fireWarning(formatCall(args) + " - FAILED!", getLineNumber(), true);
					
					if (rex.getCause() == null || !java.lang.reflect.InvocationTargetException.class.isInstance(rex.getCause())) {
						/* swallow invocation target exceptions please */
						
						e.cleanFrame(mark);
						e.KillFrame();
						throw rex;
					}
				}
			}
		} else {
			try {
				temp = execute();
			} catch (final RuntimeException rex) {
				if (rex.getCause() == null || !java.lang.reflect.InvocationTargetException.class.isInstance(rex.getCause())) {
					/* swallow invocation target exceptions please */
					
					e.cleanFrame(mark);
					e.KillFrame();
					throw rex;
				}
			}
		}
		
		if (e.isThrownValue()) {
			e.getScriptInstance().recordStackFrame(getFrameDescription(), getLineNumber());
		}
		
		if (temp == null) {
			temp = SleepUtils.getEmptyScalar();
		}
		
		e.cleanFrame(mark);
		e.FrameResult(temp);
		
		/* if you're digging here then you've discovered my dirty little secret.  My continuation's continue to possess Java stack frames until
		   something decides to return.  Moving this check into Block.java overcomes this limitation except it makes it so continuations don't work
		   in code invoked outside of a Call instruction enclosed within Block.java.  If this is an issue email me and I'll look at better ways to
		   eliminate this problem. */
		
		if (e.isPassControl()) {
			final Scalar callme = temp;
			
			e.pushSource(((SleepClosure) callme.objectValue()).getAndRemoveMetadata("sourceFile", "<unknown>") + "");
			final int lno = ((Integer) ((SleepClosure) callme.objectValue()).getAndRemoveMetadata("sourceLine", new Integer(-1))).intValue();
			
			if (e.markFrame() >= 0) {
				final Object check = e.getCurrentFrame().pop(); /* get rid of the function that we're going to callcc */
				
				if (check != temp) {
					e.getScriptInstance().fireWarning("bad callcc stack: " + SleepUtils.describe((Scalar) check) + " expected " + SleepUtils.describe(temp), lno);
				}
			}
			
			e.flagReturn(null, ScriptEnvironment.FLOW_CONTROL_NONE);
			
			e.CreateFrame(); /* create a frame because the function call will destroy it */
			
			/**
			 * pass the continuation as the first argument to the callcc'd
			 * closure
			 */
			e.getCurrentFrame().push(((SleepClosure) callme.objectValue()).getAndRemoveMetadata("continuation", null));
			
			final CallRequest.ClosureCallRequest request = new CallRequest.ClosureCallRequest(environment, lno, callme, "CALLCC");
			request.CallFunction();
			
			e.popSource();
		}
	}
	
	/** execute a closure with all of the trimmings. */
	public static class ClosureCallRequest extends CallRequest {
		
		protected String name;
		
		protected Scalar scalar;
		
		public ClosureCallRequest(final ScriptEnvironment e, final int lineNo, final Scalar _scalar, final String _name) {
		
			super(e, lineNo);
			scalar = _scalar;
			name = _name;
		}
		
		@Override
		public String getFunctionName() {
		
			return ((SleepClosure) scalar.objectValue()).toStringGeneric();
		}
		
		@Override
		public String getFrameDescription() {
		
			return scalar.toString();
		}
		
		@Override
		public String formatCall(final String args) {
		
			final StringBuffer buffer = new StringBuffer("[" + SleepUtils.describe(scalar));
			
			if (name != null && name.length() > 0) {
				buffer.append(" " + name);
			}
			
			if (args.length() > 0) {
				buffer.append(": " + args);
			}
			
			buffer.append("]");
			
			return buffer.toString();
		}
		
		@Override
		protected Scalar execute() {
		
			final Function func = SleepUtils.getFunctionFromScalar(scalar, getScriptEnvironment().getScriptInstance());
			
			Scalar result;
			result = func.evaluate(name, getScriptEnvironment().getScriptInstance(), getScriptEnvironment().getCurrentFrame());
			getScriptEnvironment().clearReturn();
			return result;
		}
	}
	
	/** execute a function with all of the debug, trace, etc.. support */
	public static class FunctionCallRequest extends CallRequest {
		
		protected String function;
		
		protected Function callme;
		
		public FunctionCallRequest(final ScriptEnvironment e, final int lineNo, final String functionName, final Function f) {
		
			super(e, lineNo);
			function = functionName;
			callme = f;
		}
		
		@Override
		public String getFunctionName() {
		
			return function;
		}
		
		@Override
		public String getFrameDescription() {
		
			return function + "()";
		}
		
		@Override
		public String formatCall(final String args) {
		
			return function + "(" + args + ")";
		}
		
		@Override
		public boolean isDebug() {
		
			return super.isDebug() && !function.equals("&@") && !function.equals("&%") && !function.equals("&warn");
		}
		
		@Override
		protected Scalar execute() {
		
			final Scalar temp = callme.evaluate(function, getScriptEnvironment().getScriptInstance(), getScriptEnvironment().getCurrentFrame());
			getScriptEnvironment().clearReturn();
			return temp;
		}
	}
	
	/**
	 * execute a block of code inline with all the profiling, tracing, and other
	 * support
	 */
	public static class InlineCallRequest extends CallRequest {
		
		protected String function;
		
		protected Block inline;
		
		public InlineCallRequest(final ScriptEnvironment e, final int lineNo, final String functionName, final Block i) {
		
			super(e, lineNo);
			function = functionName;
			inline = i;
		}
		
		@Override
		public String getFunctionName() {
		
			return "<inline> " + function;
		}
		
		@Override
		public String getFrameDescription() {
		
			return "<inline> " + function + "()";
		}
		
		@Override
		protected String formatCall(final String args) {
		
			return "<inline> " + function + "(" + args + ")";
		}
		
		@Override
		protected Scalar execute() {
		
			final ScriptVariables vars = getScriptEnvironment().getScriptVariables();
			synchronized(vars) {
				final Variable localLevel = vars.getLocalVariables();
				final Scalar oldargs = localLevel.getScalar("@_"); /* save the current local variables */
				
				final int targs = sleep.bridges.BridgeUtilities.initLocalScope(vars, localLevel, getScriptEnvironment().getCurrentFrame());
				final Scalar eval = inline.evaluate(getScriptEnvironment());
				
				/* restore the argument variables */
				if (oldargs != null && oldargs.getArray() != null) {
					localLevel.putScalar("@_", oldargs);
					if (targs > 0) {
						final Iterator i = oldargs.getArray().scalarIterator();
						int count = 1;
						while(i.hasNext() && count <= targs) {
							final Scalar temp = (Scalar) i.next();
							localLevel.putScalar("$" + count, temp);
							count++;
						}
					}
				}
				return eval;
			}
		}
	}
}
