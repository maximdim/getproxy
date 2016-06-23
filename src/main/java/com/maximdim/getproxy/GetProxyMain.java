package com.maximdim.getproxy;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.AbstractHandler;

public class GetProxyMain {
	
	public static void main(String[] args) throws Exception {
		if (args.length != 1) {
			System.err.println("Usage: "+GetProxyMain.class.getSimpleName()+" <targetUrl>");
			System.exit(1);
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
			HttpGet get = new HttpGet(this.base+target);
			
			Collections.list(request.getHeaderNames())
				.stream()
				.forEach(name -> {
					get.addHeader(name, request.getHeader(name));
				});
			
			try(CloseableHttpResponse resp = httpclient.execute(get)) {
				Arrays.stream(resp.getAllHeaders())
					.forEach(h -> {
						response.setHeader(h.getName(), h.getValue());
					});
				HttpEntity entity = resp.getEntity();
				entity.writeTo(response.getOutputStream());
			}
		}
	}
}


