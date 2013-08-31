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

import java.util.Hashtable;

/**
 * A class that provides a bunch of static methods for checking a stream of
 * sleep tokens for a certain lexical structure.
 */
public class Checkers {
	
	/**
	 * a hashtable that keeps track of language keywords so they are not
	 * mistaken for function names
	 */
	protected static Hashtable<String, Boolean> keywords;
	
	public static void addKeyword(final String keyword) {
	
		Checkers.keywords.put(keyword, Boolean.TRUE);
	}
	
	static {
		Checkers.keywords = new Hashtable<String, Boolean>();
		
		Checkers.keywords.put("if", Boolean.TRUE);
		Checkers.keywords.put("for", Boolean.TRUE);
		Checkers.keywords.put("while", Boolean.TRUE);
		Checkers.keywords.put("foreach", Boolean.TRUE);
		Checkers.keywords.put("&&", Boolean.TRUE);
		Checkers.keywords.put("||", Boolean.TRUE);
		Checkers.keywords.put("EOT", Boolean.TRUE);
		Checkers.keywords.put("EOL", Boolean.TRUE);
		Checkers.keywords.put("return", Boolean.TRUE);
		Checkers.keywords.put("halt", Boolean.TRUE);
		Checkers.keywords.put("done", Boolean.TRUE);
		Checkers.keywords.put("break", Boolean.TRUE);
		Checkers.keywords.put("continue", Boolean.TRUE);
		Checkers.keywords.put("yield", Boolean.TRUE);
		Checkers.keywords.put("throw", Boolean.TRUE);
		Checkers.keywords.put("try", Boolean.TRUE);
		Checkers.keywords.put("catch", Boolean.TRUE);
		Checkers.keywords.put("assert", Boolean.TRUE);
	}
	
	public static boolean isIfStatement(final String a, final String b, final String c) {
	
		return a.toString().equals("if") && Checkers.isExpression(b.toString()) && Checkers.isBlock(c.toString());
	}
	
	public static boolean isElseStatement(final String a, final String b) {
	
		return a.equals("else") && Checkers.isBlock(b);
	}
	
	public static boolean isElseIfStatement(final String a, final String b, final String c, final String d) {
	
		return a.equals("else") && Checkers.isIfStatement(b, c, d);
	}
	
	public static final boolean isIncrementHack(final String a) {
	
		return Checkers.isScalar(a) && a.length() > 3 && a.substring(a.length() - 2, a.length()).equals("++");
	}
	
	public static final boolean isDecrementHack(final String a) {
	
		return Checkers.isScalar(a) && a.length() > 3 && a.substring(a.length() - 2, a.length()).equals("--");
	}
	
	public static final boolean isObjectNew(final String a, final String b) {
	
		return a.equals("new");
	}
	
	public static final boolean isClosureCall(final String a, final String b) {
	
		return b.equals("EOT");
	}
	
	public static final boolean isImportStatement(final String a, final String b) {
	
		return a.equals("import");
	}
	
	public static final boolean isClassLiteral(final String a) {
	
		return a.length() >= 2 && a.charAt(0) == '^';
	}
	
	public static final boolean isClassPiece(final String a) {
	
		if (a.length() >= 1 && !Checkers.isVariable(a) && Character.isJavaIdentifierStart(a.charAt(0))) {
			for (int x = 1; x < a.length(); x++) {
				if (!Character.isJavaIdentifierPart(a.charAt(x)) && a.charAt(x) != '.') {
					return false;
				}
			}
			return true;
		}
		return false;
	}
	
	public static final boolean isClassIdentifier(final Parser parser, final String a) {
	
		return Checkers.isClassPiece(a) && parser.findImportedClass(a) != null;
	}
	
	public static final boolean isBindFilter(final String a, final String b, final String c, final String d) {
	
		return Checkers.isBlock(d);
	}
	
	public static final boolean isBindPredicate(final String a, final String b, final String c) {
	
		return Checkers.isExpression(b) && Checkers.isBlock(c);
	}
	
	public static final boolean isBind(final String a, final String b, final String c) {
	
		return !b.equals("=") && Checkers.isBlock(c);
	}
	
	public static boolean isHash(final String a) {
	
		return a.charAt(0) == '%';
	}
	
	public static boolean isArray(final String a) {
	
		return a.charAt(0) == '@';
	}
	
	public static boolean isFunctionReferenceToken(final String a) {
	
		return a.charAt(0) == '&' && a.length() > 1 && !a.equals("&&");
	}
	
	public static final boolean isVariableReference(final String temp) {
	
		return temp.length() >= 3 && temp.charAt(0) == '\\' && !temp.equals("\\$null") && Checkers.isVariable(temp.substring(1));
	}
	
	public static final boolean isVariable(final String temp) {
	
		return Checkers.isScalar(temp) || Checkers.isHash(temp) || Checkers.isArray(temp);
	}
	
	public static final boolean isScalar(final String temp) {
	
		return temp.charAt(0) == '$';
	}
	
	public static boolean isIndex(final String a) {
	
		return a.charAt(0) == '[' && a.charAt(a.length() - 1) == ']';
	}
	
	public static boolean isExpression(final String a) {
	
		return a.charAt(0) == '(' && a.charAt(a.length() - 1) == ')';
	}
	
	public static boolean isBlock(final String a) {
	
		return a.charAt(0) == '{' && a.charAt(a.length() - 1) == '}';
	}
	
	public static boolean isFunctionCall(final String a, final String b) {
	
		return (Checkers.isFunction(a) || a.equals("@") || a.equals("%")) && Checkers.isExpression(b);
	}
	
	public static boolean isFunction(final String a) {
	
		return (Character.isJavaIdentifierStart(a.charAt(0)) || a.charAt(0) == '&') && a.charAt(0) != '$' && Checkers.keywords.get(a) == null;
	}
	
	public static boolean isDataLiteral(final String a) {
	
		return a.length() > 2 && (a.substring(0, 2).equals("@(") || a.substring(0, 2).equals("%("));
	}
	
	public static boolean isFunctionCall(final String a) {
	
		return (Checkers.isFunction(a) || Checkers.isDataLiteral(a)) && a.indexOf('(') > -1 && a.indexOf(')') > -1;
	}
	
	public static boolean isIndexableItem(final String a, final String b) {
	
		return Checkers.isIndex(b) && (Checkers.isFunctionCall(a) || Checkers.isExpression(a) || Checkers.isVariable(a) || Checkers.isIndex(a) || Checkers.isFunctionReferenceToken(a) || Checkers.isBacktick(a));
	}
	
	public static boolean isIndexableItem(final String a) {
	
		if (a.charAt(a.length() - 1) == ']') {
			final int idx = a.lastIndexOf('[');
			if (idx > 0) {
				return Checkers.isIndexableItem(a.substring(0, idx), a.substring(idx, a.length()));
			}
		}
		
		return false;
	}
	
	public static boolean isHashIndex(final String a) {
	
		return Checkers.isHash(a) && a.indexOf('[') > -1 && a.indexOf(']') > -1;
	}
	
	public static boolean isArrayIndex(final String a) {
	
		return Checkers.isArray(a) && a.indexOf('[') > -1 && a.indexOf(']') > -1;
	}
	
	public static boolean isOperator(final String a, final String b, final String c) {
	
		return true;
	}
	
	public static final boolean isSpecialWhile(final String a, final String b, final String c, final String d) {
	
		return Checkers.isWhile(a, c, d) && Checkers.isVariable(b);
	}
	
	public static final boolean isWhile(final String a, final String b, final String c) {
	
		return a.equals("while") && Checkers.isExpression(b) && Checkers.isBlock(c);
	}
	
	public static final boolean isFor(final String a, final String b, final String c) {
	
		return a.equals("for") && Checkers.isExpression(b) && Checkers.isBlock(c);
	}
	
	public static final boolean isTryCatch(final String a, final String b, final String c, final String d, final String e) {
	
		return a.equals("try") && c.equals("catch") && Checkers.isBlock(b) && Checkers.isBlock(e) && Checkers.isScalar(d);
	}
	
	public static final boolean isForeach(final String a, final String b, final String c, final String d) {
	
		return a.equals("foreach") && Checkers.isVariable(b) && Checkers.isExpression(c) && Checkers.isBlock(d);
	}
	
	public static final boolean isSpecialForeach(final String a, final String b, final String c, final String d, final String e, final String f) {
	
		return a.equals("foreach") && Checkers.isVariable(b) && c.equals("=>") && Checkers.isVariable(d) && Checkers.isExpression(e) && Checkers.isBlock(f);
	}
	
	public static final boolean isAssert(final String temp) {
	
		return temp.equals("assert");
	}
	
	public static final boolean isReturn(final String temp) {
	
		// halt and done are kind of jIRC related... when you write the scripting language you
		// can do whatever you want...
		return temp.equals("return") || temp.equals("done") || temp.equals("halt") || temp.equals("break") || temp.equals("yield") || temp.equals("continue") || temp.equals("throw") || temp.equals("callcc");
	}
	
	public static final boolean isString(final String item) {
	
		return item.charAt(0) == '\"' && item.charAt(item.length() - 1) == '\"';
	}
	
	public static final boolean isBacktick(final String item) {
	
		return item.charAt(0) == '`' && item.charAt(item.length() - 1) == '`';
	}
	
	public static final boolean isLiteral(final String item) {
	
		return item.charAt(0) == '\'' && item.charAt(item.length() - 1) == '\'';
	}
	
	public static final boolean isNumber(String temp) {
	
		try {
			if (temp.endsWith("L")) {
				temp = temp.substring(0, temp.length() - 1);
				Long.decode(temp);
			} else {
				Integer.decode(temp);
			}
		} catch (final Exception hex) {
			return false;
		}
		return true;
	}
	
	public static final boolean isDouble(final String temp) {
	
		try {
			Double.parseDouble(temp);
		} catch (final Exception hex) {
			return false;
		}
		return true;
	}
	
	public static final boolean isBoolean(final String temp) {
	
		return temp.equals("true") || temp.equals("false");
	}
	
	public static final boolean isBiPredicate(final String a, final String b, final String c) {
	
		return true;
	}
	
	public static final boolean isUniPredicate(final String a, final String b) {
	
		return a.charAt(0) == '-' || a.length() > 1 && a.charAt(0) == '!' && a.charAt(1) == '-';
	}
	
	public static final boolean isAndPredicate(final String a, final String b, final String c) {
	
		return b.equals("&&");
	}
	
	public static final boolean isOrPredicate(final String a, final String b, final String c) {
	
		return b.equals("||");
	}
	
	public static final boolean isComment(final String a) {
	
		return a.charAt(0) == '#' && a.charAt(a.length() - 1) == '\n';
	}
	
	public static final boolean isEndOfVar(final char n) {
	
		return n == ' ' || n == '\t' || n == '\n' || n == '$' || n == '\\';
	}
}
