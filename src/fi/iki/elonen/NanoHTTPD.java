package fi.iki.elonen;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.RandomAccessFile;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Scanner;
import java.util.StringTokenizer;
import java.util.TimeZone;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import sleep.runtime.ScriptLoader;

/**
 * A simple, tiny, nicely embeddable HTTP server in Java
 * <p/>
 * <p/>
 * NanoHTTPD
 * <p>
 * </p>
 * Copyright (c) 2012-2013 by Paul S. Hawke, 2001,2005-2013 by Jarno Elonen,
 * 2010 by Konstantinos Togias</p>
 * <p/>
 * <p/>
 * <b>Features + limitations: </b>
 * <ul>
 * <p/>
 * <li>Only one Java file</li>
 * <li>Java 5 compatible</li>
 * <li>Released as open source, Modified BSD licence</li>
 * <li>No fixed config files, logging, authorization etc. (Implement yourself if
 * you need them.)</li>
 * <li>Supports parameter parsing of GET and POST methods (+ rudimentary PUT
 * support in 1.25)</li>
 * <li>Supports both dynamic content and file serving</li>
 * <li>Supports file upload (since version 1.2, 2010)</li>
 * <li>Supports partial content (streaming)</li>
 * <li>Supports ETags</li>
 * <li>Never caches anything</li>
 * <li>Doesn't limit bandwidth, request time or simultaneous connections</li>
 * <li>Default code serves files and shows all HTTP parameters and headers</li>
 * <li>File server supports directory listing, index.html and index.htm</li>
 * <li>File server supports partial content (streaming)</li>
 * <li>File server supports ETags</li>
 * <li>File server does the 301 redirection trick for directories without '/'</li>
 * <li>File server supports simple skipping for files (continue download)</li>
 * <li>File server serves also very long files without memory overhead</li>
 * <li>Contains a built-in list of most common mime types</li>
 * <li>All header names are converted lowercase so they don't vary between
 * browsers/clients</li>
 * <p/>
 * </ul>
 * <p/>
 * <p/>
 * <b>How to use: </b>
 * <ul>
 * <p/>
 * <li>Subclass and implement serve() and embed to your own program</li>
 * <p/>
 * </ul>
 * <p/>
 * See the separate "LICENSE.md" file for the distribution license (Modified BSD
 * licence) edited by gravypod
 */
public abstract class NanoHTTPD {
	
	/**
	 * Common mime type for dynamic content: plain text
	 */
	public static final String MIME_PLAINTEXT = "text/plain";
	
	/**
	 * Common mime type for dynamic content: html
	 */
	public static final String MIME_HTML = "text/html";
	
	/**
	 * Common mime type for dynamic content: binary
	 */
	public static final String MIME_DEFAULT_BINARY = "application/octet-stream";
	
	private int[] myPort;
	
	/**
	 * Pseudo-Parameter to use to store the actual query string in the
	 * parameters map for later re-processing.
	 */
	private static final String QUERY_STRING_PARAMETER = "NanoHttpd.QUERY_STRING";
	
	private static AtomicReference<SimpleDateFormat> simpleDateFormat = new AtomicReference<SimpleDateFormat>();
	
	public NanoHTTPD(final int port) {
	
		myPort = new int[] { port };
		final SimpleDateFormat gmtFrmt = new SimpleDateFormat("E, d MMM yyyy HH:mm:ss 'GMT'", Locale.US);
		gmtFrmt.setTimeZone(TimeZone.getTimeZone("GMT"));
		simpleDateFormat.set(gmtFrmt);
		
	}
	
	public NanoHTTPD(int[] ports) {
	
		myPort = ports;
		final SimpleDateFormat gmtFrmt = new SimpleDateFormat("E, d MMM yyyy HH:mm:ss 'GMT'", Locale.US);
		gmtFrmt.setTimeZone(TimeZone.getTimeZone("GMT"));
		simpleDateFormat.set(gmtFrmt);
	}
	
	/**
	 * Start the server.
	 * 
	 * @throws IOException
	 *             if the socket is in use.
	 */
	public void run() {
		List<Server> servers = new ArrayList<NanoHTTPD.Server>();
		try {
			
			final TimeUnit timeUnit = TimeUnit.MICROSECONDS;
			final ArrayBlockingQueue<Runnable> threadQueue = new ArrayBlockingQueue<Runnable>(100);
			
			ThreadPoolExecutor pool = new ThreadPoolExecutor(1, 100, 10, timeUnit, threadQueue);
			
			
			for (int port : myPort) {
				ServerSocketChannel myServerSocket = ServerSocketChannel.open();
				
				myServerSocket.configureBlocking(false);
				
				myServerSocket.bind(new InetSocketAddress(port));
				Server currentServer = new Server(myServerSocket, pool);
				servers.add(currentServer);
				currentServer.start();
				
			}
			
		} catch (IOException e1) {
			e1.printStackTrace();
		}
		
		Scanner sc = new Scanner(System.in);
		
		while(sc.hasNext()) {
			String next = sc.nextLine();
			switch(next) {
				case "kill":
				case "shutdown":
				case "stop":
					System.exit(0);
					break;
			}
		}
		
	}
	
	// ------------------------------------------------------------------------------- //
	//
	// Temp file handling strategy.
	//
	// ------------------------------------------------------------------------------- //
	
	/**
	 * Factory to create temp file managers.
	 */
	public interface TempFileManagerFactory {
		
		TempFileManager create();
	}
	
	/**
	 * Temp file manager.
	 * 
	 * <p>
	 * Temp file managers are created 1-to-1 with incoming requests, to create
	 * and cleanup temporary files created as a result of handling the request.
	 * </p>
	 */
	public interface TempFileManager {
		
		TempFile createTempFile() throws Exception;
		
		void clear();
	}
	
	/**
	 * A temp file.
	 * 
	 * <p>
	 * Temp files are responsible for managing the actual temporary storage and
	 * cleaning themselves up when no longer needed.
	 * </p>
	 */
	public interface TempFile {
		
		File getFile();
		
		FileChannel open() throws Exception;
		
		void delete() throws Exception;
		
		String getName();
		
		RandomAccessFile getRandomAccess();
	}
	
	/**
	 * Default strategy for creating and cleaning up temporary files.
	 */
	private class DefaultTempFileManagerFactory implements TempFileManagerFactory {
		
		@Override
		public TempFileManager create() {
		
			return new DefaultTempFileManager();
		}
	}
	
	/**
	 * Default strategy for creating and cleaning up temporary files.
	 * 
	 * <p>
	 * </p>
	 * This class stores its files in the standard location (that is, wherever
	 * <code>java.io.tmpdir</code> points to). Files are added to an internal
	 * list, and deleted when no longer needed (that is, when
	 * <code>clear()</code> is invoked at the end of processing a request).</p>
	 */
	public static class DefaultTempFileManager implements TempFileManager {
		
		private final String tmpdir;
		
		private final List<TempFile> tempFiles;
		
		public DefaultTempFileManager() {
		
			tmpdir = System.getProperty("java.io.tmpdir");
			tempFiles = new ArrayList<TempFile>();
		}
		
		@Override
		public TempFile createTempFile() throws Exception {
		
			final DefaultTempFile tempFile = new DefaultTempFile(tmpdir);
			tempFiles.add(tempFile);
			return tempFile;
		}
		
		@Override
		public void clear() {
		
			for (final TempFile file : tempFiles) {
				try {
					file.delete();
				} catch (final Exception ignored) {
				}
			}
			tempFiles.clear();
		}
	}
	
	/**
	 * Default strategy for creating and cleaning up temporary files.
	 * 
	 * <p>
	 * </p>
	 * </[>By default, files are created by <code>File.createTempFile()</code>
	 * in the directory specified.</p>
	 */
	public static class DefaultTempFile implements TempFile {
		
		private final File file;
		
		private final RandomAccessFile randomAccessFile;
		
		public DefaultTempFile(final String tempdir) throws IOException {
		
			file = File.createTempFile("NanoHTTPD-", "", new File(tempdir));
			randomAccessFile = new RandomAccessFile(file, "rw");
		}
		
		@Override
		public File getFile() {
		
			return file;
		}
		
		@Override
		public FileChannel open() throws Exception {
		
			return randomAccessFile.getChannel();
		}
		
		@Override
		public void delete() throws Exception {
		
			file.delete();
		}
		
		@Override
		public String getName() {
		
			return file.getAbsolutePath();
		}
		
		@Override
		public RandomAccessFile getRandomAccess() {
		
			return randomAccessFile;
		}
	}
	
	/**
	 * Override this to customize the server.
	 * <p/>
	 * <p/>
	 * (By default, this delegates to serveFile() and allows directory listing.)
	 * 
	 * @param uri
	 *            Percent-decoded URI without parameters, for example
	 *            "/index.cgi"
	 * @param method
	 *            "GET", "POST" etc.
	 * @param parms
	 *            Parsed, percent decoded parameters from URI and, in case of
	 *            POST, data.
	 * @param header
	 *            Header entries, percent decoded
	 * @param loader
	 * @return HTTP response, see class Response for details
	 */
	public abstract Response serve(final String uri, final Method method, final Map<String, String> header, final Map<String, String> parms, final Map<String, String> files, final ScriptLoader loader);
	
	/**
	 * Decode percent encoded <code>String</code> values.
	 * 
	 * @param str
	 *            the percent encoded <code>String</code>
	 * @return expanded form of the input, for example "foo%20bar" becomes
	 *         "foo bar"
	 */
	protected String decodePercent(final String str) {
	
		final StringBuilder sb = new StringBuilder();
		for (int i = 0; i < str.length(); i++) {
			final char c = str.charAt(i);
			switch(c) {
				case '+':
					sb.append(' ');
					break;
				case '%':
					sb.append((char) Integer.parseInt(str.substring(i + 1, i + 3), 16));
					i += 2;
					break;
				default:
					sb.append(c);
					break;
			}
		}
		return sb.toString().trim();
	}
	
	/**
	 * Decode parameters from a URL, handing the case where a single parameter
	 * name might have been supplied several times, by return lists of values.
	 * In general these lists will contain a single element.
	 * 
	 * @param parms
	 *            original <b>NanoHttpd</b> parameters values, as passed to the
	 *            <code>serve()</code> method.
	 * @return a map of <code>String</code> (parameter name) to
	 *         <code>List&lt;String&gt;</code> (a list of the values supplied).
	 */
	protected Map<String, List<String>> decodeParameters(final Map<String, String> parms) {
	
		return this.decodeParameters(parms.get(NanoHTTPD.QUERY_STRING_PARAMETER));
	}
	
	/**
	 * Decode parameters from a URL, handing the case where a single parameter
	 * name might have been supplied several times, by return lists of values.
	 * In general these lists will contain a single element.
	 * 
	 * @param queryString
	 *            a query string pulled from the URL.
	 * @return a map of <code>String</code> (parameter name) to
	 *         <code>List&lt;String&gt;</code> (a list of the values supplied).
	 */
	protected Map<String, List<String>> decodeParameters(final String queryString) {
	
		final Map<String, List<String>> parms = new HashMap<String, List<String>>();
		if (queryString != null) {
			final StringTokenizer st = new StringTokenizer(queryString, "&");
			while(st.hasMoreTokens()) {
				final String e = st.nextToken();
				final int sep = e.indexOf('=');
				final String propertyName = sep >= 0 ? decodePercent(e.substring(0, sep)) : decodePercent(e);
				if (!parms.containsKey(propertyName)) {
					parms.put(propertyName, new ArrayList<String>());
				}
				final String propertyValue = sep >= 0 ? decodePercent(e.substring(sep + 1)) : null;
				if (propertyValue != null) {
					parms.get(propertyName).add(propertyValue);
				}
			}
		}
		return parms;
	}
	
	/**
	 * HTTP Request methods, with the ability to decode a <code>String</code>
	 * back to its enum value.
	 */
	public enum Method {
		GET, PUT, POST, DELETE, HEAD;
		
		static Method lookup(final String method) {
		
			return Method.valueOf(method.toUpperCase(Locale.ENGLISH));
		}
	}
	
	/**
	 * HTTP response. Return one of these from serve().
	 */
	public static class Response {
		
		/**
		 * HTTP status code after processing, e.g. "200 OK", HTTP_OK
		 */
		private Status status;
		
		/**
		 * MIME type of content, e.g. "text/html"
		 */
		private String mimeType;
		
		/**
		 * Data of the response, may be null.
		 */
		private InputStream data;
		
		/**
		 * Headers for the HTTP response. Use addHeader() to add lines.
		 */
		private final Map<String, String> header = new HashMap<String, String>();
		
		/**
		 * The request method that spawned this response.
		 */
		private Method requestMethod;
		
		/**
		 * Default constructor: response = HTTP_OK, mime = MIME_HTML and your
		 * supplied message
		 */
		public Response(final String msg) {
		
			this(Status.OK, NanoHTTPD.MIME_HTML, msg);
		}
		
		/**
		 * Basic constructor.
		 */
		public Response(final Status status, final String mimeType, final InputStream data) {
		
			this.status = status;
			this.mimeType = mimeType;
			this.data = data;
		}
		
		/**
		 * Convenience method that makes an InputStream out of given text.
		 */
		public Response(final Status status, final String mimeType, final String txt) {
		
			this.status = status;
			this.mimeType = mimeType;
			try {
				data = txt != null ? new ByteArrayInputStream(txt.getBytes("UTF-8")) : null;
			} catch (final java.io.UnsupportedEncodingException uee) {
				uee.printStackTrace();
			}
		}
		
		public static void error(final SocketChannel channel, final Status error, final String message) {
		
			new Response(error, NanoHTTPD.MIME_PLAINTEXT, message).send(channel);
		}
		
		/**
		 * Adds given line to the header.
		 */
		public void addHeader(final String name, final String value) {
		
			header.put(name, value);
		}
		
		/**
		 * Sends given response to the socket.
		 */
		private void send(final SocketChannel channel) {
		
			final String mime = mimeType;
			
			try {
				if (status == null) {
					throw new Error("sendResponse(): Status can't be null.");
				}
				final StringBuilder headerString = new StringBuilder("HTTP/1.0 " + status.getDescription() + " \r\n");
				
				if (mime != null) {
					headerString.append("Content-Type: " + mime + "\r\n");
				}
				
				if (header == null || header.get("Date") == null) {
					headerString.append("Date: " + simpleDateFormat.get().format(new Date()) + "\r\n");
				}
				
				if (header != null) {
					for (final String key : header.keySet()) {
						final String value = header.get(key);
						headerString.append(key + ": " + value + "\r\n");
					}
				}
				
				headerString.append("\r\n");
				{
					final ByteBuffer data = ByteBuffer.wrap(headerString.toString().getBytes());
					data.flip();
					channel.write(data);
				}
				if (requestMethod != Method.HEAD && data != null) {
					int pending = data.available(); // This is to support partial sends, see serveFile()
					final int BUFFER_SIZE = 16 * 1024;
					final byte[] buff = new byte[BUFFER_SIZE];
					while(pending > 0) {
						final int read = data.read(buff, 0, pending > BUFFER_SIZE ? BUFFER_SIZE : pending);
						if (read <= 0) {
							break;
						}
						
						final ByteBuffer writeData = ByteBuffer.wrap(buff, 0, pending > BUFFER_SIZE ? BUFFER_SIZE : pending);
						channel.write(writeData);
						
						pending -= read;
					}
				}
				channel.close();
				if (data != null) {
					data.close();
				}
			} catch (final IOException ioe) {
				// Couldn't write? No can do.
			}
		}
		
		public Status getStatus() {
		
			return status;
		}
		
		public void setStatus(final Status status) {
		
			this.status = status;
		}
		
		public String getMimeType() {
		
			return mimeType;
		}
		
		public void setMimeType(final String mimeType) {
		
			this.mimeType = mimeType;
		}
		
		public InputStream getData() {
		
			return data;
		}
		
		public void setData(final InputStream data) {
		
			this.data = data;
		}
		
		public Method getRequestMethod() {
		
			return requestMethod;
		}
		
		public void setRequestMethod(final Method requestMethod) {
		
			this.requestMethod = requestMethod;
		}
		
		/**
		 * Some HTTP response status codes
		 */
		public enum Status {
			OK(200, "OK"), CREATED(201, "Created"), ACCEPTED(202, "Accepted"), NO_CONTENT(204, "No Content"), PARTIAL_CONTENT(206, "Partial Content"), REDIRECT(301, "Moved Permanently"), NOT_MODIFIED(304, "Not Modified"), BAD_REQUEST(400, "Bad Request"), UNAUTHORIZED(401, "Unauthorized"), FORBIDDEN(403, "Forbidden"), NOT_FOUND(404, "Not Found"), RANGE_NOT_SATISFIABLE(416, "Requested Range Not Satisfiable"), INTERNAL_ERROR(500, "Internal Server Error");
			
			private final int requestStatus;
			
			private final String description;
			
			Status(final int requestStatus, final String description) {
			
				this.requestStatus = requestStatus;
				this.description = description;
			}
			
			public int getRequestStatus() {
			
				return requestStatus;
			}
			
			public String getDescription() {
			
				return requestStatus + " " + description;
			}
		}
	}
	
	/**
	 * Handles one session, i.e. parses the HTTP request and returns the
	 * response.
	 */
	protected class HTTPSession implements Runnable {
		
		public static final int BUFSIZE = 8192;
		
		private final SocketChannel channel;
		
		private final TempFileManager manager;
		
		private final ScriptLoader loader;
		
		/**
		 * Create a new HTTPSession lined to the given {@link SocketChannel} and
		 * using the specified {@link TempfileSystem}
		 * 
		 * @param channel
		 *            - SocketChannel to listen to
		 * @param loader
		 * @param tempFileSystem
		 *            - The {@link TempfileSystem}
		 * @param sessionI
		 */
		public HTTPSession(final SocketChannel channel, final TempFileManager manager, final ScriptLoader loader) {
		
			this.channel = channel;
			this.manager = manager;
			this.loader = loader;
		}
		
		@Override
		public void run() {
		
			try {
				
				// Read the first 8192 bytes.
				// The full header should fit in here.
				// Apache's default header limit is 8KB.
				// Do NOT assume that a single read will get the entire header at once!
				ByteBuffer buffer = ByteBuffer.allocate(HTTPSession.BUFSIZE);
				
				int splitbyte = 0;
				
				int rlen = 0;
				
				{
					int read = channel.read(buffer);
					
					while(read != -1 || read != 0) {
						rlen += read;
						splitbyte = findHeaderEnd(buffer, rlen);
						if (splitbyte > 0) {
							break;
						}
						read = channel.read(buffer);
						
					}
				}
				
				buffer.flip();
				
				// Create a BufferedReader for parsing the header.
				final BufferedReader hin = new BufferedReader(new InputStreamReader(new ByteBufferInputStream(buffer)));
				final Map<String, String> pre = new HashMap<String, String>();
				final Map<String, String> parms = new HashMap<String, String>();
				final Map<String, String> header = new HashMap<String, String>();
				final Map<String, String> files = new HashMap<String, String>();
				
				// Decode the header into parms and header java properties
				decodeHeader(hin, pre, parms, header);
				final Method method = Method.lookup(pre.get("method"));
				if (method == null) {
					Response.error(channel, Response.Status.BAD_REQUEST, "BAD REQUEST: Syntax error.");
					throw new InterruptedException();
				}
				final String uri = pre.get("uri");
				long size = extractContentLength(header);
				
				// Write the part of body already read to ByteArrayOutputStream f
				final FileChannel f = getTmpBucket(manager).getChannel();
				
				// While Firefox sends on the first read all the data fitting
				// our buffer, Chrome and Opera send only the headers even if
				// there is data for the body. We do some magic here to find
				// out whether we have already consumed part of body, if we
				// have reached the end of the data to be sent or we should
				// expect the first byte of the body at the next read.
				if (splitbyte < rlen) {
					size -= rlen - splitbyte + 1;
					
					f.write(buffer);
				} else if (splitbyte == 0 || size == 0x7FFFFFFFFFFFFFFFl) {
					size = 0;
				}
				
				// Now read all the body and write it to f
				buffer = ByteBuffer.allocate(512);
				while(rlen >= 0 && size > 0) {
					rlen = channel.read(buffer);
					size -= rlen;
					buffer.flip();
					if (rlen > 0) {
						f.write(buffer, rlen);
					}
				}
				
				// Get the raw body as a byte []
				buffer = f.map(FileChannel.MapMode.READ_ONLY, 0, f.size());
				f.position(0);
				
				// Create a BufferedReader for easily reading it as string.
				final BufferedReader in = new BufferedReader(Channels.newReader(f, "ISO-8859-1"));
				
				// If the method is POST, there may be parameters
				// in data section, too, read it:
				
				switch(method) {
					case POST: {
						
						String contentType = "";
						final String contentTypeHeader = header.get("content-type");
						
						StringTokenizer st = null;
						if (contentTypeHeader != null) {
							st = new StringTokenizer(contentTypeHeader, ",; ");
							if (st.hasMoreTokens()) {
								contentType = st.nextToken();
							}
						}
						
						if ("multipart/form-data".equalsIgnoreCase(contentType)) {
							// Handle multipart/form-data
							if (!st.hasMoreTokens()) {
								Response.error(channel, Response.Status.BAD_REQUEST, "BAD REQUEST: Content type is multipart/form-data but boundary missing. Usage: GET /example/file.html");
								throw new InterruptedException();
							}
							
							final String boundaryStartString = "boundary=";
							final int boundaryContentStart = contentTypeHeader.indexOf(boundaryStartString) + boundaryStartString.length();
							String boundary = contentTypeHeader.substring(boundaryContentStart, contentTypeHeader.length());
							if (boundary.startsWith("\"") && boundary.startsWith("\"")) {
								boundary = boundary.substring(1, boundary.length() - 1);
							}
							
							decodeMultipartData(boundary, buffer, in, parms, files);
						} else {
							// Handle application/x-www-form-urlencoded
							String postLine = "";
							final char[] pbuf = new char[512];
							int read = in.read(pbuf);
							while(read >= 0 && !postLine.endsWith("\r\n")) {
								postLine += String.valueOf(pbuf, 0, read);
								read = in.read(pbuf);
							}
							postLine = postLine.trim();
							decodeParms(postLine, parms);
						}
					}
						break;
					case PUT: {
						files.put("content", saveTmpFile(buffer, 0, buffer.limit()));
					}
						break;
				}
				
				// Ok, now do the serve()
				final Response r = serve(uri, method, header, parms, files, loader);
				if (r == null) {
					Response.error(channel, Response.Status.INTERNAL_ERROR, "SERVER INTERNAL ERROR: Serve() returned a null response.");
					throw new InterruptedException();
				} else {
					r.setRequestMethod(method);
					r.send(channel);
				}
				
				in.close();
				channel.close();
			} catch (final IOException ioe) {
				try {
					Response.error(channel, Response.Status.INTERNAL_ERROR, "SERVER INTERNAL ERROR: IOException: " + ioe.getMessage());
					throw new InterruptedException();
				} catch (final Throwable ignored) {
				}
			} catch (final InterruptedException ie) {
				// Thrown by sendError, ignore and exit the thread.
			} finally {
				manager.clear(); // Remove all temp files
			}
		}
		
		private long extractContentLength(final Map<String, String> header) {
		
			long size = 0x7FFFFFFFFFFFFFFFl;
			final String contentLength = header.get("content-length");
			if (contentLength != null) {
				try {
					size = Integer.parseInt(contentLength);
				} catch (final NumberFormatException ex) {
					ex.printStackTrace();
				}
			}
			return size;
		}
		
		/**
		 * Decodes the sent headers and loads the data into Key/value pairs
		 */
		private void decodeHeader(final BufferedReader in, final Map<String, String> pre, final Map<String, String> parms, final Map<String, String> header) throws InterruptedException {
		
			try {
				// Read the request line
				final String inLine = in.readLine();
				if (inLine == null) {
					return;
				}
				
				final StringTokenizer st = new StringTokenizer(inLine);
				if (!st.hasMoreTokens()) {
					Response.error(channel, Response.Status.BAD_REQUEST, "BAD REQUEST: Syntax error. Usage: GET /example/file.html");
					throw new InterruptedException();
				}
				
				pre.put("method", st.nextToken());
				
				if (!st.hasMoreTokens()) {
					Response.error(channel, Response.Status.BAD_REQUEST, "BAD REQUEST: Missing URI. Usage: GET /example/file.html");
					throw new InterruptedException();
				}
				
				String uri = st.nextToken();
				
				// Decode parameters from the URI
				final int qmi = uri.indexOf('?');
				if (qmi >= 0) {
					decodeParms(uri.substring(qmi + 1), parms);
					uri = decodePercent(uri.substring(0, qmi));
				} else {
					uri = decodePercent(uri);
				}
				
				// If there's another token, it's protocol version,
				// followed by HTTP headers. Ignore version but parse headers.
				// NOTE: this now forces header names lowercase since they are
				// case insensitive and vary by client.
				if (st.hasMoreTokens()) {
					String line = in.readLine();
					while(line != null && line.trim().length() > 0) {
						final int p = line.indexOf(':');
						if (p >= 0) {
							header.put(line.substring(0, p).trim().toLowerCase(), line.substring(p + 1).trim());
						}
						line = in.readLine();
					}
				}
				
				pre.put("uri", uri);
			} catch (final IOException ioe) {
				Response.error(channel, Response.Status.INTERNAL_ERROR, "SERVER INTERNAL ERROR: IOException: " + ioe.getMessage());
				throw new InterruptedException();
			}
		}
		
		/**
		 * Decodes the Multipart Body data and put it into Key/Value pairs.
		 */
		private void decodeMultipartData(final String boundary, final ByteBuffer fbuf, final BufferedReader in, final Map<String, String> parms, final Map<String, String> files) throws InterruptedException {
		
			try {
				final int[] bpositions = getBoundaryPositions(fbuf, boundary.getBytes());
				int boundarycount = 1;
				String mpline = in.readLine();
				while(mpline != null) {
					if (!mpline.contains(boundary)) {
						Response.error(channel, Response.Status.BAD_REQUEST, "BAD REQUEST: Content type is multipart/form-data but next chunk does not start with boundary. Usage: GET /example/file.html");
						throw new InterruptedException();
					}
					boundarycount++;
					final Map<String, String> item = new HashMap<String, String>();
					mpline = in.readLine();
					while(mpline != null && mpline.trim().length() > 0) {
						final int p = mpline.indexOf(':');
						if (p != -1) {
							item.put(mpline.substring(0, p).trim().toLowerCase(), mpline.substring(p + 1).trim());
						}
						mpline = in.readLine();
					}
					if (mpline != null) {
						final String contentDisposition = item.get("content-disposition");
						if (contentDisposition == null) {
							Response.error(channel, Response.Status.BAD_REQUEST, "BAD REQUEST: Content type is multipart/form-data but no content-disposition info found. Usage: GET /example/file.html");
							throw new InterruptedException();
						}
						final StringTokenizer st = new StringTokenizer(contentDisposition, "; ");
						final Map<String, String> disposition = new HashMap<String, String>();
						while(st.hasMoreTokens()) {
							final String token = st.nextToken();
							final int p = token.indexOf('=');
							if (p != -1) {
								disposition.put(token.substring(0, p).trim().toLowerCase(), token.substring(p + 1).trim());
							}
						}
						String pname = disposition.get("name");
						pname = pname.substring(1, pname.length() - 1);
						
						String value = "";
						if (item.get("content-type") == null) {
							while(mpline != null && !mpline.contains(boundary)) {
								mpline = in.readLine();
								if (mpline != null) {
									final int d = mpline.indexOf(boundary);
									if (d == -1) {
										value += mpline;
									} else {
										value += mpline.substring(0, d - 2);
									}
								}
							}
						} else {
							if (boundarycount > bpositions.length) {
								Response.error(channel, Response.Status.INTERNAL_ERROR, "Error processing request");
								throw new InterruptedException();
							}
							final int offset = stripMultipartHeaders(fbuf, bpositions[boundarycount - 2]);
							final String path = saveTmpFile(fbuf, offset, bpositions[boundarycount - 1] - offset - 4);
							files.put(pname, path);
							value = disposition.get("filename");
							value = value.substring(1, value.length() - 1);
							do {
								mpline = in.readLine();
							} while(mpline != null && !mpline.contains(boundary));
						}
						parms.put(pname, value);
					}
				}
			} catch (final IOException ioe) {
				Response.error(channel, Response.Status.INTERNAL_ERROR, "SERVER INTERNAL ERROR: IOException: " + ioe.getMessage());
				throw new InterruptedException();
			}
		}
		
		/**
		 * Find byte index separating header from body. It must be the last byte
		 * of the first two sequential new lines.
		 * 
		 * @param rlen
		 * @param rlen
		 */
		private int findHeaderEnd(final ByteBuffer examin, final int rlen) {
		
			int splitbyte = 0;
			while(splitbyte + 3 < rlen) {
				if (examin.get(splitbyte) == '\r' && examin.get(splitbyte + 1) == '\n' && examin.get(splitbyte + 2) == '\r' && examin.get(splitbyte + 3) == '\n') {
					return splitbyte + 4;
				}
				splitbyte++;
			}
			return 0;
		}
		
		/**
		 * Find the byte positions where multipart boundaries start.
		 */
		public int[] getBoundaryPositions(final ByteBuffer b, final byte[] boundary) {
		
			int matchcount = 0;
			int matchbyte = -1;
			final List<Integer> matchbytes = new ArrayList<Integer>();
			for (int i = 0; i < b.limit(); i++) {
				if (b.get(i) == boundary[matchcount]) {
					if (matchcount == 0) {
						matchbyte = i;
					}
					matchcount++;
					if (matchcount == boundary.length) {
						matchbytes.add(matchbyte);
						matchcount = 0;
						matchbyte = -1;
					}
				} else {
					i -= matchcount;
					matchcount = 0;
					matchbyte = -1;
				}
			}
			final int[] ret = new int[matchbytes.size()];
			for (int i = 0; i < ret.length; i++) {
				ret[i] = matchbytes.get(i);
			}
			return ret;
		}
		
		/**
		 * Retrieves the content of a sent file and saves it to a temporary
		 * file. The full path to the saved file is returned.
		 */
		private String saveTmpFile(final ByteBuffer b, final int offset, final int len) {
		
			String path = "";
			if (len > 0) {
				try {
					final TempFile tempFile = manager.createTempFile();
					final ByteBuffer src = b.duplicate();
					final FileChannel dest = tempFile.open();
					src.position(offset).limit(offset + len);
					dest.write(src.slice());
					path = tempFile.getName();
				} catch (final Exception e) { // Catch exception if any
					System.err.println("Error: " + e.getMessage());
				}
			}
			return path;
		}
		
		private RandomAccessFile getTmpBucket(final TempFileManager fileGenerator) {
		
			try {
				final TempFile tempFile = fileGenerator.createTempFile();
				return tempFile.getRandomAccess();
			} catch (final Exception e) {
				System.err.println("Error: " + e.getMessage());
			}
			return null;
		}
		
		/**
		 * It returns the offset separating multipart file headers from the
		 * file's data.
		 */
		private int stripMultipartHeaders(final ByteBuffer b, final int offset) {
		
			int i;
			for (i = offset; i < b.limit(); i++) {
				if (b.get(i) == '\r' && b.get(++i) == '\n' && b.get(++i) == '\r' && b.get(++i) == '\n') {
					break;
				}
			}
			return i + 1;
		}
		
		/**
		 * Decodes parameters in percent-encoded URI-format ( e.g.
		 * "name=Jack%20Daniels&pass=Single%20Malt" ) and adds them to given
		 * Map. NOTE: this doesn't support multiple identical keys due to the
		 * simplicity of Map.
		 */
		private void decodeParms(final String parms, final Map<String, String> p) {
		
			if (parms == null) {
				p.put(NanoHTTPD.QUERY_STRING_PARAMETER, "");
				return;
			}
			
			p.put(NanoHTTPD.QUERY_STRING_PARAMETER, parms);
			final StringTokenizer st = new StringTokenizer(parms, "&");
			while(st.hasMoreTokens()) {
				final String e = st.nextToken();
				final int sep = e.indexOf('=');
				if (sep >= 0) {
					p.put(decodePercent(e.substring(0, sep)), decodePercent(e.substring(sep + 1)));
				} else {
					p.put(decodePercent(e), "");
				}
			}
		}
	}
	
	class SessionExecutor implements Runnable {
		
		final private SocketChannel finalAccept;
		
		final private TempFileManagerFactory tempFileManagerFactory;
		
		final private ScriptLoader loader;
		
		public SessionExecutor(SocketChannel finalAccept, TempFileManagerFactory tempFileManagerFactory, ScriptLoader loader) {
		
			this.finalAccept = finalAccept;
			this.tempFileManagerFactory = tempFileManagerFactory;
			this.loader = loader;
		}
		
		@Override
		public void run() {
		
			final TempFileManager tempFileManager = tempFileManagerFactory.create();
			
			final HTTPSession session = new HTTPSession(finalAccept, tempFileManager, loader);
			
			session.run();
			System.out.println("Ending " + System.identityHashCode(finalAccept));
			try {
				finalAccept.close();
			} catch (final IOException ignored) {
				ignored.printStackTrace();
			}
			
		}
		
	}
	
	class Server extends Thread {
		
		ServerSocketChannel channel;
		
		ThreadPoolExecutor pool;
		
		DefaultTempFileManagerFactory fileFactory;
		
		ScriptLoader scriptLoader = new ScriptLoader();
		
		public Server(ServerSocketChannel channel, ThreadPoolExecutor pool) {
		
			this.pool = pool;
			fileFactory = new DefaultTempFileManagerFactory();
			this.channel = channel;
			scriptLoader.setGlobalCache(true);
		}
		
		@Override
		public void run() {
		
			do {
				try {
					
					SocketChannel accept = channel.accept();
					
					if (accept == null) {
						Thread.sleep(1l);
						continue;
					} else {
						System.out.println("Starting " + System.identityHashCode(accept));
						pool.execute(new SessionExecutor(accept, fileFactory, scriptLoader));
					}
				} catch (Exception e) {
				}
				
			} while(true); // Create shutdonw
		}
		
	}
	
}
