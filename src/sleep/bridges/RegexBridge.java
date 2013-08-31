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

import java.util.Collections;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Stack;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import sleep.interfaces.Function;
import sleep.interfaces.Loadable;
import sleep.interfaces.Predicate;
import sleep.parser.ParserConfig;
import sleep.runtime.Scalar;
import sleep.runtime.ScriptEnvironment;
import sleep.runtime.ScriptInstance;
import sleep.runtime.SleepUtils;
import sleep.taint.TaintUtils;

/** Provides a bridge between Java's regex API and sleep. Rock on */
public class RegexBridge implements Loadable {
	
	private static Map patternCache = Collections.synchronizedMap(new Cache(128));
	
	private static class Cache extends LinkedHashMap {
		
		/**
         * 
         */
		private static final long serialVersionUID = 3598751767142046909L;
		
		protected int count;
		
		public Cache(final int count) {
		
			super(11, 0.75f, true);
			this.count = count;
		}
		
		@Override
		protected boolean removeEldestEntry(final Map.Entry eldest) {
		
			return size() >= count;
		}
	}
	
	static {
		ParserConfig.addKeyword("ismatch");
		ParserConfig.addKeyword("hasmatch");
	}
	
	private static Pattern getPattern(final String pattern) {
	
		Pattern temp = (Pattern) RegexBridge.patternCache.get(pattern);
		
		if (temp != null) {
			return temp;
		} else {
			temp = Pattern.compile(pattern);
			RegexBridge.patternCache.put(pattern, temp);
			
			return temp;
		}
	}
	
	@Override
	public void scriptUnloaded(final ScriptInstance aScript) {
	
	}
	
	@Override
	public void scriptLoaded(final ScriptInstance aScript) {
	
		final Hashtable temp = aScript.getScriptEnvironment().getEnvironment();
		
		final isMatch matcher = new isMatch();
		
		// predicates
		temp.put("ismatch", matcher);
		temp.put("hasmatch", matcher);
		
		// functions
		temp.put("&matched", matcher);
		temp.put("&split", new split());
		temp.put("&join", new join());
		temp.put("&matches", new getMatches());
		temp.put("&replace", new rreplace());
		temp.put("&find", new ffind());
	}
	
	private static class ffind implements Function {
		
		/**
         * 
         */
		private static final long serialVersionUID = -6792108973495222660L;
		
		@Override
		public Scalar evaluate(final String n, final ScriptInstance i, final Stack l) {
		
			final String string = BridgeUtilities.getString(l, "");
			final String patterns = BridgeUtilities.getString(l, "");
			
			final Pattern pattern = RegexBridge.getPattern(patterns);
			final Matcher matchit = pattern.matcher(string);
			final int start = BridgeUtilities.normalize(BridgeUtilities.getInt(l, 0), string.length());
			
			final boolean check = matchit.find(start);
			
			if (check) {
				i.getScriptEnvironment().setContextMetadata("matcher", SleepUtils.getScalar(matchit));
			} else {
				i.getScriptEnvironment().setContextMetadata("matcher", null);
			}
			
			return check ? SleepUtils.getScalar(matchit.start()) : SleepUtils.getEmptyScalar();
		}
	}
	
	private static String key(final String text, final Pattern p) {
	
		final StringBuffer buffer = new StringBuffer(text.length() + p.pattern().length() + 1);
		buffer.append(text);
		buffer.append(p.pattern());
		
		return buffer.toString();
	}
	
	private static Scalar getLastMatcher(final ScriptEnvironment env) {
	
		final Scalar temp = (Scalar) env.getContextMetadata("matcher");
		return temp == null ? SleepUtils.getEmptyScalar() : temp;
	}
	
	/** a helper utility to get the matcher out of the script environment */
	private static Scalar getMatcher(final ScriptEnvironment env, final String key, final String text, final Pattern p) {
	
		Map matchers = (Map) env.getContextMetadata("matchers");
		
		if (matchers == null) {
			matchers = new Cache(16);
			env.setContextMetadata("matchers", matchers);
		}
		
		/* get our value */
		
		Scalar temp = (Scalar) matchers.get(key);
		
		if (temp == null) {
			temp = SleepUtils.getScalar(p.matcher(text));
			matchers.put(key, temp);
			return temp;
		} else {
			return temp;
		}
	}
	
	private static class isMatch implements Predicate, Function {
		
		/**
         * 
         */
		private static final long serialVersionUID = -592338356077189061L;
		
		@Override
		public boolean decide(final String n, final ScriptInstance i, final Stack l) {
		
			boolean rv;
			
			/* do some tainter checking plz */
			final Scalar bb = (Scalar) l.pop(); // PATTERN
			final Scalar aa = (Scalar) l.pop(); // TEXT TO MATCH AGAINST
			
			final Pattern pattern = RegexBridge.getPattern(bb.toString());
			
			Scalar container = null;
			Matcher matcher = null;
			
			if (n.equals("hasmatch")) {
				final String key = RegexBridge.key(aa.toString(), pattern);
				
				container = RegexBridge.getMatcher(i.getScriptEnvironment(), key, aa.toString(), pattern);
				matcher = (Matcher) container.objectValue();
				
				rv = matcher.find();
				
				if (!rv) {
					final Map matchers = (Map) i.getScriptEnvironment().getContextMetadata("matchers");
					if (matchers != null) {
						matchers.remove(key);
					}
				}
			} else {
				matcher = pattern.matcher(aa.toString());
				container = SleepUtils.getScalar(matcher);
				
				rv = matcher.matches();
			}
			
			/* check our taint value please */
			if (TaintUtils.isTainted(aa) || TaintUtils.isTainted(bb)) {
				TaintUtils.taintAll(container);
			}
			
			/* set our matcher for retrieval by matched() later */
			i.getScriptEnvironment().setContextMetadata("matcher", rv ? container : null);
			
			return rv;
		}
		
		@Override
		public Scalar evaluate(final String n, final ScriptInstance i, final Stack l) {
		
			final Scalar value = SleepUtils.getArrayScalar();
			
			final Scalar container = RegexBridge.getLastMatcher(i.getScriptEnvironment());
			
			if (!SleepUtils.isEmptyScalar(container)) {
				final Matcher matcher = (Matcher) container.objectValue();
				
				final int count = matcher.groupCount();
				
				for (int x = 1; x <= count; x++) {
					value.getArray().push(SleepUtils.getScalar(matcher.group(x)));
				}
			}
			
			return TaintUtils.isTainted(container) ? TaintUtils.taintAll(value) : value;
		}
	}
	
	private static class getMatches implements Function {
		
		/**
         * 
         */
		private static final long serialVersionUID = 1021706688338297893L;
		
		@Override
		public Scalar evaluate(final String n, final ScriptInstance i, final Stack l) {
		
			final String a = ((Scalar) l.pop()).toString();
			final String b = ((Scalar) l.pop()).toString();
			final int c = BridgeUtilities.getInt(l, -1);
			final int d = BridgeUtilities.getInt(l, c);
			
			final Pattern pattern = RegexBridge.getPattern(b);
			final Matcher matcher = pattern.matcher(a);
			
			Scalar value = SleepUtils.getArrayScalar();
			
			int temp = 0;
			
			while(matcher.find()) {
				final int count = matcher.groupCount();
				
				if (temp == c) {
					value = SleepUtils.getArrayScalar();
				}
				
				for (int x = 1; x <= count; x++) {
					value.getArray().push(SleepUtils.getScalar(matcher.group(x)));
				}
				
				if (temp == d) {
					return value;
				}
				
				temp++;
			}
			
			return value;
		}
	}
	
	private static class split implements Function {
		
		/**
         * 
         */
		private static final long serialVersionUID = -874472899013604597L;
		
		@Override
		public Scalar evaluate(final String n, final ScriptInstance i, final Stack l) {
		
			final String a = ((Scalar) l.pop()).toString();
			final String b = ((Scalar) l.pop()).toString();
			
			final Pattern pattern = RegexBridge.getPattern(a);
			
			final String results[] = l.isEmpty() ? pattern.split(b) : pattern.split(b, BridgeUtilities.getInt(l, 0));
			
			final Scalar array = SleepUtils.getArrayScalar();
			
			for (final String result : results) {
				array.getArray().push(SleepUtils.getScalar(result));
			}
			
			return array;
		}
	}
	
	private static class join implements Function {
		
		/**
         * 
         */
		private static final long serialVersionUID = -6539149064296000713L;
		
		@Override
		public Scalar evaluate(final String n, final ScriptInstance script, final Stack l) {
		
			final String a = ((Scalar) l.pop()).toString();
			final Iterator i = BridgeUtilities.getIterator(l, script);
			
			final StringBuffer result = new StringBuffer();
			
			if (i.hasNext()) {
				result.append(i.next().toString());
			}
			
			while(i.hasNext()) {
				result.append(a);
				result.append(i.next().toString());
			}
			
			return SleepUtils.getScalar(result.toString());
		}
	}
	
	private static class rreplace implements Function {
		
		/**
         * 
         */
		private static final long serialVersionUID = -2814846069198435323L;
		
		@Override
		public Scalar evaluate(final String n, final ScriptInstance script, final Stack l) {
		
			final String a = BridgeUtilities.getString(l, ""); // current
			final String b = BridgeUtilities.getString(l, ""); // old
			final String c = BridgeUtilities.getString(l, ""); // new
			final int d = BridgeUtilities.getInt(l, -1);
			
			final StringBuffer rv = new StringBuffer();
			
			final Pattern pattern = RegexBridge.getPattern(b);
			final Matcher matcher = pattern.matcher(a);
			
			int matches = 0;
			
			while(matcher.find() && matches != d) {
				matcher.appendReplacement(rv, c);
				matches++;
			}
			
			matcher.appendTail(rv);
			
			return SleepUtils.getScalar(rv.toString());
		}
	}
}
