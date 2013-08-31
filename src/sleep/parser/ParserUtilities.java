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

import java.util.Iterator;

public class ParserUtilities {
	
	public static Token combineTokens(final Token a, final Token b) {
	
		return new Token(a.toString() + b.toString(), a.getHint());
	}
	
	public static Token makeToken(final String token, final Token a) {
	
		return new Token(token, a.getHint());
	}
	
	public static Token[] get(final Token[] t, final int a, final int b) {
	
		final Token rv[] = new Token[b - a];
		for (int x = 0; x < rv.length; x++) {
			rv[x] = t[a + x];
		}
		return rv;
	}
	
	public static Token join(final Token[] temp) {
	
		return ParserUtilities.join(temp, " ");
	}
	
	public static Token join(final Token[] temp, final String with) {
	
		final StringBuffer rv = new StringBuffer();
		
		for (int x = 0; x < temp.length; x++) {
			if (x > 0 && temp[x].getHint() == temp[x - 1].getTopHint() || x == 0) {
				rv.append(with);
			} else {
				final int difference = temp[x].getHint() - temp[x - 1].getTopHint();
				for (int z = 0; z < difference; z++) {
					rv.append("\n");
				}
			}
			
			rv.append(temp[x].toString());
		}
		
		return new Token(rv.toString(), temp[0].getHint());
	}
	
	public static Token extract(final Token temp) {
	
		return new Token(ParserUtilities.extract(temp.toString()), temp.getHint());
	}
	
	public static String extract(final String temp) {
	
		return temp.substring(1, temp.length() - 1);
	}
	
	/**
	 * breaks down the token into sub tokens that are one "term" wide, in the
	 * case of blocks separated by ;
	 */
	public static TokenList groupByBlockTerm(final Parser parser, final Token smokin) {
	
		final StringIterator iterator = new StringIterator(smokin.toString(), smokin.getHint());
		final TokenList tokens = LexicalAnalyzer.GroupBlockTokens(parser, iterator);
		return ParserUtilities.groupByTerm(tokens);
	}
	
	/**
	 * breaks down the token into sub tokens that are one "term" wide, in the
	 * case of messages separated by :
	 */
	public static TokenList groupByMessageTerm(final Parser parser, final Token smokin) {
	
		final StringIterator iterator = new StringIterator(smokin.toString(), smokin.getHint());
		final TokenList tokens = LexicalAnalyzer.GroupExpressionIndexTokens(parser, iterator);
		return ParserUtilities.groupByTerm(tokens);
	}
	
	/**
	 * breaks down the token into sub tokens that are one "term" wide, a termi
	 * in the case of parameters it uses ,
	 */
	public static TokenList groupByParameterTerm(final Parser parser, final Token smokin) {
	
		final StringIterator iterator = new StringIterator(smokin.toString(), smokin.getHint());
		final TokenList tokens = LexicalAnalyzer.GroupParameterTokens(parser, iterator);
		return ParserUtilities.groupByTerm(tokens);
	}
	
	private static TokenList groupByTerm(final TokenList n) {
	
		final TokenList rv = new TokenList();
		
		if (n.getList().size() == 0) {
			return rv;
		}
		
		StringBuffer current = new StringBuffer();
		
		int hint = -1;
		
		final Iterator i = n.getList().iterator();
		while(i.hasNext()) {
			final Token temp = (Token) i.next();
			hint = hint == -1 ? temp.getHint() : hint;
			
			if (temp.toString().equals("EOT")) {
				rv.add(new Token(current.toString(), hint));
				current = new StringBuffer();
				hint = -1; /* reset hint to prevent line # skew */
			} else {
				if (current.length() > 0) {
					current.append(" ");
				}
				
				current.append(temp.toString());
			}
		}
		
		if (current.length() > 0) {
			rv.add(new Token(current.toString(), hint));
		}
		
		return rv;
	}
}
