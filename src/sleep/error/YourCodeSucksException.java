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
package sleep.error;

import java.io.OutputStream;
import java.io.PrintWriter;
import java.util.Iterator;
import java.util.LinkedList;

/**
 * Syntax errors are a reality of programming. Any time a syntax error occurs
 * when attempting to load a script the exception YourCodeSucksException will be
 * raised. [ yes, this exception name is staying ] <br>
 * <br>
 * To catch a YourCodeSucksException: <br>
 * 
 * <pre>
 * try {
 * 	ScriptInstance script;
 * 	script = loader.loadScript(&quot;name&quot;, inputStream);
 * } catch (YourCodeSucksException ex) {
 * 	Iterator i = ex.getErrors().iterator();
 * 	while(i.hasNext()) {
 * 		SyntaxError error = (SyntaxError) i.next();
 * 		
 * 		String description = error.getDescription();
 * 		String code = error.getCodeSnippet();
 * 		int lineNumber = error.getLineNumber();
 * 	}
 * }
 * </pre>
 * 
 * @see sleep.error.SyntaxError
 */
public class YourCodeSucksException extends RuntimeException {
	
	/**
     * 
     */
	private static final long serialVersionUID = 6285672326228481903L;
	
	LinkedList<SyntaxError> allErrors;
	
	/** Initialize the exception (sleep parser) */
	public YourCodeSucksException(final LinkedList<SyntaxError> myErrors) {
	
		allErrors = myErrors;
	}
	
	/**
	 * Returns a minimal string representation of the errors within this
	 * exception
	 */
	@Override
	public String getMessage() {
	
		final StringBuffer buf = new StringBuffer(allErrors.size() + " error(s): ");
		
		final Iterator<SyntaxError> i = getErrors().iterator();
		while(i.hasNext()) {
			final SyntaxError temp = i.next();
			
			buf.append(temp.getDescription());
			buf.append(" at " + temp.getLineNumber());
			
			if (i.hasNext()) {
				buf.append("; ");
			}
		}
		
		return buf.toString();
	}
	
	/**
	 * Returns a simple string representation of the errors within this
	 * exception
	 */
	@Override
	public String toString() {
	
		return "YourCodeSucksException: " + getMessage();
	}
	
	/**
	 * print a nicely formatted version of the script errors to the specified
	 * stream
	 */
	public void printErrors(final OutputStream out) {
	
		final PrintWriter pout = new PrintWriter(out);
		pout.print(formatErrors());
		pout.flush();
	}
	
	/**
	 * generate a nicely formatted string representation of the script errors in
	 * this exception
	 */
	public String formatErrors() {
	
		final StringBuffer representation = new StringBuffer();
		
		final LinkedList<SyntaxError> errors = getErrors();
		final Iterator<SyntaxError> i = errors.iterator();
		while(i.hasNext()) {
			final SyntaxError anError = i.next();
			representation.append("Error: " + anError.getDescription() + " at line " + anError.getLineNumber() + "\n");
			representation.append("       " + anError.getCodeSnippet() + "\n");
			
			if (anError.getMarker() != null) {
				representation.append("       " + anError.getMarker() + "\n");
			}
		}
		
		return representation.toString();
	}
	
	/**
	 * All of the errors are stored in a linked list. The linked list contains
	 * {@link sleep.error.SyntaxError SyntaxError} objects.
	 */
	public LinkedList<SyntaxError> getErrors() {
	
		return allErrors;
	}
}
