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

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Stack;

import sleep.engine.Block;
import sleep.engine.CallRequest;
import sleep.interfaces.Function;
import sleep.interfaces.Variable;
import sleep.runtime.Scalar;
import sleep.runtime.ScriptEnvironment;
import sleep.runtime.ScriptInstance;
import sleep.runtime.ScriptVariables;
import sleep.runtime.SleepUtils;

/**
 * The Sleep Closure class. This class represents a Function object that is also
 * a self contained closure
 */
public class SleepClosure implements Function, Runnable {
	
	/**
     * 
     */
	private static final long serialVersionUID = 5328795954519346800L;
	
	private static int ccount = -1;
	
	private int id;
	
	private class ClosureIterator implements Iterator {
		
		protected Scalar current;
		
		protected Stack locals = new Stack();
		
		@Override
		public boolean hasNext() {
		
			current = callClosure("eval", null, locals);
			return !SleepUtils.isEmptyScalar(current);
		}
		
		@Override
		public Object next() {
		
			return current;
		}
		
		@Override
		public void remove() {
		
		}
	}
	
	public Iterator scalarIterator() {
	
		return new ClosureIterator();
	}
	
	/** the block of code associated with this sleep closure */
	Block code;
	
	/** the owning script associated with this sleep closure */
	ScriptInstance owner;
	
	/** the saved context of this closure */
	Stack context;
	
	/** the meta data for this closure context */
	HashMap metadata;
	
	/** the closure variables referenced by this closure */
	Variable variables;
	
	/** put some value into the metadata store associated with this closure. */
	public void putMetadata(final Object key, final Object value) {
	
		metadata.put(key, value);
	}
	
	/** obtain a key from the metadata store associated with this closure */
	public Object getAndRemoveMetadata(final Object key, final Object defaultv) {
	
		final Object temp = metadata.remove(key);
		if (temp == null) {
			return defaultv;
		}
		return temp;
	}
	
	/**
	 * saves the top level context; may throw an exception if an error is
	 * detected... be sure to move critical cleanup prior to this function.
	 */
	private void saveToplevelContext(final Stack _context, final LinkedList localLevel) {
	
		if (!_context.isEmpty()) {
			_context.push(localLevel); /* push the local vars on to the top of the context stack,
			                              this better be popped before use!!! */
			context.push(_context);
		} else if (localLevel.size() != 1) {
			throw new RuntimeException(localLevel.size() - 1 + " unaccounted local stack frame(s) in " + toString() + " (perhaps you forgot to &popl?)");
		}
	}
	
	/** returns the top most context stack... */
	private Stack getToplevelContext() {
	
		if (context.isEmpty()) {
			return new Stack();
		}
		return (Stack) context.pop();
	}
	
	/** Returns a generic string version of this closure without id information */
	public String toStringGeneric() {
	
		return "&closure[" + code.getSourceLocation() + "]";
	}
	
	/**
	 * Information about this closure in the form of &closure[<source
	 * file>:<line range>]#<instance number>
	 */
	@Override
	public String toString() {
	
		return toStringGeneric() + "#" + id;
	}
	
	/**
	 * Creates a new Sleep Closure, with a brand new set of internal variables.
	 * Don't be afraid, you can call this constructor from your code.
	 */
	public SleepClosure(final ScriptInstance si, final Block _code) {
	
		this(si, _code, si.getScriptVariables().getGlobalVariables().createInternalVariableContainer());
	}
	
	/**
	 * Creates a new Sleep Closure that uses the specified variable container
	 * for its internal variables
	 */
	public SleepClosure(final ScriptInstance si, final Block _code, final Variable _var) {
	
		code = _code;
		owner = si;
		context = new Stack();
		metadata = new HashMap();
		
		_var.putScalar("$this", SleepUtils.getScalar(this));
		setVariables(_var);
		
		SleepClosure.ccount = (SleepClosure.ccount + 1) % Short.MAX_VALUE;
		
		id = SleepClosure.ccount;
	}
	
	/** Returns the owning script instance */
	public ScriptInstance getOwner() {
	
		return owner;
	}
	
	/** Returns the runnable block of code associated with this closure */
	public Block getRunnableCode() {
	
		return code;
	}
	
	/** Returns the variable container for this closures */
	public Variable getVariables() {
	
		return variables;
	}
	
	/** Sets the variable environment for this closure */
	public void setVariables(final Variable _variables) {
	
		variables = _variables;
	}
	
	/** "Safely" calls this closure. */
	@Override
	public void run() {
	
		callClosure("run", null, null);
	}
	
	/**
	 * "Safely" calls this closure. Use this if you are evaluating this closure
	 * from your own code.
	 * 
	 * @param message
	 *            the message to pass to this closure (available as $0)
	 * @param the
	 *            calling script instance (null value assumes same as owner)
	 * @param the
	 *            local data as a stack object (available as $1 .. $n)
	 * @return the scalar returned by this closure
	 */
	public Scalar callClosure(final String message, ScriptInstance si, Stack locals) {
	
		if (si == null) {
			si = getOwner();
		}
		
		if (locals == null) {
			locals = new Stack();
		}
		
		si.getScriptEnvironment().pushSource("<internal>");
		si.getScriptEnvironment().CreateFrame();
		si.getScriptEnvironment().CreateFrame(locals); /* dump the local vars here plz */
		
		final CallRequest request = new CallRequest.ClosureCallRequest(si.getScriptEnvironment(), -1, SleepUtils.getScalar(this), message);
		request.CallFunction();
		
		/* get the return value */
		final Scalar rv = si.getScriptEnvironment().getCurrentFrame().isEmpty() ? SleepUtils.getEmptyScalar() : (Scalar) si.getScriptEnvironment().getCurrentFrame().pop();
		
		/* handle the cleanup */
		si.getScriptEnvironment().KillFrame();
		si.getScriptEnvironment().clearReturn();
		si.getScriptEnvironment().popSource();
		
		return rv;
	}
	
	/** Evaluates the closure, use callClosure instead. */
	@Override
	public Scalar evaluate(final String message, final ScriptInstance si, final Stack locals) {
	
		if (owner == null) {
			owner = si;
		}
		
		final ScriptVariables vars = si.getScriptVariables();
		final ScriptEnvironment env = si.getScriptEnvironment();
		
		Variable localLevel;
		
		Scalar temp; // return value of subroutine.
		
		synchronized(vars) {
			final Stack toplevel = getToplevelContext();
			env.loadContext(toplevel, metadata);
			
			vars.pushClosureLevel(getVariables());
			
			if (toplevel.isEmpty()) /* a normal closure call */
			{
				vars.beginToplevel(new LinkedList());
				vars.pushLocalLevel();
			} else /* restoring from a coroutine */
			{
				final LinkedList levels = (LinkedList) toplevel.pop();
				vars.beginToplevel(levels);
			}
			
			localLevel = vars.getLocalVariables();
			
			//
			// initialize local variables...
			//
			vars.setScalarLevel("$0", SleepUtils.getScalar(message), localLevel);
			BridgeUtilities.initLocalScope(vars, localLevel, locals);
			
			//
			// call the function, save the scalar that was returned. 
			//
			if (toplevel.isEmpty()) {
				temp = code.evaluate(env);
			} else {
				temp = env.evaluateOldContext();
			}
			
			final LinkedList phear = vars.leaveToplevel(); /* this will simultaneously save and remove all local scopes associated with
			                                               the current closure context.  Very sexy */
			vars.popClosureLevel(); /* still have to do this manually, one day I need to refactor this state saving stuff */
			
			if (si.getScriptEnvironment().isCallCC()) {
				final SleepClosure tempc = SleepUtils.getFunctionFromScalar(si.getScriptEnvironment().getReturnValue(), si);
				tempc.putMetadata("continuation", SleepUtils.getScalar(this));
				tempc.putMetadata("sourceLine", si.getScriptEnvironment().getCurrentFrame().pop());
				tempc.putMetadata("sourceFile", si.getScriptEnvironment().getCurrentFrame().pop());
				
				si.getScriptEnvironment().flagReturn(si.getScriptEnvironment().getReturnValue(), ScriptEnvironment.FLOW_CONTROL_PASS);
			}
			
			saveToplevelContext(env.saveContext(), phear); /* saves the top level context *pHEAR*; done last in case there is an error with this */
		}
		
		return temp;
	}
	
	private void writeObject(final ObjectOutputStream out) throws IOException {
	
		out.writeInt(id);
		out.writeObject(code);
		out.writeObject(context);
		/*       out.writeObject(metadata); */
		out.writeObject(variables);
	}
	
	private void readObject(final ObjectInputStream in) throws IOException, ClassNotFoundException {
	
		id = in.readInt();
		code = (Block) in.readObject();
		context = (Stack) in.readObject();
		metadata = new HashMap();
		/*       metadata  = (HashMap)in.readObject(); */
		variables = (Variable) in.readObject();
		owner = null;
	}
}
