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

import java.util.Hashtable;

import sleep.interfaces.Loadable;
import sleep.interfaces.Variable;
import sleep.runtime.Scalar;
import sleep.runtime.ScriptInstance;

public class DefaultVariable implements Variable, Loadable {
	
	/**
     * 
     */
	private static final long serialVersionUID = -2706370801224485626L;
	
	protected Hashtable values = new Hashtable();
	
	@Override
	public boolean scalarExists(final String key) {
	
		return values.containsKey(key);
	}
	
	@Override
	public Scalar getScalar(final String key) {
	
		return (Scalar) values.get(key);
	}
	
	@Override
	public Scalar putScalar(final String key, final Scalar value) {
	
		return (Scalar) values.put(key, value);
	}
	
	@Override
	public void removeScalar(final String key) {
	
		values.remove(key);
	}
	
	@Override
	public Variable createLocalVariableContainer() {
	
		return new DefaultVariable();
	}
	
	@Override
	public Variable createInternalVariableContainer() {
	
		return new DefaultVariable();
	}
	
	@Override
	public void scriptLoaded(final ScriptInstance script) {
	
	}
	
	@Override
	public void scriptUnloaded(final ScriptInstance script) {
	
	}
	
	public DefaultVariable() {
	
	}
}
