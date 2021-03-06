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

import sleep.error.SyntaxError;

public class Rule {
	
	public static int PRESERVE_ENTITY = 100;
	
	public static int PRESERVE_SINGLE = 101;
	
	int type;
	
	char left;
	
	char right;
	
	char single;
	
	String errorDescription1, errorDescription2;
	
	public int getType() {
	
		return type;
	}
	
	@Override
	public String toString() {
	
		return errorDescription1;
	}
	
	public SyntaxError getSyntaxError() {
	
		while(open.size() > 0 && close.size() > 0) {
			open.removeLast();
			close.removeLast();
		}
		
		String desc;
		
		if (type == Rule.PRESERVE_ENTITY && open.size() > 0) {
			desc = errorDescription2;
		} else {
			desc = errorDescription1;
		}
		
		Token token;
		
		if (open.size() > 0) {
			token = (Token) open.getFirst();
		} else {
			token = (Token) close.getFirst();
		}
		
		open.clear();
		close.clear();
		
		return new SyntaxError(desc, token.toString(), token.getHint(), token.getMarker());
	}
	
	public String wrap(final String value) {
	
		final StringBuffer rv = new StringBuffer(value.length() + 2);
		if (type == Rule.PRESERVE_ENTITY) {
			rv.append(left);
			rv.append(value);
			rv.append(right);
		} else {
			rv.append(single);
			rv.append(value);
			rv.append(single);
		}
		
		return rv.toString();
	}
	
	public boolean isLeft(final char n) {
	
		return type == Rule.PRESERVE_ENTITY && left == n;
	}
	
	public boolean isRight(final char n) {
	
		return type == Rule.PRESERVE_ENTITY && right == n;
	}
	
	public boolean isMatch(final char n) {
	
		return type == Rule.PRESERVE_SINGLE && single == n;
	}
	
	protected LinkedList<Token> open = new LinkedList<Token>();
	
	protected LinkedList<Token> close = new LinkedList<Token>();
	
	public boolean isBalanced() {
	
		if (open.size() == close.size()) {
			open.clear();
			close.clear();
			return true;
		}
		return false;
	}
	
	/** Used to keep track of opening braces to check balance later on */
	public void witnessOpen(final Token token) {
	
		open.add(token);
		adjustLists();
	}
	
	/** Used to keep track of closing braces to check balance later on */
	public void witnessClose(final Token token) {
	
		if (type == Rule.PRESERVE_ENTITY) {
			close.addFirst(token);
		} else {
			close.add(token);
		}
		adjustLists();
	}
	
	private void adjustLists() {
	
		if (open.size() > 0 && close.size() > 0) {
			if (((Token) open.getLast()).getHint() == ((Token) close.getLast()).getHint()) {
				open.removeLast();
				close.removeLast();
			}
		}
	}
	
	public char getLeft() {
	
		return left;
	}
	
	public char getRight() {
	
		return right;
	}
	
	public Rule copyRule() {
	
		if (type == Rule.PRESERVE_ENTITY) {
			return new Rule(errorDescription1, errorDescription2, left, right);
		}
		
		return new Rule(errorDescription1, single);
	}
	
	public Rule(final String errorDesc1, final String errorDesc2, final char l, final char r) {
	
		type = Rule.PRESERVE_ENTITY;
		left = l;
		right = r;
		
		errorDescription1 = errorDesc1;
		errorDescription2 = errorDesc2;
	}
	
	public Rule(final String errorDesc, final char s) {
	
		type = Rule.PRESERVE_SINGLE;
		single = s;
		
		errorDescription1 = errorDesc;
	}
	
	public Rule() {
	
		// don't call me unless you really have to
	}
}
