package fi.iki.elonen;

import java.io.File;
import java.io.FileInputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;

import sleep.runtime.ScriptLoader;

import com.gravypod.SleepServer.Site;
import com.gravypod.SleepServer.SleepCodeHandler;

import fi.iki.elonen.NanoHTTPD.Response.Status;

/**
 * Copyright (c) 2012-2013 by Paul S. Hawke, 2001,2005-2013 by Jarno Elonen,
 * 2010 by Konstantinos Togias
 * 
 * @author gravypod
 * 
 */
public class SimpleWebServer extends NanoHTTPD {
	
	private final Map<String, String> mimiTypes;
	
	private final String[] indexFiles;
	
	Site[] sites;
	
	public SimpleWebServer(Site[] sites, int[] sitePorts, Map<String, String> mimiTypes, String[] indexFiles) {
	
		super(sitePorts);
		this.sites = sites;
		this.indexFiles = indexFiles;
		this.mimiTypes = mimiTypes;
		
	}
	
	/**
	 * URL-encodes everything between "/"-characters. Encodes spaces as '%20'
	 * instead of '+'.
	 */
	private String encodeUri(final String uri) {
	
		String newUri = "";
		final StringTokenizer st = new StringTokenizer(uri, "/ ", true);
		while(st.hasMoreTokens()) {
			final String tok = st.nextToken();
			if (tok.equals("/")) {
				newUri += "/";
			} else if (tok.equals(" ")) {
				newUri += "%20";
			} else {
				try {
					newUri += URLEncoder.encode(tok, "UTF-8");
				} catch (final UnsupportedEncodingException ignored) {
				}
			}
		}
		return newUri;
	}
	
	private String listDirectory(final String uri, final File f) {
	
		final String heading = "Directory " + uri;
		String msg = "<html><head><title>" + heading + "</title><style><!--\n" + "span.dirname { font-weight: bold; }\n" + "span.filesize { font-size: 75%; }\n" + "// -->\n" + "</style>" + "</head><body><h1>" + heading + "</h1>";
		
		String up = null;
		if (uri.length() > 1) {
			final String u = uri.substring(0, uri.length() - 1);
			final int slash = u.lastIndexOf('/');
			if (slash >= 0 && slash < u.length()) {
				up = uri.substring(0, slash + 1);
			}
		}
		
		final List<String> files = Arrays.asList(f.list(new FilenameFilter() {
			
			@Override
			public boolean accept(final File dir, final String name) {
			
				return new File(dir, name).isFile();
			}
		}));
		Collections.sort(files);
		final List<String> directories = Arrays.asList(f.list(new FilenameFilter() {
			
			@Override
			public boolean accept(final File dir, final String name) {
			
				return new File(dir, name).isDirectory();
			}
		}));
		Collections.sort(directories);
		if (up != null || directories.size() + files.size() > 0) {
			msg += "<ul>";
			if (up != null || directories.size() > 0) {
				msg += "<section class=\"directories\">";
				if (up != null) {
					msg += "<li><a rel=\"directory\" href=\"" + up + "\"><span class=\"dirname\">..</span></a></b></li>";
				}
				for (int i = 0; i < directories.size(); i++) {
					
					final String dir = directories.get(i) + "/";
					msg += "<li><a rel=\"directory\" href=\"" + encodeUri(uri + dir) + "\"><span class=\"dirname\">" + dir + "</span></a></b></li>";
				}
				msg += "</section>";
			}
			if (files.size() > 0) {
				msg += "<section class=\"files\">";
				for (int i = 0; i < files.size(); i++) {
					final String file = files.get(i);
					
					msg += "<li><a href=\"" + encodeUri(uri + file) + "\"><span class=\"filename\">" + file + "</span></a>";
					final File curFile = new File(f, file);
					final long len = curFile.length();
					msg += "&nbsp;<span class=\"filesize\">(";
					if (len < 1024) {
						msg += len + " bytes";
					} else if (len < 1024 * 1024) {
						msg += len / 1024 + "." + len % 1024 / 10 % 100 + " KB";
					} else {
						msg += len / (1024 * 1024) + "." + len % (1024 * 1024) / 10 % 100 + " MB";
					}
					msg += ")</span></li>";
				}
				msg += "</section>";
			}
			msg += "</ul>";
		}
		msg += "</body></html>";
		return msg;
	}
	
	@Override
	public Response serve(String uri, final Method method, final Map<String, String> header, final Map<String, String> parms, final Map<String, String> files, final ScriptLoader loader) {
	
		// Make sure we won't die of an exception later
		
		// For VHosts
		String hostHeader = header.get("host");
		String hostIp = hostHeader.indexOf(':') > -1 ? hostHeader.substring(0, hostHeader.indexOf(':')) : hostHeader;
		
		Site currentSite = null;
		
		for (Site site : this.sites) {
			
			System.out.println(hostIp + " " + site.getHost() + " " + hostIp.contains(site.getHost()));
			if (hostIp.contains(site.getHost())) {
				currentSite = site;
				
			}
			
		}
		
		Response res = null;
		File rootDir = null;
		
		if (currentSite == null) {
			res = new Response(Status.UNAUTHORIZED, MIME_PLAINTEXT, "");
			System.out.println("Current site is not set (Not listening to that address)");
		} else {
			rootDir = new File(currentSite.getRootDir());
			System.out.println(hostIp + "'s rootDir is " + new File(currentSite.getRootDir()).getAbsolutePath());
		}
		
/*		File f = new File(rootDir, uri);
		
		if (res == null && !f.exists()) {
			res = new Response(Response.Status.NOT_FOUND, NanoHTTPD.MIME_PLAINTEXT, "Error 404, file not found.");
		}
		
		if (res == null) {
			// Remove URL arguments
			uri = uri.trim().replace(File.separatorChar, '/');
			if (uri.indexOf('?') >= 0) {
				uri = uri.substring(0, uri.indexOf('?'));
			}
			
			// Prohibit getting out of current directory
			if (uri.startsWith("src/main") || uri.endsWith("src/main") || uri.contains("../")) {
				res = new Response(Response.Status.FORBIDDEN, NanoHTTPD.MIME_PLAINTEXT, "FORBIDDEN: Won't serve ../ for security reasons.");
			}
		}
		
		// List the directory, if necessary
		if (res == null && f.isDirectory()) {
			// Browsers get confused without '/' after the
			// directory, send a redirect.
			if (!uri.endsWith("/")) {
				uri += "/";
				res = new Response(Response.Status.REDIRECT, NanoHTTPD.MIME_HTML, "<html><body>Redirected: <a href=\"" + uri + "\">" + uri + "</a></body></html>");
				res.addHeader("Location", uri);
			}
			
			if (res == null) {
				// First try index.html and index.htm
				
				boolean found = false;
				for (String file : indexFiles) {
					
					if (new File(f, file).exists()) {
						found = true;
						f = new File(rootDir, uri + file);
						break;
					}
					
				}
				
				if (!found) {
					if (f.canRead()) {
						// No index file, list the directory if it is readable
						res = new Response(listDirectory(uri, f));
					} else {
						res = new Response(Response.Status.FORBIDDEN, NanoHTTPD.MIME_PLAINTEXT, "FORBIDDEN: No directory listing.");
					}
				}
				
			}
		}
		
		try {
			if (res == null) {
				// Get MIME type from file name extension, if possible
				String mime = null;
				final int dot = f.getCanonicalPath().lastIndexOf('.');
				if (dot >= 0) {
					mime = this.mimiTypes.get(f.getCanonicalPath().substring(dot + 1).toLowerCase());
				}
				
				if (mime == null) {
					mime = NanoHTTPD.MIME_DEFAULT_BINARY;
				} else if (mime.equalsIgnoreCase("application/sl")) {
					
					final SleepCodeHandler p = new SleepCodeHandler(f, NanoHTTPD.MIME_HTML, parms, header, method.toString(), loader);
					
					final Response r = new Response(Response.Status.OK, p.getMimeType(), p.parseSleep());
					
					for (final Map.Entry<String, String> e : p.getHeaders().entrySet()) {
						r.addHeader(e.getKey(), e.getValue());
					}
					
					return r;
				}
				
				// Calculate etag
				final String etag = Integer.toHexString((f.getAbsolutePath() + f.lastModified() + "" + f.length()).hashCode());
				
				// Support (simple) skipping:
				long startFrom = 0;
				long endAt = -1;
				String range = header.get("range");
				if (range != null) {
					if (range.startsWith("bytes=")) {
						range = range.substring("bytes=".length());
						final int minus = range.indexOf('-');
						try {
							if (minus > 0) {
								startFrom = Long.parseLong(range.substring(0, minus));
								endAt = Long.parseLong(range.substring(minus + 1));
							}
						} catch (final NumberFormatException ignored) {
						}
					}
				}
				
				// Change return code and add Content-Range header when skipping is requested
				final long fileLen = f.length();
				
				if (range != null && startFrom >= 0) {
					if (startFrom >= fileLen) {
						res = new Response(Response.Status.RANGE_NOT_SATISFIABLE, NanoHTTPD.MIME_PLAINTEXT, "");
						res.addHeader("Content-Range", "bytes 0-0/" + fileLen);
						res.addHeader("ETag", etag);
					} else {
						if (endAt < 0) {
							endAt = fileLen - 1;
						}
						long newLen = endAt - startFrom + 1;
						if (newLen < 0) {
							newLen = 0;
						}
						
						final long dataLen = newLen;
						final FileInputStream fis = new FileInputStream(f) {
							
							@Override
							public int available() throws IOException {
							
								return (int) dataLen;
							}
						};
						fis.skip(startFrom);
						
						res = new Response(Response.Status.PARTIAL_CONTENT, mime, fis);
						res.addHeader("Content-Length", "" + dataLen);
						res.addHeader("Content-Range", "bytes " + startFrom + "-" + endAt + "/" + fileLen);
						res.addHeader("ETag", etag);
					}
				} else {
					if (etag.equals(header.get("if-none-match"))) {
						res = new Response(Response.Status.NOT_MODIFIED, mime, "");
					} else {
						res = new Response(Response.Status.OK, mime, new FileInputStream(f));
						res.addHeader("Content-Length", "" + fileLen);
						res.addHeader("ETag", etag);
					}
				}
			}
		} catch (final IOException ioe) {
			res = new Response(Response.Status.FORBIDDEN, NanoHTTPD.MIME_PLAINTEXT, "FORBIDDEN: Reading file failed.");
		}
		
		res.addHeader("Accept-Ranges", "bytes"); // Announce that the file server accepts partial content requestes
		//return res;
*/		
		// Make sure we won't die of an exception later
		if (!rootDir.isDirectory()) {
			res = new Response(Response.Status.INTERNAL_ERROR, NanoHTTPD.MIME_PLAINTEXT, "INTERNAL ERRROR: serveFile(): given homeDir is not a directory.");
		}
		
		if (res == null) {
			// Remove URL arguments
			uri = uri.trim().replace(File.separatorChar, '/');
			if (uri.indexOf('?') >= 0)
				uri = uri.substring(0, uri.indexOf('?'));
			
			// Prohibit getting out of current directory
			if (uri.startsWith("src/main") || uri.endsWith("src/main") || uri.contains("../"))
				res = new Response(Response.Status.FORBIDDEN, NanoHTTPD.MIME_PLAINTEXT, "FORBIDDEN: Won't serve ../ for security reasons.");
		}
		
		File f = new File(rootDir, uri);
		if (res == null && !f.exists()) {
			res = new Response(Response.Status.NOT_FOUND, NanoHTTPD.MIME_PLAINTEXT, "Error 404, file not found.");
		}
		
		// List the directory, if necessary
		if (res == null && f.isDirectory()) {
			// Browsers get confused without '/' after the
			// directory, send a redirect.
			if (!uri.endsWith("/")) {
				uri += "/";
				res = new Response(Response.Status.REDIRECT, NanoHTTPD.MIME_HTML, "<html><body>Redirected: <a href=\"" + uri + "\">" + uri + "</a></body></html>");
				res.addHeader("Location", uri);
			}
			
			if (res == null) {
				boolean found = false;
				for (String file : indexFiles) {
					
					if (new File(f, file).exists()) {
						found = true;
						f = new File(rootDir, uri + file);
						break;
					}
					
				}
				
				if (!found) {
					if (f.canRead()) {
						// No index file, list the directory if it is readable
						res = new Response(listDirectory(uri, f));
					} else {
						res = new Response(Response.Status.FORBIDDEN, NanoHTTPD.MIME_PLAINTEXT, "FORBIDDEN: No directory listing.");
					}
				}
			}
		}
		
		try {
			if (res == null) {
				// Get MIME type from file name extension, if possible
				String mime = null;
				int dot = f.getCanonicalPath().lastIndexOf('.');
				if (dot >= 0) {
					mime = mimiTypes.get(f.getCanonicalPath().substring(dot + 1).toLowerCase());
				}
				if (mime == null) {
					mime = NanoHTTPD.MIME_DEFAULT_BINARY;
				} else if (mime.equalsIgnoreCase("application/sl")) {
					
					final SleepCodeHandler p = new SleepCodeHandler(f, NanoHTTPD.MIME_HTML, parms, header, method.toString(), loader);
					
					final Response r = new Response(Response.Status.OK, p.getMimeType(), p.parseSleep());
					
					for (final Map.Entry<String, String> e : p.getHeaders().entrySet()) {
						r.addHeader(e.getKey(), e.getValue());
					}
					
					return r;
				}
				
				// Calculate etag
				String etag = Integer.toHexString((f.getAbsolutePath() + f.lastModified() + "" + f.length()).hashCode());
				
				// Support (simple) skipping:
				long startFrom = 0;
				long endAt = -1;
				String range = header.get("range");
				if (range != null) {
					if (range.startsWith("bytes=")) {
						range = range.substring("bytes=".length());
						int minus = range.indexOf('-');
						try {
							if (minus > 0) {
								startFrom = Long.parseLong(range.substring(0, minus));
								endAt = Long.parseLong(range.substring(minus + 1));
							}
						} catch (NumberFormatException ignored) {
						}
					}
				}
				
				// Change return code and add Content-Range header when skipping is requested
				long fileLen = f.length();
				if (range != null && startFrom >= 0) {
					if (startFrom >= fileLen) {
						res = new Response(Response.Status.RANGE_NOT_SATISFIABLE, NanoHTTPD.MIME_PLAINTEXT, "");
						res.addHeader("Content-Range", "bytes 0-0/" + fileLen);
						res.addHeader("ETag", etag);
					} else {
						if (endAt < 0) {
							endAt = fileLen - 1;
						}
						long newLen = endAt - startFrom + 1;
						if (newLen < 0) {
							newLen = 0;
						}
						
						final long dataLen = newLen;
						FileInputStream fis = new FileInputStream(f) {
							
							@Override
							public int available() throws IOException {
							
								return (int) dataLen;
							}
						};
						fis.skip(startFrom);
						
						res = new Response(Response.Status.PARTIAL_CONTENT, mime, fis);
						res.addHeader("Content-Length", "" + dataLen);
						res.addHeader("Content-Range", "bytes " + startFrom + "-" + endAt + "/" + fileLen);
						res.addHeader("ETag", etag);
					}
				} else {
					if (etag.equals(header.get("if-none-match")))
						res = new Response(Response.Status.NOT_MODIFIED, mime, "");
					else {
						res = new Response(Response.Status.OK, mime, new FileInputStream(f));
						res.addHeader("Content-Length", "" + fileLen);
						res.addHeader("ETag", etag);
					}
				}
			}
		} catch (IOException ioe) {
			res = new Response(Response.Status.FORBIDDEN, NanoHTTPD.MIME_PLAINTEXT, "FORBIDDEN: Reading file failed.");
		}
		
		res.addHeader("Accept-Ranges", "bytes"); // Announce that the file server accepts partial content requestes
		return res;
		
	}
	
}
