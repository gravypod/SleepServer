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

import java.text.FieldPosition;
import java.text.ParsePosition;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Stack;

import sleep.interfaces.Function;
import sleep.interfaces.Loadable;
import sleep.runtime.Scalar;
import sleep.runtime.ScriptInstance;
import sleep.runtime.SleepUtils;

public class TimeDateBridge implements Loadable {
	
	@Override
	public void scriptLoaded(final ScriptInstance script) {
	
		// time date functions 
		script.getScriptEnvironment().getEnvironment().put("&ticks", new ticks());
		script.getScriptEnvironment().getEnvironment().put("&formatDate", new formatDate());
		script.getScriptEnvironment().getEnvironment().put("&parseDate", new parseDate());
	}
	
	@Override
	public void scriptUnloaded(final ScriptInstance script) {
	
	}
	
	private static class formatDate implements Function {
		
		/**
         * 
         */
		private static final long serialVersionUID = 1281113634470517344L;
		
		@Override
		public Scalar evaluate(final String f, final ScriptInstance si, final Stack locals) {
		
			long a = System.currentTimeMillis();
			
			if (locals.size() == 2) {
				a = BridgeUtilities.getLong(locals);
			}
			
			final String b = locals.pop().toString();
			
			final SimpleDateFormat format = new SimpleDateFormat(b);
			final Date adate = new Date(a);
			
			return SleepUtils.getScalar(format.format(adate, new StringBuffer(), new FieldPosition(0)).toString());
		}
	}
	
	private static class parseDate implements Function {
		
		/**
         * 
         */
		private static final long serialVersionUID = -8739285851642073063L;
		
		@Override
		public Scalar evaluate(final String f, final ScriptInstance si, final Stack locals) {
		
			final String a = locals.pop().toString();
			final String b = locals.pop().toString();
			
			final SimpleDateFormat format = new SimpleDateFormat(a);
			final Date pdate = format.parse(b, new ParsePosition(0));
			
			return SleepUtils.getScalar(pdate.getTime());
		}
	}
	
	private static class ticks implements Function {
		
		/**
         * 
         */
		private static final long serialVersionUID = -6291780953014906581L;
		
		@Override
		public Scalar evaluate(final String f, final ScriptInstance si, final Stack locals) {
		
			return SleepUtils.getScalar(System.currentTimeMillis());
		}
	}
}
