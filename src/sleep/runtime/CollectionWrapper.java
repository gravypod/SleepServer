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

import java.util.Collection;
import java.util.Comparator;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import sleep.engine.ObjectUtilities;

/**
 * A read only scalar array for wrapping data structures that implement the
 * java.util.Collection interface. Values will be marshalled into Sleep scalars
 * when accessed.
 */
public class CollectionWrapper implements ScalarArray {
	
	/**
     * 
     */
	private static final long serialVersionUID = -3092862665129714320L;
	
	protected Collection values;
	
	protected Object[] array = null;
	
	@Override
	public ScalarArray sublist(final int begin, final int end) {
	
		final List temp = new LinkedList();
		final Iterator i = values.iterator();
		
		int count = 0;
		while(i.hasNext() && count < end) {
			final Object tempo = i.next();
			
			if (count >= begin) {
				temp.add(tempo);
			}
			count++;
		}
		
		return new CollectionWrapper(temp);
	}
	
	public CollectionWrapper(final Collection _values) {
	
		values = _values;
	}
	
	@Override
	public String toString() {
	
		return "(read-only array: " + values.toString() + ")";
	}
	
	@Override
	public Scalar pop() {
	
		throw new RuntimeException("array is read-only");
	}
	
	@Override
	public void sort(final Comparator compare) {
	
		throw new RuntimeException("array is read-only");
	}
	
	@Override
	public Scalar push(final Scalar value) {
	
		throw new RuntimeException("array is read-only");
	}
	
	@Override
	public int size() {
	
		return values.size();
	}
	
	@Override
	public Scalar remove(final int index) {
	
		throw new RuntimeException("array is read-only");
	}
	
	@Override
	public Scalar getAt(final int index) {
	
		if (array == null) {
			array = values.toArray();
		}
		
		return ObjectUtilities.BuildScalar(true, array[index]);
	}
	
	@Override
	public Iterator scalarIterator() {
	
		return new ProxyIterator(values.iterator(), false);
	}
	
	@Override
	public Scalar add(final Scalar value, final int index) {
	
		throw new RuntimeException("array is read-only");
	}
	
	@Override
	public void remove(final Scalar value) {
	
		throw new RuntimeException("array is read-only");
		// do nothing
	}
}
