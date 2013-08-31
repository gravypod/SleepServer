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
package sleep.engine;

import java.io.Serializable;

import sleep.runtime.Scalar;
import sleep.runtime.ScriptEnvironment;
import sleep.runtime.SleepUtils;

public class Step implements Serializable {
	
	/**
     * 
     */
	private static final long serialVersionUID = 4687083741384946712L;
	
	/** the script line number that this step was generated from */
	protected int line;
	
	/** Steps act as a simple self contained linked list */
	public Step next;
	
	/** returns a string representation of this atomic step */
	public String toString(final String prefix) {
	
		return prefix + "[NOP]\n";
	}
	
	/** convience method for the code generator to set the line number. */
	public void setInfo(final int _line) {
	
		line = _line;
	}
	
	/**
	 * returns the last line number that this step is associated with (assuming
	 * it is associated with multiple lines
	 */
	public int getHighLineNumber() {
	
		return getLineNumber();
	}
	
	/**
	 * returns the first line number that this step is associated with (assuming
	 * it is associated with multiple lines
	 */
	public int getLowLineNumber() {
	
		return getLineNumber();
	}
	
	/** returns the line number this step is associated with */
	public int getLineNumber() {
	
		return line;
	}
	
	/** evaluate this atomic step. */
	public Scalar evaluate(final ScriptEnvironment e) {
	
		return SleepUtils.getEmptyScalar();
	}
	
	@Override
	public String toString() {
	
		return toString("");
	}
}
