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

import java.io.Serializable;
import java.util.Iterator;
import java.util.List;

import sleep.engine.Step;
import sleep.runtime.Scalar;
import sleep.runtime.ScriptEnvironment;
import sleep.runtime.SleepUtils;

public class PLiteral extends Step {
	
	/**
     * 
     */
	private static final long serialVersionUID = -287560155198654390L;
	
	@Override
	public String toString(final String prefix) {
	
		final StringBuffer temp = new StringBuffer();
		temp.append(prefix);
		temp.append("[Parsed Literal] ");
		
		final Iterator i = fragments.iterator();
		
		while(i.hasNext()) {
			final Fragment f = (Fragment) i.next();
			
			switch(f.type) {
				case STRING_FRAGMENT:
					temp.append(f.element);
					break;
				case ALIGN_FRAGMENT:
					temp.append("[:align:]");
					break;
				case VAR_FRAGMENT:
					temp.append("[:var:]");
					break;
			}
		}
		
		temp.append("\n");
		
		return temp.toString();
	}
	
	@Override
	public Scalar evaluate(final ScriptEnvironment e) {
	
		final Scalar value = SleepUtils.getScalar(buildString(e));
		e.getCurrentFrame().push(value);
		return value;
	}
	
	public static final int STRING_FRAGMENT = 1;
	
	public static final int ALIGN_FRAGMENT = 2;
	
	public static final int VAR_FRAGMENT = 3;
	
	private static final class Fragment implements Serializable {
		
		/**
         * 
         */
		private static final long serialVersionUID = -8862290429428941160L;
		
		public Object element;
		
		public int type;
	}
	
	private final List fragments;
	
	/**
	 * requires a list of parsed literal fragments to use when constructing the
	 * final string at runtime
	 */
	public PLiteral(final List f) {
	
		fragments = f;
	}
	
	/** create a fragment for interpretation by this parsed literal step */
	public static Fragment fragment(final int type, final Object element) {
	
		final Fragment f = new Fragment();
		f.element = element;
		f.type = type;
		
		return f;
	}
	
	private String buildString(final ScriptEnvironment e) {
	
		final StringBuffer result = new StringBuffer();
		int align = 0;
		
		String temp;
		final Iterator i = fragments.iterator();
		
		while(i.hasNext()) {
			final Fragment f = (Fragment) i.next();
			
			switch(f.type) {
				case STRING_FRAGMENT:
					result.append(f.element);
					break;
				case ALIGN_FRAGMENT:
					align = ((Scalar) e.getCurrentFrame().remove(0)).getValue().intValue();
					break;
				case VAR_FRAGMENT:
					temp = ((Scalar) e.getCurrentFrame().remove(0)).getValue().toString();
					
					for (int z = 0 - temp.length(); z > align; z--) {
						result.append(" ");
					}
					
					result.append(temp);
					
					for (int y = temp.length(); y < align; y++) {
						result.append(" ");
					}
					
					align = 0;
					break;
			}
		}
		
		e.KillFrame();
		return result.toString();
	}
}
