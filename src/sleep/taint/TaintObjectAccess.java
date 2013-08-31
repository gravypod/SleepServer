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
package sleep.taint;

import sleep.engine.Step;
import sleep.runtime.Scalar;
import sleep.runtime.ScriptEnvironment;
import sleep.runtime.ScriptInstance;
import sleep.runtime.SleepUtils;

public class TaintObjectAccess extends PermeableStep {
	
	/**
     * 
     */
	private static final long serialVersionUID = 3247644782594232776L;
	
	protected String name;
	
	protected Class classRef;
	
	public TaintObjectAccess(final Step wrapit, final String _name, final Class _classRef) {
	
		super(wrapit);
		name = _name;
		classRef = _classRef;
	}
	
	@Override
	public Scalar evaluate(final ScriptEnvironment e) {
	
		Scalar scalar = null;
		final Scalar value = null;
		
		if (classRef != null || SleepUtils.isFunctionScalar((Scalar) e.getCurrentFrame().peek())) {
			return super.evaluate(e);
		}
		
		final String desc = e.hasFrame() ? TaintUtils.checkArguments(e.getCurrentFrame()) : null;
		
		scalar = (Scalar) e.getCurrentFrame().peek();
		
		if (desc != null && !TaintUtils.isTainted(scalar)) {
			TaintUtils.taint(scalar);
			
			if ((e.getScriptInstance().getDebugFlags() & ScriptInstance.DEBUG_TRACE_TAINT) == ScriptInstance.DEBUG_TRACE_TAINT) {
				e.getScriptInstance().fireWarning("tainted object: " + SleepUtils.describe(scalar) + " from: " + desc, getLineNumber());
			}
		}
		
		return callit(e, desc);
	}
}
