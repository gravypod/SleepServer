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

import sleep.engine.Block;
import sleep.interfaces.Environment;
import sleep.interfaces.Loadable;
import sleep.runtime.ScriptInstance;

public class DefaultEnvironment implements Loadable, Environment {
	
	@Override
	public void scriptUnloaded(final ScriptInstance si) {
	
	}
	
	@Override
	public void scriptLoaded(final ScriptInstance si) {
	
		final Hashtable env = si.getScriptEnvironment().getEnvironment();
		env.put("sub", this);
		env.put("inline", this);
	}
	
	@Override
	public void bindFunction(final ScriptInstance si, final String type, final String name, final Block code) {
	
		final Hashtable env = si.getScriptEnvironment().getEnvironment();
		
		if (type.equals("sub")) {
			env.put("&" + name, new SleepClosure(si, code));
		} else if (type.equals("inline")) {
			env.put("^&" + name, code); /* add an inline function, very harmless */
		}
	}
}
