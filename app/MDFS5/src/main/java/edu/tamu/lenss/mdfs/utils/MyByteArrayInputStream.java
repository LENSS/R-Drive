package edu.tamu.lenss.mdfs.utils;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.zip.GZIPInputStream;

public class MyByteArrayInputStream extends ByteArrayInputStream {

	public MyByteArrayInputStream(byte[] buf) {
		super(buf);
	}
	
	public void setArray(byte[] newBuf){
		this.buf = newBuf;
		this.pos = 0;
		this.count = buf.length;
	}

	public static class MyGZIPInputStream extends GZIPInputStream {

		public MyGZIPInputStream(InputStream is) throws IOException {
			super(is);
		}

		public void reset(byte[] newBuf) throws IOException {
			this.eos = false;
			this.crc.reset();
			this.buf = newBuf;
			this.len = newBuf.length;
			this.inf.reset();
		}
		
	}
}


