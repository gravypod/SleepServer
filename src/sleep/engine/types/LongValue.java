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
package sleep.engine.types;

import sleep.runtime.ScalarType;

public class LongValue implements ScalarType {
	
	/**
     * 
     */
	private static final long serialVersionUID = -8590950665834023129L;
	
	protected long value;
	
	public LongValue(final long _value) {
	
		value = _value;
	}
	
	@Override
	public ScalarType copyValue() {
	
		return this;
	}
	
	@Override
	public int intValue() {
	
		return (int) value;
	}
	
	@Override
	public long longValue() {
	
		return value;
	}
	
	@Override
	public double doubleValue() {
	
		return value;
	}
	
	@Override
	public String toString() {
	
		return value + "";
	}
	
	@Override
	public Object objectValue() {
	
		return new Long(value);
	}
	
	@Override
	public Class getType() {
	
		return this.getClass();
	}
}
