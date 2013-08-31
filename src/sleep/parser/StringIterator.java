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
package sleep.parser;

import java.util.LinkedList;

public class StringIterator {
	
	protected int position = 0;
	
	protected int lineNo;
	
	protected char[] text;
	
	protected String texts;
	
	protected int begin = 0;
	
	public StringIterator(final String text) {
	
		this(text, 0);
	}
	
	@Override
	public String toString() {
	
		return texts;
	}
	
	public StringIterator(final String _text, final int _lineNo) {
	
		texts = _text;
		text = _text.toCharArray();
		lineNo = _lineNo;
	}
	
	/** check that there is another character out there for us to get */
	public boolean hasNext() {
	
		return position < text.length;
	}
	
	/** check that there are at least n chars we can still get */
	public boolean hasNext(final int n) {
	
		return position + n - 1 < text.length;
	}
	
	public int getLineNumber() {
	
		return lineNo;
	}
	
	public Token getErrorToken() {
	
		return new Token(getEntireLine(), getLineNumber(), getLineMarker());
	}
	
	public String getEntireLine() {
	
		int temp = position;
		while(temp < text.length && text[temp] != '\n') {
			temp++;
		}
		
		return texts.substring(begin, temp);
	}
	
	public int getLineMarker() {
	
		return position - begin;
	}
	
	public boolean isNextString(final String n) {
	
		return position + n.length() <= text.length && texts.substring(position, position + n.length()).equals(n);
	}
	
	public boolean isNextChar(final char n) {
	
		return hasNext() && text[position] == n;
	}
	
	public char peek() {
	
		return hasNext() ? text[position] : (char) 0;
	}
	
	/**
	 * does a direct skip of n characters, use only when you know what the chars
	 * are.. this will not increment the line number counter
	 */
	public void skip(final int n) {
	
		position += n;
	}
	
	/** returns the string consisting of the next n characters. */
	public String next(final int n) {
	
		final StringBuffer buffer = new StringBuffer();
		
		for (int x = 0; x < n; x++) {
			buffer.append(next());
		}
		
		return buffer.toString();
	}
	
	/** moves the iterator forward one char */
	public char next() {
	
		final char current = text[position];
		
		if (position > 0 && text[position - 1] == '\n') {
			lineNo++;
			begin = position;
		}
		
		position++;
		
		return current;
	}
	
	public void mark() {
	
		mark1.add(0, new Integer(position));
		mark2.add(0, new Integer(lineNo));
	}
	
	public String reset() {
	
		final Integer temp1 = mark1.removeFirst();
		final Integer temp2 = mark2.removeFirst();
		//      position = temp1.intValue();
		//      lineNo   = temp2.intValue();
		
		return texts.substring(temp1, position);
	}
	
	protected LinkedList<Integer> mark1 = new LinkedList<Integer>();
	
	protected LinkedList<Integer> mark2 = new LinkedList<Integer>();
	
	public static void main(final String args[]) {
	
		final StringIterator temp = new StringIterator(args[0]);
		
		StringBuffer blah = new StringBuffer();
		while(temp.hasNext()) {
			final char t = temp.next();
			blah.append(t);
			if (t == '\n') {
				System.out.print(temp.getLineNumber() + ": " + blah.toString());
				blah = new StringBuffer();
			}
		}
	}
}
