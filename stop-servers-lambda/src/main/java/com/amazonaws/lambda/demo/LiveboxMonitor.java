package com.amazonaws.lambda.demo;

import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.io.IOUtils;
import org.apache.http.HttpEntity;
import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.client.methods.RequestBuilder;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.conn.ssl.TrustAllStrategy;
import org.apache.http.cookie.Cookie;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.BasicCookieStore;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.ssl.SSLContextBuilder;
import org.apache.http.util.EntityUtils;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

public class LiveboxMonitor {

	private String url = "https://ikjuchniewicz.ddns.net:10000";
	private String username = System.getenv("USERNAME");
	private String password = System.getenv("PASSWORD");
	
	private CloseableHttpClient httpclient;
	private String contextId;
	
	private JsonNode dsl0Node = null;
	
	public LiveboxMonitor(String url, String username, String password) {
		super();
		this.url = url;
		this.username = username;
		this.password = password;
	}

	public void getDsl0Status() throws Exception {
		StringEntity requestEntity = new StringEntity(
				"{\"parameters\":{\"mibs\":\"dsl\",\"flag\":\"\",\"traverse\":\"down\"}}", 
				ContentType.APPLICATION_JSON);

		HttpPost postMethod = new HttpPost(url + "/sysbus/NeMo/Intf/data:getMIBs");
		postMethod.setEntity(requestEntity);
		postMethod.setHeader("X-Context", contextId);

		CloseableHttpResponse response3 = httpclient.execute(postMethod);
		try {
			HttpEntity entity = response3.getEntity();
			String jsonStr = IOUtils.toString(entity.getContent(), "UTF-8");
			System.out.println("Get stats post: " + response3.getStatusLine());
			System.out.println("Get stats result: " + jsonStr);
			
//			{
//				"result": {
//					"status": {
//						"dsl": {
//							"dsl0": {
//								"LastChangeTime": 151457,
//								"LastChange": 18586,
//								"UpstreamCurrRate": 3421,
//								"DownstreamCurrRate": 23160,
//								"LinkStatus": "Up",
//								"UpstreamMaxRate": 4218000,
//								"DownstreamMaxRate": 29717000,
//								"UpstreamAttenuation": 285,
//								"DownstreamAttenuation": 414,
//								"UpstreamNoiseMargin": 83,
//								"DownstreamNoiseMargin": 90,
//								"UpstreamPower": 63,
//								"DownstreamPower": 145,
//								"FirmwareVersion": "4132707636463033396a2e6432346e00",
//								"StandardsSupported": "G.992.1_Annex_A, G.992.1_Annex_B, G.992.1_Annex_C,T1.413, T1.413i2,ETSI_101_388, G.992.2,G.992.3_Annex_A, G.992.3_Annex_B, G.992.3_Annex_C, G.992.3_Annex_I, G.992.3_Annex_J,G.992.3_Annex_M, G.992.4,G.992.5_Annex_A, G.992.5_Annex_B, G.992.5_Annex_C, G.992.5_Annex_I, G.992.5_Annex_J, G.992.5_Annex_M, G.993.1,G.993.1_Annex_A, G.993.2_Annex_A, G.993.2_Annex_B",
//								"StandardUsed": "G.993.2_Annex_B",
//								"DataPath": "Interleaved",
//								"InterleaveDepth": 0,
//								"ModulationType": "VDSL",
//								"ModulationHint": "Auto",
//								"CurrentProfile": "17a",
//								"UPBOKLE": 236
//							}
//						}
//					}
//				}
			
			ObjectMapper objectMapper = new ObjectMapper();
			JsonNode tree = objectMapper.readTree(jsonStr.getBytes("UTF-8"));
			JsonNode resultNode = tree.get("result");
			JsonNode statusNode = resultNode.get("status");
			JsonNode dslNode = statusNode.get("dsl");
			dsl0Node = dslNode.get("dsl0");
		} finally {
			response3.close();
		}
	}
	
	public double getUpstreamCurrRate() throws Exception {
		
		if (dsl0Node == null) {
			getDsl0Status();
		}
		JsonNode node = dsl0Node.get("UpstreamCurrRate");
		double download = Double.parseDouble(node.asText());
		return download;
	}
	
	public double getDownStreamCurrRate() throws Exception {
		
		if (dsl0Node == null) {
			getDsl0Status();
		}
		JsonNode downstreamCurrRateNode = dsl0Node.get("DownstreamCurrRate");
		double download = Double.parseDouble(downstreamCurrRateNode.asText());
		return download;
	}

	
	public void disconnect() throws IOException {
		httpclient.close();
	}
	
	public void connect() throws Exception {
		
		BasicCookieStore cookieStore = new BasicCookieStore();

		SSLContextBuilder builder = new SSLContextBuilder();
		builder.loadTrustMaterial(null, new TrustAllStrategy());
		SSLConnectionSocketFactory sslsf = new SSLConnectionSocketFactory(builder.build(),
				NoopHostnameVerifier.INSTANCE);
		httpclient = HttpClients.custom().setSSLSocketFactory(sslsf)
				.setDefaultCookieStore(cookieStore).build();

		HttpGet httpget = new HttpGet(url + "/homeAuthentificationRemote.html");
		CloseableHttpResponse response1 = httpclient.execute(httpget);
		try {
			HttpEntity entity = response1.getEntity();

			System.out.println("Login form get: " + response1.getStatusLine());
			EntityUtils.consume(entity);

			System.out.println("Initial set of cookies:");
			List<Cookie> cookies = cookieStore.getCookies();
			if (cookies.isEmpty()) {
				System.out.println("None");
			} else {
				for (int i = 0; i < cookies.size(); i++) {
					System.out.println("- " + cookies.get(i).toString());
				}
			}
		} finally {
			response1.close();
		}

		
		List<NameValuePair> urlParameters = new ArrayList<NameValuePair>();
		urlParameters.add(new BasicNameValuePair("username", username));
		urlParameters.add(new BasicNameValuePair("password", password));

		HttpUriRequest login = RequestBuilder.post()
				.setUri(new URI(url + "/authenticate"))
				.addParameter("username", username)
				.addParameter("password", password)
				.setEntity(new UrlEncodedFormEntity(urlParameters))
				.build();

		CloseableHttpResponse response2 = httpclient.execute(login);
		try {
			HttpEntity entity = response2.getEntity();

			System.out.println("Login form get: " + response2.getStatusLine());

//				{
//					"status": 0,
//					"data": {
//						"contextID": "tWkF1qPJGMVvOqp14Whvy8JRQ2Q4brYsb9Y3nDzTgieSysGpeNIDLikp8Zi7e5pf"
//					}
//				}
			
			ObjectMapper objectMapper = new ObjectMapper();
			JsonNode tree = objectMapper.readTree(IOUtils.toByteArray(entity.getContent()));
			JsonNode dataNode = tree.get("data");
			JsonNode ctxNode = dataNode.get("contextID");
			contextId = ctxNode.asText();
			System.out.println("Post logon cookies:");
			List<Cookie> cookies = cookieStore.getCookies();
			if (cookies.isEmpty()) {
				System.out.println("None");
			} else {
				for (int i = 0; i < cookies.size(); i++) {
					System.out.println("- " + cookies.get(i).toString());
				}
			}
		} finally {
			response2.close();
		}
	}
}