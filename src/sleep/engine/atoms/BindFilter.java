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
import sleep.interfaces.FilterEnvironment;
import sleep.runtime.Scalar;
import sleep.runtime.ScriptEnvironment;

public class BindFilter extends Step {
	
	/**
     * 
     */
	private static final long serialVersionUID = 8233057403547197939L;
	
	String funcenv;
	
	Block code;
	
	String filter;
	
	String name;
	
	@Override
	public String toString() {
	
		final StringBuffer temp = new StringBuffer();
		temp.append("[Bind Filter]: " + name + "\n");
		temp.append("   [Filter]:       \n");
		temp.append("      " + filter.toString());
		temp.append("   [Code]:       \n");
		temp.append(code.toString("      "));
		
		return temp.toString();
	}
	
	public BindFilter(final String e, final String n, final Block c, final String f) {
	
		funcenv = e;
		code = c;
		filter = f;
		name = n;
	}
	
	@Override
	public Scalar evaluate(final ScriptEnvironment e) {
	
		final FilterEnvironment temp = e.getFilterEnvironment(funcenv);
		
		if (temp != null) {
			temp.bindFilteredFunction(e.getScriptInstance(), funcenv, name, filter, code);
		} else {
			e.getScriptInstance().fireWarning("Attempting to bind code to non-existent predicate environment: " + funcenv, getLineNumber());
		}
		
		return null;
	}
}
