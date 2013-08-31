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

import java.io.File;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

/**
 * This class mantains a cache of imported package names and resolve classes for
 * a Sleep parser. The existence of this class also allows these imports to be
 * shared between parser instances. Value is allowing dynamically parsed code
 * like eval, expr, compile_clousre etc.. to inherit their parents imported
 * class information.
 */
public class ImportManager {
	
	protected List<String> imports = new LinkedList<String>();
	
	protected HashMap<String, Class<?>> classes = new HashMap<String, Class<?>>();
	
	/** Used by Sleep to import statement to save an imported package name. */
	public void importPackage(final String packagez, final String from) {
	
		String pack, clas;
		
		if (packagez.indexOf(".") > -1) {
			clas = packagez.substring(packagez.lastIndexOf(".") + 1, packagez.length());
			pack = packagez.substring(0, packagez.lastIndexOf("."));
		} else {
			clas = packagez;
			pack = null;
		}
		
		/* resolve and setup our class loader for the specified jar file */
		
		if (from != null) {
			File returnValue = null;
			returnValue = ParserConfig.findJarFile(from);
			
			if (returnValue == null || !returnValue.exists()) {
				throw new RuntimeException("jar file to import package from was not found!");
			}
			
			addFile(returnValue);
		}
		
		/* handle importing our package */
		
		if (clas.equals("*")) {
			imports.add(pack);
		} else if (pack == null) {
			imports.add(packagez);
			final Class<?> found = resolveClass(null, packagez); /* try with no package to see if we have an anonymous class */
			classes.put(packagez, found);
			
			if (found == null) {
				throw new RuntimeException("imported class was not found");
			}
		} else {
			imports.add(packagez);
			
			final Class<?> found = findImportedClass(packagez);
			classes.put(clas, found);
			
			if (found == null) {
				throw new RuntimeException("imported class was not found");
			}
		}
	}
	
	/**
	 * This method is used by Sleep to resolve a specific class (or at least
	 * try)
	 */
	private Class<?> resolveClass(final String pack, final String clas) {
	
		final StringBuffer name = new StringBuffer();
		if (pack != null) {
			name.append(pack);
			name.append(".");
		}
		name.append(clas);
		
		try {
			return Class.forName(name.toString());
		} catch (final Exception ex) {
		}
		
		return null;
	}
	
	/** A hack to add a jar to the system classpath courtesy of Ralph Becker. */
	private void addFile(final File f) {
	
		try {
			final URL url = f.toURI().toURL();
			
			final URLClassLoader sysloader = (URLClassLoader) ClassLoader.getSystemClassLoader();
			final Class<URLClassLoader> sysclass = java.net.URLClassLoader.class;
			
			final Method method = sysclass.getDeclaredMethod("addURL", new Class[] { URL.class });
			method.setAccessible(true);
			method.invoke(sysloader, new Object[] { url });
		} catch (final Throwable t) {
			t.printStackTrace();
			throw new RuntimeException("Error, could not add " + f + " to system classloader - " + t.getMessage());
		}
	}
	
	/**
	 * Attempts to find a class, starts out with the passed in string itself, if
	 * that doesn't resolve then the string is appended to each imported package
	 * to see where the class might exist
	 */
	public Class<?> findImportedClass(final String name) {
	
		if (classes.get(name) == null) {
			Class<?> rv = null;
			String clas, pack;
			
			if (name.indexOf(".") > -1) {
				clas = name.substring(name.lastIndexOf(".") + 1, name.length());
				pack = name.substring(0, name.lastIndexOf("."));
				
				rv = resolveClass(pack, clas);
			} else {
				rv = resolveClass(null, name); /* try with no package to see if we have an anonymous class */
				
				final Iterator<String> i = imports.iterator();
				while(i.hasNext() && rv == null) {
					rv = resolveClass((String) i.next(), name);
				}
			}
			
			// some friendly (really) debugging
			/*          if (rv == null)
			          {
			             System.err.println("Argh: " + name + " is not an imported class");
			             Thread.dumpStack();
			          } */
			
			classes.put(name, rv);
		}
		
		return (Class<?>) classes.get(name);
	}
}
