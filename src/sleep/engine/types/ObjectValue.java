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

public class ObjectValue implements ScalarType {
	
	/**
     * 
     */
	private static final long serialVersionUID = -5081985781831374967L;
	
	protected Object value;
	
	public ObjectValue(final Object _value) {
	
		value = _value;
	}
	
	@Override
	public ScalarType copyValue() {
	
		return this;
	}
	
	@Override
	public int intValue() {
	
		final String str = toString();
		
		if (str.length() == 0) {
			return 0;
		}
		if (str.equals("true")) {
			return 1;
		}
		if (str.equals("false")) {
			return 0;
		}
		
		try {
			return Integer.decode(str).intValue();
		} catch (final Exception ex) {
			return 0;
		}
	}
	
	@Override
	public long longValue() {
	
		final String str = toString();
		
		if (str.length() == 0) {
			return 0L;
		}
		if (str.equals("true")) {
			return 1L;
		}
		if (str.equals("false")) {
			return 0L;
		}
		
		try {
			return Long.decode(str).longValue();
		} catch (final Exception ex) {
			return 0L;
		}
	}
	
	@Override
	public double doubleValue() {
	
		final String str = toString();
		
		if (str.length() == 0) {
			return 0.0;
		}
		if (str.equals("true")) {
			return 1.0;
		}
		if (str.equals("false")) {
			return 0.0;
		}
		
		try {
			return Double.parseDouble(str);
		} catch (final Exception ex) {
			return 0;
		}
	}
	
	@Override
	public String toString() {
	
		return value.toString();
	}
	
	@Override
	public Object objectValue() {
	
		return value;
	}
	
	@Override
	public Class getType() {
	
		return this.getClass();
	}
}
