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

import java.io.Serializable;
import java.util.AbstractSequentialList;
import java.util.ConcurrentModificationException;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;

import sleep.runtime.SleepUtils;

public class MyLinkedList extends AbstractSequentialList implements Cloneable, Serializable, List {
	
	/**
     * 
     */
	private static final long serialVersionUID = 1210095983993674965L;
	
	private class MyListIterator implements ListIterator, Serializable {
		
		/**
         * 
         */
		private static final long serialVersionUID = -5885880822066207416L;
		
		protected int index;
		
		protected int start;
		
		protected ListEntry current;
		
		protected int modCountCheck = modCount;
		
		public void checkSafety() {
		
			if (modCountCheck != modCount) {
				throw new ConcurrentModificationException("@array changed during iteration");
			}
		}
		
		public MyListIterator(final ListEntry entry, final int index) {
		
			this.index = index;
			start = index;
			current = entry;
		}
		
		@Override
		public void add(final Object o) {
		
			checkSafety();
			
			/* add the new element after the current element */
			current = current.addAfter(o);
			
			/* increment the list so that the next element returned is
			   unaffected by this call */
			index++;
			
			modCountCheck++;
		}
		
		@Override
		public boolean hasNext() {
		
			return index != size;
		}
		
		@Override
		public boolean hasPrevious() {
		
			return index != 0;
		}
		
		@Override
		public Object next() {
		
			checkSafety();
			current = current.next();
			index++;
			return current.element();
		}
		
		@Override
		public Object previous() {
		
			checkSafety();
			current = current.previous();
			index--;
			return current.element();
		}
		
		@Override
		public int nextIndex() {
		
			return index;
		}
		
		@Override
		public int previousIndex() {
		
			return index - 1;
		}
		
		@Override
		public void remove() {
		
			if (current == header) {
				throw new IllegalStateException("list is empty");
			}
			
			checkSafety();
			current = current.remove().previous();
			
			index--;
			
			modCountCheck++;
		}
		
		@Override
		public void set(final Object o) {
		
			if (current == header) {
				throw new IllegalStateException("list is empty");
			}
			
			checkSafety();
			current.setElement(o);
		}
	}
	
	private transient int size = 0;
	
	private transient ListEntry header;
	
	/* fields used by sublists */
	private transient MyLinkedList parentList;
	
	@Override
	public int size() {
	
		return size;
	}
	
	private MyLinkedList(final MyLinkedList plist, final ListEntry begin, final ListEntry end, final int _size) {
	
		parentList = plist;
		modCount = parentList.modCount;
		
		header = new SublistHeaderEntry(begin, end);
		size = _size;
	}
	
	public MyLinkedList() {
	
		header = new NormalListEntry(SleepUtils.getScalar("[:HEADER:]"), null, null);
		header.setNext(header);
		header.setPrevious(header);
	}
	
	@Override
	public List subList(final int beginAt, final int endAt) {
	
		checkSafety();
		
		ListEntry begin = getAt(beginAt).next(); /* included */
		ListEntry end = getAt(endAt); /* not included */
		
		/* we want each sublist to consist of a direct view into the parent... operations on other
		   sublists will fail if the parent is changed through some other sublist, this makes things
		   efficient and safe */
		
		while(begin instanceof ListEntryWrapper) {
			begin = ((ListEntryWrapper) begin).parent;
		}
		
		while(end instanceof ListEntryWrapper) {
			end = ((ListEntryWrapper) end).parent;
		}
		
		return new MyLinkedList(parentList == null ? this : parentList, begin, end, endAt - beginAt);
	}
	
	/** add an object to the list */
	@Override
	public boolean add(final Object o) {
	
		final ListEntry entry = header;
		header.previous().addAfter(o);
		return true;
	}
	
	/** add an object to the list at the specified index */
	@Override
	public void add(final int index, final Object element) {
	
		final ListEntry entry = getAt(index);
		entry.addAfter(element);
	}
	
	/** get an object from the linked list */
	@Override
	public Object get(final int index) {
	
		if (index >= size) {
			throw new IndexOutOfBoundsException("Index: " + index + ", Size: " + size);
		}
		
		return getAt(index).next().element();
	}
	
	/** remove an object at the specified index */
	@Override
	public Object remove(final int index) {
	
		if (index >= size) {
			throw new IndexOutOfBoundsException("Index: " + index + ", Size: " + size);
		}
		
		final ListEntry entry = getAt(index).next();
		final Object value = entry.element();
		entry.remove();
		
		return value;
	}
	
	/** returns the entry at the specified index */
	private ListEntry getAt(final int index) {
	
		if (index < 0 || index > size) {
			throw new IndexOutOfBoundsException("Index: " + index + ", Size: " + size);
		}
		
		ListEntry entry = header;
		
		if (index == size) {
			return header.previous();
		} else if (index < size / 2) {
			for (int x = 0; x < index; x++) {
				entry = entry.next();
			}
		} else {
			entry = entry.previous();
			for (int x = size; x > index; x--) {
				entry = entry.previous();
			}
		}
		
		return entry;
	}
	
	@Override
	public ListIterator listIterator(final int index) {
	
		return new MyListIterator(getAt(index), index);
	}
	
	// code for the ListEntry //
	
	private interface ListEntry extends Serializable {
		
		public ListEntry remove();
		
		public ListEntry addBefore(final Object o);
		
		public ListEntry addAfter(final Object o);
		
		public ListEntry next();
		
		public ListEntry previous();
		
		public void setNext(final ListEntry entry);
		
		public void setPrevious(final ListEntry entry);
		
		public Object element();
		
		public void setElement(final Object o);
	}
	
	public void checkSafety() {
	
		if (parentList != null && modCount != parentList.modCount) {
			throw new ConcurrentModificationException("parent @array changed after &sublist creation");
		}
	}
	
	private class SublistHeaderEntry implements ListEntry {
		
		/**
         * 
         */
		private static final long serialVersionUID = 4107915613809344025L;
		
		private final ListEntry anchorLeft;
		
		private final ListEntry anchorRight;
		
		public SublistHeaderEntry(final ListEntry a, final ListEntry b) {
		
			anchorLeft = a.previous();
			anchorRight = b.next();
		}
		
		@Override
		public ListEntry remove() {
		
			throw new UnsupportedOperationException("remove");
		}
		
		@Override
		public ListEntry previous() {
		
			return new ListEntryWrapper(anchorRight.previous());
		}
		
		@Override
		public ListEntry next() {
		
			return new ListEntryWrapper(anchorLeft.next());
		}
		
		@Override
		public void setNext(final ListEntry e) {
		
			anchorRight.setPrevious(e);
			e.setNext(anchorRight);
		}
		
		@Override
		public void setPrevious(final ListEntry e) {
		
			anchorLeft.setNext(e);
			e.setPrevious(anchorLeft);
		}
		
		@Override
		public ListEntry addBefore(final Object o) {
		
			return previous().addAfter(o);
		}
		
		@Override
		public ListEntry addAfter(final Object o) {
		
			return next().addBefore(o);
		}
		
		@Override
		public Object element() {
		
			return SleepUtils.getScalar("[:header:]");
		}
		
		@Override
		public void setElement(final Object o) {
		
			throw new UnsupportedOperationException("setElement");
		}
	}
	
	private class ListEntryWrapper implements ListEntry {
		
		/**
         * 
         */
		private static final long serialVersionUID = -4694848786530414348L;
		
		public ListEntry parent;
		
		public ListEntryWrapper(final ListEntry _parent) {
		
			parent = _parent;
		}
		
		@Override
		public ListEntry remove() {
		
			checkSafety();
			
			final ListEntry temp = parent.remove();
			
			size--;
			modCount++;
			
			if (size == 0) {
				return header;
			} else {
				if (parent == header.next()) {
					header.setNext(temp);
				}
				
				if (parent == header.previous()) {
					header.setPrevious(temp);
				}
			}
			
			return new ListEntryWrapper(temp);
		}
		
		@Override
		public ListEntry addBefore(final Object o) {
		
			checkSafety();
			
			final ListEntry temp = parent.addBefore(o);
			
			size++;
			modCount++;
			
			if (size == 1) {
				header.setNext(temp);
				header.setPrevious(temp);
			} else if (parent == header.next()) {
				header.setPrevious(temp);
			}
			
			return new ListEntryWrapper(temp);
		}
		
		@Override
		public ListEntry addAfter(final Object o) {
		
			checkSafety();
			
			final ListEntry temp = parent.addAfter(o);
			
			size++;
			modCount++;
			
			if (size == 1) {
				header.setNext(temp);
				header.setPrevious(temp);
			} else if (parent == header.previous()) {
				header.setNext(temp);
			}
			
			return new ListEntryWrapper(temp);
		}
		
		@Override
		public void setNext(final ListEntry entry) {
		
			throw new UnsupportedOperationException("ListEntryWrapper::setNext");
		}
		
		@Override
		public void setPrevious(final ListEntry entry) {
		
			throw new UnsupportedOperationException("ListEntryWrapper::setPrevious");
		}
		
		@Override
		public Object element() {
		
			return parent.element();
		}
		
		@Override
		public void setElement(final Object o) {
		
			parent.setElement(o);
		}
		
		@Override
		public ListEntry next() {
		
			checkSafety();
			
			if (parent == header.next()) {
				return new ListEntryWrapper(header);
			}
			
			final ListEntryWrapper r = new ListEntryWrapper(parent.next());
			return r;
		}
		
		@Override
		public ListEntry previous() {
		
			checkSafety();
			
			if (parent == header.previous()) {
				return new ListEntryWrapper(header);
			}
			
			final ListEntryWrapper r = new ListEntryWrapper(parent.previous());
			return r;
		}
	}
	
	private class NormalListEntry implements ListEntry {
		
		/**
         * 
         */
		private static final long serialVersionUID = -5634867561418618651L;
		
		public Object element;
		
		public ListEntry previous;
		
		public ListEntry next;
		
		public NormalListEntry(final Object _element, final ListEntry _previous, final ListEntry _next) {
		
			element = _element;
			previous = _previous;
			next = _next;
			
			if (previous != null) {
				previous.setNext(this);
			}
			
			if (next != null) {
				next.setPrevious(this);
			}
		}
		
		@Override
		public void setNext(final ListEntry entry) {
		
			next = entry;
		}
		
		@Override
		public void setPrevious(final ListEntry entry) {
		
			previous = entry;
		}
		
		@Override
		public ListEntry next() {
		
			return next;
		}
		
		@Override
		public ListEntry previous() {
		
			return previous;
		}
		
		@Override
		public ListEntry remove() {
		
			final ListEntry prev = previous();
			final ListEntry nxt = next();
			
			nxt.setPrevious(prev);
			prev.setNext(nxt);
			
			size--;
			modCount++;
			return nxt;
		}
		
		@Override
		public void setElement(final Object o) {
		
			element = o;
		}
		
		@Override
		public Object element() {
		
			return element;
		}
		
		@Override
		public ListEntry addBefore(final Object o) {
		
			final ListEntry temp = new NormalListEntry(o, previous, this);
			
			size++;
			modCount++;
			
			return temp;
		}
		
		@Override
		public ListEntry addAfter(final Object o) {
		
			final ListEntry temp = new NormalListEntry(o, this, next);
			
			size++;
			modCount++;
			
			return temp;
		}
		
		@Override
		public String toString() {
		
			StringBuffer buffer = new StringBuffer(":[" + element() + "]:");
			
			if (this == header) {
				buffer = new StringBuffer(":[HEADER]:");
			}
			
			ListEntry entry = previous();
			while(entry != header) {
				buffer.insert(0, "[" + entry.element() + "]-> ");
				entry = entry.previous();
			}
			
			entry = next();
			while(entry != header) {
				buffer.append(" ->[" + entry.element() + "]");
				entry = entry.next();
			}
			
			return buffer.toString();
		}
	}
	
	/* save this list to the stream */
	private synchronized void writeObject(final java.io.ObjectOutputStream out) throws java.io.IOException {
	
		/* grab any fields I missed */
		out.defaultWriteObject();
		
		/* write out the size */
		out.writeInt(size);
		
		/* blah blah blah */
		final Iterator i = iterator();
		while(i.hasNext()) {
			out.writeObject(i.next());
		}
	}
	
	/* reconstitute this list from the stream */
	private synchronized void readObject(final java.io.ObjectInputStream in) throws java.io.IOException, ClassNotFoundException {
	
		/* read any fields I missed */
		in.defaultReadObject();
		
		/* read in the size */
		final int size = in.readInt();
		
		/* create the header */
		header = new NormalListEntry(SleepUtils.getScalar("[:HEADER:]"), null, null);
		header.setNext(header);
		header.setPrevious(header);
		
		/* populate the list */
		for (int x = 0; x < size; x++) {
			add(in.readObject());
		}
	}
}
