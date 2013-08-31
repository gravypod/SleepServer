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

import java.util.Comparator;
import java.util.Iterator;

import sleep.runtime.Scalar;
import sleep.runtime.ScalarArray;

/**
 * Used to wrap read-only arrays so values are only converted on an as-needed
 * basis
 */
public class TaintArray implements ScalarArray {
	
	/**
     * 
     */
	private static final long serialVersionUID = 1654537223403805180L;
	
	protected ScalarArray source;
	
	@Override
	public ScalarArray sublist(final int begin, final int end) {
	
		return new TaintArray(source.sublist(begin, end));
	}
	
	public TaintArray(final ScalarArray src) {
	
		source = src;
	}
	
	@Override
	public String toString() {
	
		return source.toString();
	}
	
	@Override
	public Scalar pop() {
	
		return TaintUtils.taintAll(source.pop());
	}
	
	@Override
	public void sort(final Comparator compare) {
	
		source.sort(compare);
	}
	
	@Override
	public Scalar push(final Scalar value) {
	
		return TaintUtils.taintAll(source.push(value));
	}
	
	@Override
	public int size() {
	
		return source.size();
	}
	
	@Override
	public Scalar remove(final int index) {
	
		return TaintUtils.taintAll(source.remove(index));
	}
	
	@Override
	public Scalar getAt(final int index) {
	
		return TaintUtils.taintAll(source.getAt(index));
	}
	
	@Override
	public Iterator scalarIterator() {
	
		return new TaintIterator(source.scalarIterator());
	}
	
	@Override
	public Scalar add(final Scalar value, final int index) {
	
		return TaintUtils.taintAll(source.add(value, index));
	}
	
	@Override
	public void remove(final Scalar value) {
	
		source.remove(value);
	}
	
	protected class TaintIterator implements Iterator {
		
		protected Iterator realIterator;
		
		public TaintIterator(final Iterator iter) {
		
			realIterator = iter;
		}
		
		@Override
		public boolean hasNext() {
		
			return realIterator.hasNext();
		}
		
		@Override
		public Object next() {
		
			return TaintUtils.taintAll((Scalar) realIterator.next());
		}
		
		@Override
		public void remove() {
		
			realIterator.remove();
		}
	}
}
