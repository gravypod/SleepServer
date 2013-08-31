package fi.iki.elonen;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;

public class ByteBufferInputStream extends InputStream {
	
	private final ByteBuffer buf;
	
	public ByteBufferInputStream(final ByteBuffer buf) {
	
		this.buf = buf;
	}
	
	@Override
	public synchronized int read() throws IOException {
	
		if (!buf.hasRemaining()) {
			return -1;
		}
		return buf.get() & 0xFF;
	}
	
	@Override
	public int available() throws IOException {
	
		return buf.remaining();
	}
	
	@Override
	public synchronized int read(final byte[] bytes, final int off, int len) throws IOException {
	
		if (!buf.hasRemaining()) {
			return -1;
		}
		
		len = Math.min(len, buf.remaining());
		buf.get(bytes, off, len);
		return len;
	}
}
