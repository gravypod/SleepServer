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
package sleep.bridges;

import java.util.Comparator;
import java.util.Hashtable;
import java.util.Stack;

import sleep.interfaces.Function;
import sleep.interfaces.Loadable;
import sleep.interfaces.Operator;
import sleep.interfaces.Predicate;
import sleep.parser.ParserConfig;
import sleep.runtime.Scalar;
import sleep.runtime.ScalarArray;
import sleep.runtime.ScalarType;
import sleep.runtime.ScriptInstance;
import sleep.runtime.SleepUtils;

/** provides basic string parsing facilities */
public class BasicStrings implements Loadable, Predicate {
	
	static {
		// if an operator followed by an expression could be mistaken for
		// a function call, then we need to register the operator as a keyword.
		// :)
		ParserConfig.addKeyword("x");
		ParserConfig.addKeyword("eq");
		ParserConfig.addKeyword("ne");
		ParserConfig.addKeyword("lt");
		ParserConfig.addKeyword("gt");
		ParserConfig.addKeyword("isin");
		ParserConfig.addKeyword("iswm");
		ParserConfig.addKeyword("cmp");
	}
	
	@Override
	public void scriptUnloaded(final ScriptInstance aScript) {
	
	}
	
	@Override
	public void scriptLoaded(final ScriptInstance aScript) {
	
		final Hashtable temp = aScript.getScriptEnvironment().getEnvironment();
		
		// functions
		temp.put("&left", new func_left());
		temp.put("&right", new func_right());
		
		temp.put("&charAt", new func_charAt());
		temp.put("&byteAt", temp.get("&charAt"));
		temp.put("&uc", new func_uc());
		temp.put("&lc", new func_lc());
		
		final func_substr f_substr = new func_substr();
		temp.put("&substr", f_substr);
		temp.put("&mid", f_substr);
		
		temp.put("&indexOf", new func_indexOf());
		temp.put("&lindexOf", temp.get("&indexOf"));
		temp.put("&strlen", new func_strlen());
		temp.put("&strrep", new func_strrep());
		temp.put("&replaceAt", new func_replaceAt());
		
		temp.put("&tr", new func_tr());
		
		temp.put("&asc", new func_asc());
		temp.put("&chr", new func_chr());
		
		temp.put("&sort", new func_sort());
		
		final func_sorters funky = new func_sorters();
		temp.put("&sorta", funky);
		temp.put("&sortn", funky);
		temp.put("&sortd", funky);
		
		// predicates
		temp.put("eq", this);
		temp.put("ne", this);
		temp.put("lt", this);
		temp.put("gt", this);
		
		temp.put("-isletter", this);
		temp.put("-isnumber", this);
		
		temp.put("-isupper", this);
		temp.put("-islower", this);
		
		temp.put("isin", this);
		temp.put("iswm", new pred_iswm()); // I couldn't resist >)
		
		// operators
		temp.put(".", new oper_concat());
		temp.put("x", new oper_multiply());
		temp.put("cmp", new oper_compare());
		temp.put("<=>", new oper_spaceship());
	}
	
	@Override
	public boolean decide(final String n, final ScriptInstance i, final Stack l) {
	
		if (l.size() == 1) {
			final String a = BridgeUtilities.getString(l, "");
			
			if (n.equals("-isupper")) {
				return a.toUpperCase().equals(a);
			} else if (n.equals("-islower")) {
				return a.toLowerCase().equals(a);
			} else if (n.equals("-isletter")) {
				if (a.length() <= 0) {
					return false;
				}
				
				for (int x = 0; x < a.length(); x++) {
					if (!Character.isLetter(a.charAt(x))) {
						return false;
					}
				}
				
				return true;
			} else if (n.equals("-isnumber")) {
				if (a.length() <= 0) {
					return false;
				}
				
				if (a.indexOf('.') > -1 && a.indexOf('.') != a.lastIndexOf('.')) {
					return false;
				}
				
				for (int x = 0; x < a.length(); x++) {
					if (!Character.isDigit(a.charAt(x)) && (a.charAt(x) != '.' || x + 1 >= a.length())) {
						return false;
					}
				}
				
				return true;
			}
		} else {
			final String b = BridgeUtilities.getString(l, "");
			final String a = BridgeUtilities.getString(l, "");
			
			if (n.equals("eq")) {
				return a.equals(b);
			} else if (n.equals("ne")) {
				return !a.equals(b);
			} else if (n.equals("isin")) {
				return b.indexOf(a) > -1;
			} else if (n.equals("gt")) {
				return a.compareTo(b) > 0;
			} else if (n.equals("lt")) {
				return a.compareTo(b) < 0;
			}
		}
		
		return false;
	}
	
	private static class pred_iswm implements Predicate {
		
		@Override
		public boolean decide(final String name, final ScriptInstance script, final Stack locals) {
		
			final String b = locals.pop().toString();
			final String a = locals.pop().toString();
			
			try {
				if ((a.length() == 0 || b.length() == 0) && a.length() != b.length()) {
					return false;
				}
				
				int aptr = 0, bptr = 0, cptr;
				
				while(aptr < a.length()) {
					if (a.charAt(aptr) == '*') {
						final boolean greedy = aptr + 1 < a.length() && a.charAt(aptr + 1) == '*';
						
						while(a.charAt(aptr) == '*') {
							aptr++;
							if (aptr == a.length()) {
								return true;
							}
						}
						
						for (cptr = aptr; cptr < a.length() && a.charAt(cptr) != '?' && a.charAt(cptr) != '\\' && a.charAt(cptr) != '*'; cptr++) {
						} // body intentionally left empty.
						
						if (cptr != aptr) // don't advance our bptr unless there is some non-wildcard pattern to look for next in the string
						{
							if (greedy) {
								cptr = b.lastIndexOf(a.substring(aptr, cptr));
							} else {
								cptr = b.indexOf(a.substring(aptr, cptr), bptr);
							}
							
							if (cptr == -1 || cptr < bptr) // < - require 0 or more chars, <= - requires 1 or more chars
							{
								return false;
							}
							
							bptr = cptr;
						}
						
						if (a.charAt(aptr) == '?') // if the current aptr is a ?, decrement so the loop can deal with it on the next round
						{
							aptr--;
						}
					} else if (bptr >= b.length()) {
						return false;
					} else if (a.charAt(aptr) == '\\') {
						aptr++;
						
						if (aptr < a.length() && a.charAt(aptr) != b.charAt(bptr)) {
							return false;
						}
					} else if (a.charAt(aptr) != '?' && a.charAt(aptr) != b.charAt(bptr)) {
						return false;
					}
					
					aptr++;
					bptr++;
				}
				return bptr == b.length();
			} catch (final Exception ex) {
				ex.printStackTrace();
			}
			
			return false;
		}
		
	}
	
	private static class func_left implements Function {
		
		/**
         * 
         */
		private static final long serialVersionUID = 3679313199380019886L;
		
		@Override
		public Scalar evaluate(final String n, final ScriptInstance i, final Stack l) {
		
			final String temp = l.pop().toString();
			final int value = ((Scalar) l.pop()).intValue();
			
			return SleepUtils.getScalar(BasicStrings.substring(n, temp, 0, value));
		}
	}
	
	private static class func_tr implements Function {
		
		/**
         * 
         */
		private static final long serialVersionUID = 594755995110155926L;
		
		@Override
		public Scalar evaluate(final String n, final ScriptInstance i, final Stack l) {
		
			final String old = BridgeUtilities.getString(l, "");
			final String pattern = BridgeUtilities.getString(l, "");
			final String mapper = BridgeUtilities.getString(l, "");
			final String optstr = BridgeUtilities.getString(l, "");
			
			int options = 0;
			
			if (optstr.indexOf('c') > -1) {
				options = options | Transliteration.OPTION_COMPLEMENT;
			}
			if (optstr.indexOf('d') > -1) {
				options = options | Transliteration.OPTION_DELETE;
			}
			if (optstr.indexOf('s') > -1) {
				options = options | Transliteration.OPTION_SQUEEZE;
			}
			
			final Transliteration temp = Transliteration.compile(pattern, mapper, options);
			
			return SleepUtils.getScalar(temp.translate(old));
		}
	}
	
	private static class func_right implements Function {
		
		/**
         * 
         */
		private static final long serialVersionUID = -6219987364746665680L;
		
		@Override
		public Scalar evaluate(final String n, final ScriptInstance i, final Stack l) {
		
			final String temp = l.pop().toString();
			final int value = ((Scalar) l.pop()).intValue();
			
			return SleepUtils.getScalar(BasicStrings.substring(n, temp, 0 - value, temp.length()));
		}
	}
	
	private static class func_asc implements Function {
		
		/**
         * 
         */
		private static final long serialVersionUID = -857442686301628643L;
		
		@Override
		public Scalar evaluate(final String n, final ScriptInstance i, final Stack l) {
		
			return SleepUtils.getScalar(BridgeUtilities.getString(l, "\u0000").charAt(0));
		}
	}
	
	private static class func_chr implements Function {
		
		/**
         * 
         */
		private static final long serialVersionUID = -6338002997491120492L;
		
		@Override
		public Scalar evaluate(final String n, final ScriptInstance i, final Stack l) {
		
			return SleepUtils.getScalar((char) BridgeUtilities.getInt(l) + "");
		}
	}
	
	private static class func_uc implements Function {
		
		/**
         * 
         */
		private static final long serialVersionUID = -7748076952889472064L;
		
		@Override
		public Scalar evaluate(final String n, final ScriptInstance i, final Stack l) {
		
			return SleepUtils.getScalar(l.pop().toString().toUpperCase());
		}
	}
	
	private static class func_lc implements Function {
		
		/**
         * 
         */
		private static final long serialVersionUID = -5111902851524104920L;
		
		@Override
		public Scalar evaluate(final String n, final ScriptInstance i, final Stack l) {
		
			return SleepUtils.getScalar(l.pop().toString().toLowerCase());
		}
	}
	
	private static class func_strlen implements Function {
		
		/**
         * 
         */
		private static final long serialVersionUID = -6805009611901900068L;
		
		@Override
		public Scalar evaluate(final String n, final ScriptInstance i, final Stack l) {
		
			return SleepUtils.getScalar(l.pop().toString().length());
		}
	}
	
	private static class func_strrep implements Function {
		
		/**
         * 
         */
		private static final long serialVersionUID = -5625057723389787548L;
		
		@Override
		public Scalar evaluate(final String n, final ScriptInstance i, final Stack l) {
		
			final StringBuffer work = new StringBuffer(BridgeUtilities.getString(l, ""));
			
			while(!l.isEmpty()) {
				final String oldstr = BridgeUtilities.getString(l, "");
				final String newstr = BridgeUtilities.getString(l, "");
				
				if (oldstr.length() == 0) {
					continue;
				}
				
				int x = 0;
				final int oldlen = oldstr.length();
				final int newlen = newstr.length();
				
				while((x = work.indexOf(oldstr, x)) > -1) {
					work.replace(x, x + oldlen, newstr);
					x += newstr.length();
				}
			}
			
			return SleepUtils.getScalar(work.toString());
		}
	}
	
	private static class func_replaceAt implements Function {
		
		/**
         * 
         */
		private static final long serialVersionUID = -5485251381164577452L;
		
		@Override
		public Scalar evaluate(final String n, final ScriptInstance i, final Stack l) {
		
			final StringBuffer work = new StringBuffer(BridgeUtilities.getString(l, ""));
			final String nstr = BridgeUtilities.getString(l, "");
			final int index = BridgeUtilities.normalize(BridgeUtilities.getInt(l, 0), work.length());
			final int nchar = BridgeUtilities.getInt(l, nstr.length());
			
			work.delete(index, index + nchar);
			work.insert(index, nstr);
			
			return SleepUtils.getScalar(work.toString());
		}
	}
	
	private static class func_substr implements Function {
		
		/**
         * 
         */
		private static final long serialVersionUID = 374727239317162579L;
		
		@Override
		public Scalar evaluate(final String n, final ScriptInstance i, final Stack l) {
		
			final String value = BridgeUtilities.getString(l, "");
			
			int start, stop;
			start = BridgeUtilities.getInt(l);
			
			if (n.equals("&mid")) {
				stop = BridgeUtilities.getInt(l, value.length() - start) + start;
			} else {
				stop = BridgeUtilities.getInt(l, value.length());
			}
			
			return SleepUtils.getScalar(BasicStrings.substring(n, value, start, stop));
		}
	}
	
	private static class func_indexOf implements Function {
		
		/**
         * 
         */
		private static final long serialVersionUID = 314967010295916531L;
		
		@Override
		public Scalar evaluate(final String n, final ScriptInstance i, final Stack l) {
		
			final String value = l.pop().toString();
			final String item = l.pop().toString();
			
			int rv;
			
			if (n.equals("&lindexOf")) {
				final int start = BridgeUtilities.normalize(BridgeUtilities.getInt(l, value.length()), value.length());
				rv = value.lastIndexOf(item, start);
			} else {
				final int start = BridgeUtilities.normalize(BridgeUtilities.getInt(l, 0), value.length());
				rv = value.indexOf(item, start);
			}
			
			if (rv == -1) {
				return SleepUtils.getEmptyScalar();
			}
			return SleepUtils.getScalar(rv);
		}
	}
	
	private static class func_charAt implements Function {
		
		/**
         * 
         */
		private static final long serialVersionUID = 5070007748400855838L;
		
		@Override
		public Scalar evaluate(final String n, final ScriptInstance i, final Stack l) {
		
			final String value = l.pop().toString();
			final int start = BridgeUtilities.getInt(l);
			
			if (n.equals("&charAt")) {
				return SleepUtils.getScalar(BasicStrings.charAt(value, start) + "");
			} else {
				return SleepUtils.getScalar(BasicStrings.charAt(value, start));
			}
		}
	}
	
	private static class func_sort implements Function {
		
		/**
         * 
         */
		private static final long serialVersionUID = -3797786600478933678L;
		
		@Override
		public Scalar evaluate(final String n, final ScriptInstance i, final Stack l) {
		
			if (l.size() != 2) {
				throw new IllegalArgumentException("&sort requires a function to specify how to sort the data");
			}
			
			final Function my_func = BridgeUtilities.getFunction(l, i);
			final ScalarArray array = BridgeUtilities.getWorkableArray(l);
			
			if (my_func == null) {
				return SleepUtils.getArrayScalar();
			}
			
			array.sort(new CompareFunction(my_func, i));
			return SleepUtils.getArrayScalar(array);
		}
	}
	
	private static class func_sorters implements Function {
		
		/**
         * 
         */
		private static final long serialVersionUID = 5362322515933074807L;
		
		@Override
		public Scalar evaluate(final String n, final ScriptInstance i, final Stack l) {
		
			final ScalarArray array = BridgeUtilities.getWorkableArray(l);
			
			if (n.equals("&sorta")) {
				array.sort(new CompareStrings());
			} else if (n.equals("&sortn")) {
				array.sort(new CompareNumbers());
			} else if (n.equals("&sortd")) {
				array.sort(new CompareDoubles());
			}
			
			return SleepUtils.getArrayScalar(array);
		}
	}
	
	private static class CompareFunction implements Comparator {
		
		protected SleepClosure func;
		
		protected ScriptInstance script;
		
		protected Stack locals;
		
		public CompareFunction(final Function _func, final ScriptInstance _script) {
		
			func = (SleepClosure) _func;
			script = _script;
			locals = new Stack();
		}
		
		@Override
		public int compare(final Object a, final Object b) {
		
			locals.push(b);
			locals.push(a);
			
			final Scalar temp = func.callClosure("&sort", script, locals);
			return temp.intValue();
		}
	}
	
	private static class CompareNumbers implements Comparator {
		
		@Override
		public int compare(final Object a, final Object b) {
		
			final long aa = ((Scalar) a).longValue();
			final long bb = ((Scalar) b).longValue();
			
			return (int) (aa - bb);
		}
	}
	
	private static class CompareDoubles implements Comparator {
		
		@Override
		public int compare(final Object a, final Object b) {
		
			final double aa = ((Scalar) a).doubleValue();
			final double bb = ((Scalar) b).doubleValue();
			
			if (aa == bb) {
				return 0;
			}
			
			if (aa < bb) {
				return -1;
			}
			
			return 1;
		}
	}
	
	private static class CompareStrings implements Comparator {
		
		@Override
		public int compare(final Object a, final Object b) {
		
			return a.toString().compareTo(b.toString());
		}
	}
	
	private static class oper_concat implements Operator {
		
		@Override
		public Scalar operate(final String o, final ScriptInstance i, final Stack l) {
		
			final Scalar left = (Scalar) l.pop();
			final Scalar right = (Scalar) l.pop();
			
			if (o.equals(".")) {
				return SleepUtils.getScalar(left.toString() + right.toString());
			}
			
			return null;
		}
	}
	
	private static class oper_multiply implements Operator {
		
		@Override
		public Scalar operate(final String o, final ScriptInstance i, final Stack l) {
		
			final Scalar left = (Scalar) l.pop();
			final Scalar right = (Scalar) l.pop();
			
			final String str = left.toString();
			final int num = right.intValue();
			
			final StringBuffer value = new StringBuffer();
			
			for (int x = 0; x < num; x++) {
				value.append(str);
			}
			
			return SleepUtils.getScalar(value);
		}
	}
	
	private static class oper_compare implements Operator {
		
		@Override
		public Scalar operate(final String o, final ScriptInstance i, final Stack l) {
		
			final Scalar left = (Scalar) l.pop();
			final Scalar right = (Scalar) l.pop();
			
			return SleepUtils.getScalar(left.toString().compareTo(right.toString()));
		}
	}
	
	private static class oper_spaceship implements Operator {
		
		@Override
		public Scalar operate(final String o, final ScriptInstance i, final Stack l) {
		
			final ScalarType left = BridgeUtilities.getScalar(l).getActualValue();
			final ScalarType right = BridgeUtilities.getScalar(l).getActualValue();
			
			if (left.getType() == sleep.engine.types.DoubleValue.class || right.getType() == sleep.engine.types.DoubleValue.class) {
				if (left.doubleValue() > right.doubleValue()) {
					return SleepUtils.getScalar(1);
				} else if (left.doubleValue() < right.doubleValue()) {
					return SleepUtils.getScalar(-1);
				}
			} else {
				if (left.longValue() > right.longValue()) {
					return SleepUtils.getScalar(1);
				} else if (left.longValue() < right.longValue()) {
					return SleepUtils.getScalar(-1);
				}
			}
			return SleepUtils.getScalar(0);
		}
	}
	
	/**
	 * Normalizes the start/end parameters based on the length of the string and
	 * returns a substring. Strings normalized in this way will be able to
	 * accept negative indices for their parameters.
	 */
	private static final String substring(final String func, final String str, final int _start, final int _end) {
	
		final int length = str.length();
		int start, end;
		
		start = BridgeUtilities.normalize(_start, length);
		end = _end < 0 ? _end + length : _end;
		end = end <= length ? end : length;
		
		if (start == end) {
			return "";
		} else if (start > end) {
			throw new IllegalArgumentException(func + ": illegal substring('" + str + "', " + _start + " -> " + start + ", " + _end + " -> " + end + ") indices");
		}
		
		return str.substring(start, end);
	}
	
	/**
	 * Normalizes the start parameter based on the length of the string and
	 * returns a character. Functions with parameters normalized in this way
	 * will be able to accept nagative indices for their parameters
	 */
	private static final char charAt(final String str, final int start) {
	
		return str.charAt(BridgeUtilities.normalize(start, str.length()));
	}
}
