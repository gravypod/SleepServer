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
package sleep.runtime;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import sleep.engine.ObjectUtilities;

/**
 * A class for creating accessing a Map data structure in your application in a
 * ready only way. It is assumed that your map data structure uses strings for
 * keys. Accessed values will be marshalled into Sleep scalars
 */
public class MapWrapper implements ScalarHash {
	
	/**
     * 
     */
	private static final long serialVersionUID = -6798394964717165237L;
	
	protected Map values;
	
	public MapWrapper(final Map _values) {
	
		values = _values;
	}
	
	@Override
	public Scalar getAt(final Scalar key) {
	
		final Object o = values.get(key.getValue().toString());
		return ObjectUtilities.BuildScalar(true, o);
	}
	
	/**
	 * this operation is kind of expensive... should be fixed up to take care of
	 * that
	 */
	@Override
	public ScalarArray keys() {
	
		return new CollectionWrapper(values.keySet());
	}
	
	@Override
	public void remove(final Scalar key) {
	
		throw new RuntimeException("hash is read-only");
	}
	
	@Override
	public Map getData() {
	
		final Map temp = new HashMap();
		final Iterator i = values.entrySet().iterator();
		while(i.hasNext()) {
			final Map.Entry next = (Map.Entry) i.next();
			
			if (next.getValue() != null && next.getKey() != null) {
				temp.put(next.getKey().toString(), ObjectUtilities.BuildScalar(true, next.getValue()));
			}
		}
		
		return temp;
	}
	
	public void rehash(final int capacity, final float load) {
	
		throw new RuntimeException("hash is read-only");
	}
	
	@Override
	public String toString() {
	
		return values.toString();
	}
}
