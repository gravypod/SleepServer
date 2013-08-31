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

import java.util.Stack;

import sleep.interfaces.Function;
import sleep.runtime.Scalar;
import sleep.runtime.ScriptInstance;

public class Tainter implements Function {
	
	/**
     * 
     */
	private static final long serialVersionUID = -8938834993320735168L;
	
	protected Object function;
	
	public Tainter(final Object f) {
	
		function = f;
	}
	
	@Override
	public Scalar evaluate(final String name, final ScriptInstance script, final Stack arguments) {
	
		final Scalar value = ((Function) function).evaluate(name, script, arguments);
		TaintUtils.taintAll(value);
		return value;
	}
}
