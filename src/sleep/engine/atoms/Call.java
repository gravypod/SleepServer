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

import sleep.engine.Block;
import sleep.engine.CallRequest;
import sleep.engine.Step;
import sleep.interfaces.Function;
import sleep.runtime.Scalar;
import sleep.runtime.ScriptEnvironment;
import sleep.runtime.SleepUtils;

public class Call extends Step {
	
	/**
     * 
     */
	private static final long serialVersionUID = -1318377443254558623L;
	
	String function;
	
	public Call(final String f) {
	
		function = f;
	}
	
	@Override
	public String toString(final String prefix) {
	
		return prefix + "[Function Call]: " + function + "\n";
	}
	
	// Pre Condition:
	//  arguments on the current stack (to allow stack to be passed0
	//
	// Post Condition:
	//  current frame will be dissolved and return value will be placed on parent frame
	
	@Override
	public Scalar evaluate(final ScriptEnvironment e) {
	
		final Function callme = e.getFunction(function);
		Block inline = null;
		
		if (callme != null) {
			final CallRequest.FunctionCallRequest request = new CallRequest.FunctionCallRequest(e, getLineNumber(), function, callme);
			request.CallFunction();
		} else if ((inline = e.getBlock(function)) != null) {
			final CallRequest.InlineCallRequest request = new CallRequest.InlineCallRequest(e, getLineNumber(), function, inline);
			request.CallFunction();
		} else {
			e.getScriptInstance().fireWarning("Attempted to call non-existent function " + function, getLineNumber());
			e.FrameResult(SleepUtils.getEmptyScalar());
		}
		
		return null;
	}
}
