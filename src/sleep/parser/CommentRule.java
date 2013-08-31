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

public class CommentRule extends Rule {
	
	@Override
	public int getType() {
	
		return Rule.PRESERVE_SINGLE;
	}
	
	@Override
	public String toString() {
	
		return "Comment parsing information";
	}
	
	@Override
	public String wrap(final String value) {
	
		final StringBuffer rv = new StringBuffer(value.length() + 2);
		rv.append('#');
		rv.append(value);
		rv.append('\n');
		
		return rv.toString();
	}
	
	@Override
	public boolean isLeft(final char n) {
	
		return n == '#';
	}
	
	@Override
	public boolean isRight(final char n) {
	
		return n == '\n';
	}
	
	@Override
	public boolean isMatch(final char n) {
	
		return false;
	}
	
	@Override
	public boolean isBalanced() {
	
		return true;
	}
	
	@Override
	public Rule copyRule() {
	
		return this; // we're safe doing this since comment rules contain no state information.
	}
	
	/** Used to keep track of opening braces to check balance later on */
	@Override
	public void witnessOpen(final Token token) {
	
	}
	
	/** Used to keep track of closing braces to check balance later on */
	@Override
	public void witnessClose(final Token token) {
	
	}
}
