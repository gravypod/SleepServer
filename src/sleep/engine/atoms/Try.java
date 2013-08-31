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
import sleep.engine.Step;
import sleep.runtime.Scalar;
import sleep.runtime.ScriptEnvironment;

public class Try extends Step {
	
	/**
     * 
     */
	private static final long serialVersionUID = -1993853311765774487L;
	
	Block owner, handler;
	
	String var;
	
	public Try(final Block _owner, final Block _handler, final String _var) {
	
		owner = _owner;
		handler = _handler;
		var = _var;
	}
	
	@Override
	public String toString(final String prefix) {
	
		final StringBuffer buffer = new StringBuffer();
		buffer.append(prefix);
		buffer.append("[Try]\n");
		buffer.append(owner.toString(prefix + "   "));
		buffer.append(prefix);
		buffer.append("[Catch]: " + var + "\n");
		buffer.append(handler.toString(prefix + "   "));
		return buffer.toString();
	}
	
	@Override
	public Scalar evaluate(final ScriptEnvironment e) {
	
		final int mark = e.markFrame();
		e.installExceptionHandler(owner, handler, var);
		final Scalar o = owner.evaluate(e);
		e.cleanFrame(mark);
		return o;
	}
}
