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

import sleep.bridges.SleepClosure;
import sleep.engine.Block;
import sleep.engine.Step;
import sleep.runtime.Scalar;
import sleep.runtime.ScriptEnvironment;
import sleep.runtime.SleepUtils;

public class CreateClosure extends Step {
	
	/**
     * 
     */
	private static final long serialVersionUID = -4885505274906556436L;
	
	Block block = null;
	
	@Override
	public String toString(final String prefix) {
	
		return prefix + "[Create Closure]\n" + block.toString(prefix + "   ");
	}
	
	public CreateClosure(final Block _block) {
	
		block = _block;
	}
	
	// 
	// no stack pre condition.
	//
	// post condition:
	//   pushes closure onto current frame
	
	@Override
	public Scalar evaluate(final ScriptEnvironment e) {
	
		final Scalar value = SleepUtils.getScalar(new SleepClosure(e.getScriptInstance(), block));
		e.getCurrentFrame().push(value);
		
		return null;
	}
}
