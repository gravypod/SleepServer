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
import sleep.interfaces.Function;
import sleep.runtime.Scalar;
import sleep.runtime.ScriptEnvironment;

public class TaintCall extends PermeableStep {
	
	/**
     * 
     */
	private static final long serialVersionUID = -1342509523077304249L;
	
	protected String function;
	
	public TaintCall(final String _function, final Step _wrapped) {
	
		super(_wrapped);
		function = _function;
	}
	
	@Override
	public Scalar evaluate(final ScriptEnvironment e) {
	
		final Function callme = e.getFunction(function);
		
		if (callme != null && (callme.getClass() == Tainter.class || callme.getClass() == Sanitizer.class)) {
			return wrapped.evaluate(e);
		} else {
			return super.evaluate(e);
		}
	}
}
