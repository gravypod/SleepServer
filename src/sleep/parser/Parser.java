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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.util.Iterator;
import java.util.LinkedList;

import sleep.engine.Block;
import sleep.engine.GeneratedSteps;
import sleep.error.SyntaxError;
import sleep.error.YourCodeSucksException;

public class Parser {
	
	private static final boolean DEBUG_ITER = false;
	
	private static final boolean DEBUG_LEX = false;
	
	private static final boolean DEBUG_COMMENTS = false;
	
	private static final boolean DEBUG_TPARSER = false;
	
	protected String code;
	
	/** the actual "code" for the script file. */
	protected String name;
	
	/** an identifier for the script file. */
	
	protected LinkedList comments = new LinkedList();
	
	/** a list of all of the comments from the script file */
	protected LinkedList errors = new LinkedList();
	
	/** a list of all of the parser errors */
	protected LinkedList warnings = new LinkedList();
	
	/** a list of all of the parser warnings */
	
	protected TokenList tokens = new TokenList();
	
	protected LinkedList statements = new LinkedList();
	
	/** a list of all of the statements */
	
	protected Block executeMe; // runnable block   
	
	public char EndOfTerm = ';';
	
	protected ImportManager imports;
	
	/** obtain the import manager, used for managing imported packages. */
	public ImportManager getImportManager() {
	
		return imports;
	}
	
	/** the factory to use when generating Sleep code */
	protected GeneratedSteps factory = null;
	
	/** set the code factory to be used to generated Sleep code */
	public void setCodeFactory(final GeneratedSteps s) {
	
		factory = s;
	}
	
	/** Used by Sleep to import statement to save an imported package name. */
	public void importPackage(final String packagez, final String from) {
	
		imports.importPackage(packagez, from);
	}
	
	/**
	 * Attempts to find a class, starts out with the passed in string itself, if
	 * that doesn't resolve then the string is appended to each imported package
	 * to see where the class might exist
	 */
	public Class findImportedClass(final String name) {
	
		return imports.findImportedClass(name);
	}
	
	public void setEndOfTerm(final char c) {
	
		EndOfTerm = c;
	}
	
	/** initialize the parser with the code you want me to work with */
	public Parser(final String _code) {
	
		this("unknown", _code);
	}
	
	/** initialize the parser with the code you want me to work with */
	public Parser(final String _name, final String _code) {
	
		this(_name, _code, null);
	}
	
	/**
	 * initialize the parser with the code you want me to work with plus a
	 * shared import manager
	 */
	public Parser(final String _name, final String _code, ImportManager imps) {
	
		if (imps == null) {
			imps = new ImportManager();
		}
		imports = imps;
		
		importPackage("java.lang.*", null);
		importPackage("java.util.*", null);
		importPackage("sleep.runtime.*", null);
		
		code = _code;
		name = _name;
	}
	
	public void addStatement(final Statement state) {
	
		statements.add(state);
	}
	
	public LinkedList getStatements() {
	
		return statements;
	}
	
	/**
	 * returns the identifier representing the source of the script we're
	 * parsing
	 */
	public String getName() {
	
		return name;
	}
	
	public void parse() throws YourCodeSucksException {
	
		parse(new StringIterator(code));
	}
	
	public void parse(final StringIterator siter) throws YourCodeSucksException {
	
		final TokenList tokens = LexicalAnalyzer.GroupBlockTokens(this, siter);
		
		/** debug the tokenizer */
		if (Parser.DEBUG_LEX) {
			final Token[] all = tokens.getTokens();
			for (int x = 0; x < all.length; x++) {
				System.out.println(x + ": " + all[x].toString() + " at " + all[x].getHint());
			}
		}
		
		if (hasErrors()) {
			errors.addAll(warnings);
			throw new YourCodeSucksException(errors);
		}
		
		final LinkedList<Statement> statements = TokenParser.ParseBlocks(this, tokens);
		
		if (Parser.DEBUG_TPARSER && statements != null) {
			final Iterator i = statements.iterator();
			while(i.hasNext()) {
				System.out.println("Block\n" + i.next());
			}
		}
		
		if (hasErrors()) {
			errors.addAll(warnings);
			throw new YourCodeSucksException(errors);
		}
		
		final CodeGenerator codegen = new CodeGenerator(this, factory);
		codegen.parseBlock(statements);
		
		if (hasErrors()) {
			errors.addAll(warnings);
			throw new YourCodeSucksException(errors);
		}
		
		executeMe = codegen.getRunnableBlock();
		
		if (Parser.DEBUG_COMMENTS) {
			final Iterator i = comments.iterator();
			while(i.hasNext()) {
				System.out.print("Comment: " + i.next());
			}
		}
	}
	
	public void reportError(final String description, final Token responsible) {
	
		errors.add(new SyntaxError(description, responsible.toString(), responsible.getHint()));
	}
	
	public void reportErrorWithMarker(final String description, final Token responsible) {
	
		errors.add(new SyntaxError(description, responsible.toString(), responsible.getHint(), responsible.getMarker()));
	}
	
	public void reportError(final SyntaxError error) {
	
		errors.add(error);
	}
	
	public Block getRunnableBlock() {
	
		return executeMe;
	}
	
	public void reportWarning(final String description, final Token responsible) {
	
		warnings.add(new SyntaxError(description, responsible.toString(), responsible.getHint()));
	}
	
	public boolean hasErrors() {
	
		return errors.size() > 0;
	}
	
	public boolean hasWarnings() {
	
		return warnings.size() > 0;
	}
	
	public void addComment(final String text) {
	
		comments.add(text);
	}
	
	public static void main(final String args[]) {
	
		try {
			final File afile = new File(args[0]);
			final BufferedReader temp = new BufferedReader(new InputStreamReader(new FileInputStream(afile)));
			
			final StringBuffer data = new StringBuffer();
			
			String text;
			while((text = temp.readLine()) != null) {
				data.append(text);
				data.append('\n');
			}
			
			final Parser p = new Parser(data.toString());
			p.parse();
			System.out.println(p.getRunnableBlock());
		} catch (final YourCodeSucksException yex) {
			final LinkedList errors = yex.getErrors();
			final Iterator i = errors.iterator();
			while(i.hasNext()) {
				final SyntaxError anError = (SyntaxError) i.next();
				System.out.println("Error: " + anError.getDescription() + " at line " + anError.getLineNumber());
				System.out.println("       " + anError.getCodeSnippet());
				if (anError.getMarker() != null) {
					System.out.println("       " + anError.getMarker());
				}
			}
		} catch (final Exception ex) {
			ex.printStackTrace();
		}
	}
}
