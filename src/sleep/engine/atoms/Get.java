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
import sleep.interfaces.Function;
import sleep.runtime.Scalar;
import sleep.runtime.ScriptEnvironment;
import sleep.runtime.ScriptInstance;
import sleep.runtime.SleepUtils;

public class Get extends Step {
	
	/**
     * 
     */
	private static final long serialVersionUID = -6481651174984159681L;
	
	String value;
	
	public Get(final String v) {
	
		value = v;
	}
	
	@Override
	public String toString(final String prefix) {
	
		return prefix + "[Get Item]: " + value + "\n";
	}
	
	@Override
	public Scalar evaluate(final ScriptEnvironment e) {
	
		if (value.charAt(0) == '&') {
			final Function func = e.getFunction(value);
			
			final Scalar blah = SleepUtils.getScalar(func);
			e.getCurrentFrame().push(blah);
		} else {
			Scalar structure = e.getScalar(value);
			
			if (structure == null) {
				if (value.charAt(0) == '@') {
					structure = SleepUtils.getArrayScalar();
				} else if (value.charAt(0) == '%') {
					structure = SleepUtils.getHashScalar();
				} else {
					structure = SleepUtils.getEmptyScalar();
				}
				
				e.putScalar(value, structure);
				
				if ((e.getScriptInstance().getDebugFlags() & ScriptInstance.DEBUG_REQUIRE_STRICT) == ScriptInstance.DEBUG_REQUIRE_STRICT) {
					e.showDebugMessage("variable '" + value + "' not declared");
				}
			}
			
			e.getCurrentFrame().push(structure);
		}
		
		return null;
	}
}
