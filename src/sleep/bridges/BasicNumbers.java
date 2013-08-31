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

import java.math.BigInteger;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;
import java.util.Stack;

import sleep.engine.types.DoubleValue;
import sleep.engine.types.IntValue;
import sleep.engine.types.LongValue;
import sleep.interfaces.Function;
import sleep.interfaces.Loadable;
import sleep.interfaces.Operator;
import sleep.interfaces.Predicate;
import sleep.runtime.Scalar;
import sleep.runtime.ScalarType;
import sleep.runtime.ScriptInstance;
import sleep.runtime.SleepUtils;

/** provides some of the basic number crunching functionality */
public class BasicNumbers implements Predicate, Operator, Loadable, Function {
	
	/**
     * 
     */
	private static final long serialVersionUID = -8025766585004185949L;
	
	@Override
	public void scriptUnloaded(final ScriptInstance aScript) {
	
	}
	
	@Override
	public void scriptLoaded(final ScriptInstance aScript) {
	
		final Hashtable temp = aScript.getScriptEnvironment().getEnvironment();
		
		final Object sanitized = sleep.taint.TaintUtils.Sanitizer(this);
		
		// math ops..
		
		final String funcs[] = new String[] { "&abs", "&acos", "&asin", "&atan", "&atan2", "&ceil", "&cos", "&log", "&round", "&sin", "&sqrt", "&tan", "&radians", "&degrees", "&exp", "&floor", "&sum" };
		
		for (final String func : funcs) {
			temp.put(func, sanitized);
		}
		
		// functions
		temp.put("&double", sanitized);
		temp.put("&int", sanitized);
		temp.put("&uint", sanitized);
		temp.put("&long", sanitized);
		
		temp.put("&parseNumber", sanitized);
		temp.put("&formatNumber", sanitized);
		
		// basic operators
		temp.put("+", sanitized);
		temp.put("-", sanitized);
		temp.put("/", sanitized);
		temp.put("*", sanitized);
		temp.put("**", sanitized); // exponentation
		
		/* why "% "?  we had an amibiguity with %() to initialize hash literals and n % (expr) 
		   for normal math ops.  the initial parser in the case of mod will preserve one bit of
		   whitespace to try to prevent mass hysteria and confusion to the parser for determining
		   wether an op is being used or a hash literal is being initialized */
		temp.put("% ", sanitized);
		
		temp.put("<<", sanitized);
		temp.put(">>", sanitized);
		temp.put("&", sanitized);
		temp.put("|", sanitized);
		temp.put("^", sanitized);
		temp.put("&not", sanitized);
		
		// predicates
		temp.put("==", this);
		temp.put("!=", this);
		temp.put("<=", this);
		temp.put(">=", this);
		temp.put("<", this);
		temp.put(">", this);
		temp.put("is", this);
		
		// functions
		temp.put("&rand", sanitized);
		temp.put("&srand", sanitized);
	}
	
	@Override
	public Scalar evaluate(final String name, final ScriptInstance si, final Stack args) {
	
		if (name.equals("&abs")) {
			return SleepUtils.getScalar(Math.abs(BridgeUtilities.getDouble(args, 0.0)));
		} else if (name.equals("&acos")) {
			return SleepUtils.getScalar(Math.acos(BridgeUtilities.getDouble(args, 0.0)));
		} else if (name.equals("&asin")) {
			return SleepUtils.getScalar(Math.asin(BridgeUtilities.getDouble(args, 0.0)));
		} else if (name.equals("&atan")) {
			return SleepUtils.getScalar(Math.atan(BridgeUtilities.getDouble(args, 0.0)));
		} else if (name.equals("&atan2")) {
			return SleepUtils.getScalar(Math.atan2(BridgeUtilities.getDouble(args, 0.0), BridgeUtilities.getDouble(args, 0.0)));
		} else if (name.equals("&ceil")) {
			return SleepUtils.getScalar(Math.ceil(BridgeUtilities.getDouble(args, 0.0)));
		} else if (name.equals("&floor")) {
			return SleepUtils.getScalar(Math.floor(BridgeUtilities.getDouble(args, 0.0)));
		} else if (name.equals("&cos")) {
			return SleepUtils.getScalar(Math.cos(BridgeUtilities.getDouble(args, 0.0)));
		} else if (name.equals("&log") && args.size() == 1) {
			return SleepUtils.getScalar(Math.log(BridgeUtilities.getDouble(args, 0.0)));
		} else if (name.equals("&log") && args.size() == 2) {
			return SleepUtils.getScalar(Math.log(BridgeUtilities.getDouble(args, 0.0)) / Math.log(BridgeUtilities.getDouble(args, 0.0)));
		} else if (name.equals("&round")) {
			if (args.size() == 1) {
				return SleepUtils.getScalar(Math.round(BridgeUtilities.getDouble(args, 0.0)));
			} else {
				/* round to a certain number of places--if the argument is significantly large, this function could break */
				double number = BridgeUtilities.getDouble(args, 0.0);
				final double places = Math.pow(10, BridgeUtilities.getInt(args, 0));
				
				number = Math.round(number * places);
				number = number / places;
				return SleepUtils.getScalar(number);
			}
		} else if (name.equals("&sin")) {
			return SleepUtils.getScalar(Math.sin(BridgeUtilities.getDouble(args, 0.0)));
		} else if (name.equals("&sqrt")) {
			return SleepUtils.getScalar(Math.sqrt(BridgeUtilities.getDouble(args, 0.0)));
		} else if (name.equals("&tan")) {
			return SleepUtils.getScalar(Math.tan(BridgeUtilities.getDouble(args, 0.0)));
		} else if (name.equals("&radians")) {
			return SleepUtils.getScalar(Math.toRadians(BridgeUtilities.getDouble(args, 0.0)));
		} else if (name.equals("&degrees")) {
			return SleepUtils.getScalar(Math.toDegrees(BridgeUtilities.getDouble(args, 0.0)));
		} else if (name.equals("&exp")) {
			return SleepUtils.getScalar(Math.exp(BridgeUtilities.getDouble(args, 0.0)));
		} else if (name.equals("&sum")) {
			final Iterator i = BridgeUtilities.getIterator(args, si);
			
			List iterators = null;
			if (args.size() >= 1) {
				/* auxillary iterators */
				iterators = new LinkedList();
				while(!args.isEmpty()) {
					iterators.add(BridgeUtilities.getIterator(args, si));
				}
			}
			
			double result = 0.0;
			double temp;
			
			/* this is a simple sum of an array or iterator */
			if (iterators == null) {
				while(i.hasNext()) {
					result += ((Scalar) i.next()).doubleValue();
				}
			}
			/* this is for summing the products of multiple arrays or iterators */
			else {
				while(i.hasNext()) {
					temp = ((Scalar) i.next()).doubleValue();
					
					final Iterator j = iterators.iterator();
					while(j.hasNext()) {
						final Iterator tempi = (Iterator) j.next();
						if (tempi.hasNext()) {
							temp *= ((Scalar) tempi.next()).doubleValue();
						} else {
							temp = 0.0;
							break;
						}
					}
					
					result += temp;
				}
			}
			
			return SleepUtils.getScalar(result);
		} else if (name.equals("&not")) {
			final ScalarType sa = ((Scalar) args.pop()).getActualValue(); /* we already assume this is a number */
			
			if (sa.getType() == IntValue.class) {
				return SleepUtils.getScalar(~sa.intValue());
			}
			
			return SleepUtils.getScalar(~sa.longValue());
		} else if (name.equals("&long")) {
			final Scalar temp = BridgeUtilities.getScalar(args);
			return SleepUtils.getScalar(temp.longValue());
		} else if (name.equals("&double")) {
			final Scalar temp = BridgeUtilities.getScalar(args);
			return SleepUtils.getScalar(temp.doubleValue());
		} else if (name.equals("&int")) {
			final Scalar temp = BridgeUtilities.getScalar(args);
			return SleepUtils.getScalar(temp.intValue());
		} else if (name.equals("&uint")) {
			final int temp = BridgeUtilities.getInt(args, 0);
			final long templ = 0x00000000FFFFFFFFL & temp;
			return SleepUtils.getScalar(templ);
		} else if (name.equals("&parseNumber")) {
			final String number = BridgeUtilities.getString(args, "0");
			final int radix = BridgeUtilities.getInt(args, 10);
			
			final BigInteger temp = new BigInteger(number, radix);
			return SleepUtils.getScalar(temp.longValue());
		} else if (name.equals("&formatNumber")) {
			final String number = BridgeUtilities.getString(args, "0");
			
			int from = 10, to = 10;
			
			if (args.size() == 2) {
				from = BridgeUtilities.getInt(args, 10);
			}
			
			to = BridgeUtilities.getInt(args, 10);
			
			final BigInteger temp = new BigInteger(number, from);
			return SleepUtils.getScalar(temp.toString(to));
		} else if (name.equals("&srand")) {
			final long seed = BridgeUtilities.getLong(args);
			si.getMetadata().put("__RANDOM__", new Random(seed));
		} else if (name.equals("&rand")) {
			if (si.getMetadata().get("__RANDOM__") == null) {
				si.getMetadata().put("__RANDOM__", new Random());
			}
			final Random r = (Random) si.getMetadata().get("__RANDOM__");
			
			if (!args.isEmpty()) {
				final Scalar temp = (Scalar) args.pop();
				
				if (temp.getArray() != null) {
					final int potential = r.nextInt(temp.getArray().size());
					return temp.getArray().getAt(potential);
				} else {
					return SleepUtils.getScalar(r.nextInt(temp.intValue()));
				}
			}
			
			return SleepUtils.getScalar(r.nextDouble());
		}
		
		return SleepUtils.getEmptyScalar();
	}
	
	@Override
	public boolean decide(final String n, final ScriptInstance i, final Stack l) {
	
		final Stack env = i.getScriptEnvironment().getEnvironmentStack();
		final Scalar vb = (Scalar) l.pop();
		final Scalar va = (Scalar) l.pop();
		
		if (n.equals("is")) {
			return va.objectValue() == vb.objectValue(); /* could be anything! */
		}
		
		final ScalarType sb = vb.getActualValue();
		final ScalarType sa = va.getActualValue();
		
		if (sa.getType() == DoubleValue.class || sb.getType() == DoubleValue.class) {
			final double a = sa.doubleValue();
			final double b = sb.doubleValue();
			
			if (n.equals("==")) {
				return a == b;
			}
			if (n.equals("!=")) {
				return a != b;
			}
			if (n.equals("<=")) {
				return a <= b;
			}
			if (n.equals(">=")) {
				return a >= b;
			}
			if (n.equals("<")) {
				return a < b;
			}
			if (n.equals(">")) {
				return a > b;
			}
		} else if (sa.getType() == LongValue.class || sb.getType() == LongValue.class) {
			final long a = sa.longValue();
			final long b = sb.longValue();
			
			if (n.equals("==")) {
				return a == b;
			}
			if (n.equals("!=")) {
				return a != b;
			}
			if (n.equals("<=")) {
				return a <= b;
			}
			if (n.equals(">=")) {
				return a >= b;
			}
			if (n.equals("<")) {
				return a < b;
			}
			if (n.equals(">")) {
				return a > b;
			}
		} else {
			final int a = sa.intValue();
			final int b = sb.intValue();
			
			if (n.equals("==")) {
				return a == b;
			}
			if (n.equals("!=")) {
				return a != b;
			}
			if (n.equals("<=")) {
				return a <= b;
			}
			if (n.equals(">=")) {
				return a >= b;
			}
			if (n.equals("<")) {
				return a < b;
			}
			if (n.equals(">")) {
				return a > b;
			}
		}
		
		return false;
	}
	
	@Override
	public Scalar operate(final String o, final ScriptInstance i, final Stack locals) {
	
		final ScalarType left = ((Scalar) locals.pop()).getActualValue();
		final ScalarType right = ((Scalar) locals.pop()).getActualValue();
		
		if ((right.getType() == DoubleValue.class || left.getType() == DoubleValue.class) && !(o.equals(">>") || o.equals("<<") || o.equals("&") || o.equals("|") || o.equals("^"))) {
			final double a = left.doubleValue();
			final double b = right.doubleValue();
			
			if (o.equals("+")) {
				return SleepUtils.getScalar(a + b);
			}
			if (o.equals("-")) {
				return SleepUtils.getScalar(a - b);
			}
			if (o.equals("*")) {
				return SleepUtils.getScalar(a * b);
			}
			if (o.equals("/")) {
				return SleepUtils.getScalar(a / b);
			}
			if (o.equals("% ")) {
				return SleepUtils.getScalar(a % b);
			}
			if (o.equals("**")) {
				return SleepUtils.getScalar(Math.pow(a, b));
			}
		} else if (right.getType() == LongValue.class || left.getType() == LongValue.class) {
			final long a = left.longValue();
			final long b = right.longValue();
			
			if (o.equals("+")) {
				return SleepUtils.getScalar(a + b);
			}
			if (o.equals("-")) {
				return SleepUtils.getScalar(a - b);
			}
			if (o.equals("*")) {
				return SleepUtils.getScalar(a * b);
			}
			if (o.equals("/")) {
				return SleepUtils.getScalar(a / b);
			}
			if (o.equals("% ")) {
				return SleepUtils.getScalar(a % b);
			}
			if (o.equals("**")) {
				return SleepUtils.getScalar(Math.pow(a, b));
			}
			if (o.equals(">>")) {
				return SleepUtils.getScalar(a >> b);
			}
			if (o.equals("<<")) {
				return SleepUtils.getScalar(a << b);
			}
			if (o.equals("&")) {
				return SleepUtils.getScalar(a & b);
			}
			if (o.equals("|")) {
				return SleepUtils.getScalar(a | b);
			}
			if (o.equals("^")) {
				return SleepUtils.getScalar(a ^ b);
			}
		} else {
			final int a = left.intValue();
			final int b = right.intValue();
			
			if (o.equals("+")) {
				return SleepUtils.getScalar(a + b);
			}
			if (o.equals("-")) {
				return SleepUtils.getScalar(a - b);
			}
			if (o.equals("*")) {
				return SleepUtils.getScalar(a * b);
			}
			if (o.equals("/")) {
				return SleepUtils.getScalar(a / b);
			}
			if (o.equals("% ")) {
				return SleepUtils.getScalar(a % b);
			}
			if (o.equals("**")) {
				return SleepUtils.getScalar(Math.pow(a, b));
			}
			if (o.equals(">>")) {
				return SleepUtils.getScalar(a >> b);
			}
			if (o.equals("<<")) {
				return SleepUtils.getScalar(a << b);
			}
			if (o.equals("&")) {
				return SleepUtils.getScalar(a & b);
			}
			if (o.equals("|")) {
				return SleepUtils.getScalar(a | b);
			}
			if (o.equals("^")) {
				return SleepUtils.getScalar(a ^ b);
			}
		}
		
		return SleepUtils.getEmptyScalar();
	}
}
