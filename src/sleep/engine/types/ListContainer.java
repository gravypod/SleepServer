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

import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;

import sleep.runtime.Scalar;
import sleep.runtime.ScalarArray;
import sleep.runtime.SleepUtils;

/**
 * A linked list backing for Sleep Arrays. Most array ops are better off with
 * this type of backing
 */
public class ListContainer implements ScalarArray {
	
	private static final long serialVersionUID = -5269195591772389649L;
	
	protected List<Scalar> values;
	
	public ListContainer() {
	
		values = new MyLinkedList();
	}
	
	public ListContainer(final List<Scalar> list) {
	
		values = list;
	}
	
	@Override
	public ScalarArray sublist(final int from, final int to) {
	
		return new ListContainer(values.subList(from, to));
	}
	
	/** initial values must be a collection of Scalar's */
	public ListContainer(final Collection<Scalar> initialValues) {
	
		this();
		values.addAll(initialValues);
	}
	
	@Override
	public Scalar pop() {
	
		return values.remove(values.size() - 1);
	}
	
	@Override
	public Scalar push(final Scalar value) {
	
		values.add(value);
		return value;
	}
	
	@Override
	public int size() {
	
		return values.size();
	}
	
	@Override
	public void sort(final Comparator compare) {
	
		Collections.sort(values, compare);
	}
	
	@Override
	public Scalar getAt(final int index) {
	
		if (index >= size()) {
			final Scalar temp = SleepUtils.getEmptyScalar();
			values.add(temp);
			return temp;
		}
		
		return values.get(index);
	}
	
	@Override
	public void remove(final Scalar key) {
	
		SleepUtils.removeScalar(values.iterator(), key);
	}
	
	@Override
	public Scalar remove(final int index) {
	
		return values.remove(index);
	}
	
	@Override
	public Iterator<Scalar> scalarIterator() {
	
		return values.iterator();
	}
	
	@Override
	public Scalar add(final Scalar value, final int index) {
	
		values.add(index, value);
		return value;
	}
	
	@Override
	public String toString() {
	
		return values.toString();
	}
}
