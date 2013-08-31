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

import sleep.runtime.ScalarType;

/** A tainted scalar value *pHEAR* */
public class TaintedValue implements ScalarType {
	
	/**
     * 
     */
	private static final long serialVersionUID = -3515970653051866590L;
	
	protected ScalarType value = null;
	
	/** construct a tainted scalar */
	public TaintedValue(final ScalarType _value) {
	
		value = _value;
	}
	
	@Override
	public ScalarType copyValue() {
	
		return new TaintedValue(value.copyValue());
	}
	
	public ScalarType untaint() {
	
		return value;
	}
	
	@Override
	public int intValue() {
	
		return value.intValue();
	}
	
	@Override
	public long longValue() {
	
		return value.longValue();
	}
	
	@Override
	public double doubleValue() {
	
		return value.doubleValue();
	}
	
	@Override
	public String toString() {
	
		return value.toString();
	}
	
	@Override
	public Object objectValue() {
	
		return value.objectValue();
	}
	
	@Override
	public Class getType() {
	
		return value.getType();
	}
}
