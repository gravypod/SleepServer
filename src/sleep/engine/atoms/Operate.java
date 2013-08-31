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

import sleep.engine.Step;
import sleep.interfaces.Operator;
import sleep.runtime.Scalar;
import sleep.runtime.ScriptEnvironment;
import sleep.runtime.SleepUtils;

public class Operate extends Step {
	
	/**
     * 
     */
	private static final long serialVersionUID = -7440583617175514999L;
	
	String oper;
	
	public Operate(final String o) {
	
		oper = o;
	}
	
	@Override
	public String toString(final String prefix) {
	
		return prefix + "[Operator]: " + oper + "\n";
	}
	
	//
	// Pre Condition:
	//   lhs, rhs are both on current frame
	//
	// Post Condition:
	//   current frame is dissolved
	//   return value of operation placed on parent frame
	//
	
	@Override
	public Scalar evaluate(final ScriptEnvironment e) {
	
		final Operator callme = e.getOperator(oper);
		
		if (callme != null) {
			final Scalar temp = callme.operate(oper, e.getScriptInstance(), e.getCurrentFrame());
			e.KillFrame();
			e.getCurrentFrame().push(temp);
		} else {
			e.getScriptInstance().fireWarning("Attempting to use non-existent operator: '" + oper + "'", getLineNumber());
			e.KillFrame();
			e.getCurrentFrame().push(SleepUtils.getEmptyScalar());
		}
		
		return null;
	}
}
