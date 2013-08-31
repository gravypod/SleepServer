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

import java.util.Iterator;
import java.util.Map;

import sleep.runtime.Scalar;
import sleep.runtime.ScalarArray;
import sleep.runtime.ScalarHash;

/** This class is used to wrap read-only hashes */
public class TaintHash implements ScalarHash {
	
	/**
     * 
     */
	private static final long serialVersionUID = -3496590939016050606L;
	
	protected ScalarHash source;
	
	public TaintHash(final ScalarHash src) {
	
		source = src;
	}
	
	@Override
	public Scalar getAt(final Scalar key) {
	
		return TaintUtils.taintAll(source.getAt(key));
	}
	
	/**
	 * this operation is kind of expensive... should be fixed up to take care of
	 * that
	 */
	@Override
	public ScalarArray keys() {
	
		return source.keys();
	}
	
	@Override
	public void remove(final Scalar key) {
	
		source.remove(key);
	}
	
	@Override
	public Map getData() {
	
		final Map temp = source.getData();
		
		final Iterator i = temp.entrySet().iterator();
		while(i.hasNext()) {
			final Map.Entry next = (Map.Entry) i.next();
			
			if (next.getValue() != null && next.getKey() != null) {
				next.setValue(TaintUtils.taintAll((Scalar) next.getValue()));
			}
		}
		
		return temp;
	}
	
	@Override
	public String toString() {
	
		return source.toString();
	}
}
