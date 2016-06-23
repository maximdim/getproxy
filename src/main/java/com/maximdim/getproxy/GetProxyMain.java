package com.maximdim.getproxy;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Collections;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.AbstractHandler;

public class GetProxyMain {
	static final File DIR = new File(System.getProperty("user.home"), ".getproxy");
	
	public static void main(String[] args) throws Exception {
		if (args.length != 1) {
			System.err.println("Usage: "+GetProxyMain.class.getSimpleName()+" <targetUrl>");
			System.exit(1);
		}
		
		if (!DIR.exists()) {
			DIR.mkdir();
			System.out.println(DIR.getAbsolutePath()+" created");
		}
		
        Server server = new Server(9999);
        server.setHandler(new Handler(args[0]));
        server.start();
        server.dumpStdErr();
        server.join();		
	}
	
	public static class Handler extends AbstractHandler {
		private final String base;
		CloseableHttpClient httpclient = HttpClients.createDefault();
		
		public Handler(String targetUrl) {
			this.base = targetUrl;
		}

		@Override
		public void handle(String target, Request baseRequest, HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
			if (!"GET".equalsIgnoreCase(request.getMethod())) {
				response.sendError(400, "Only GET requests supported");
				return;
			}
			System.out.println(target);
			
			File file = new File(DIR, DigestUtils.sha1Hex(target));
			if (readFromFile(file, response)) {
				System.out.println("Served cached version: "+file.getAbsolutePath());
				return;
			}
			
			HttpGet get = new HttpGet(this.base+target);
			
			Collections.list(request.getHeaderNames())
				.stream()
				.forEach(name -> {
					get.addHeader(name, request.getHeader(name));
				});
			
			try(CloseableHttpResponse resp = httpclient.execute(get)) {
				writeToFile(file, resp);
			}
			System.out.println("Saved to cache: "+file.getAbsolutePath());
			readFromFile(file, response);
		}
	}

	static void writeToFile(File f, CloseableHttpResponse resp) throws IOException {
		try(BufferedOutputStream os = new BufferedOutputStream(new FileOutputStream(f))) {
			copy(resp.getEntity().getContent(), os);
			os.flush();
		}
	}
	
	static boolean readFromFile(File f, HttpServletResponse response) throws IOException {
		if (!f.exists()) {
			return false;
		}
		try(BufferedInputStream is = new BufferedInputStream(new FileInputStream(f))) {
			copy(is, response.getOutputStream());
		}
		return true;
	}
	
	static int copy(InputStream in, OutputStream out) throws IOException {
		int byteCount = 0;
		byte[] buffer = new byte[4096];
		int bytesRead = -1;
		while ((bytesRead = in.read(buffer)) != -1) {
			out.write(buffer, 0, bytesRead);
			byteCount += bytesRead;
		}
		out.flush();
		return byteCount;
	}
	
	
}


