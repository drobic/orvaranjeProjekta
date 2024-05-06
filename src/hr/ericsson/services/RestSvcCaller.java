package hr.ericsson.services;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.ProtocolException;
import java.net.URL;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.xml.soap.SOAPConnection;
import javax.xml.soap.SOAPConnectionFactory;
import javax.xml.soap.SOAPMessage;


public class RestSvcCaller {

	public RestSvcCaller() {}
	
	public void postSignaliToken() {

		try {
			CustomHttpsConnector connector = new CustomHttpsConnector();
			HttpsURLConnection postConn = connector.createHttpsConnection("https://gsbapi-pp.cdu.gov.hr/token");
			String urlParameters  = "grant_type=client_credentials&param2=data2";

			postConn.setRequestMethod("POST");
			postConn.setRequestProperty("Authorization", "Basic YmlrdmtaSzVGZHpESWtDdlkzZUFPcUxqd2JrYTpOb2NKVkxJNV9YUFdvR19CaUR1M2ZNX2VCWUVh");
			postConn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
			postConn.setRequestProperty("charset", "utf-8");

			postConn.setRequestProperty("grant_type", "client_credentials");

		} catch (ProtocolException e) {
			e.printStackTrace();
		}
	}
	
	public void getPosta(String urlConnection) {

		try {
			CustomHttpsConnector connector = new CustomHttpsConnector();
			HttpsURLConnection getConn = connector.createHttpsConnection(urlConnection);

			getConn.setRequestMethod("GET");
			//getConn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
			//getConn.setRequestProperty("charset", "utf-8");

			//getConn.setRequestProperty("grant_type", "client_credentials");

			//getConn.setRequestProperty("User-Agent", USER_AGENT);
			int responseCode = getConn.getResponseCode();
			BufferedReader in=null;
			System.out.println("GET Response Code :: " + responseCode);
				if (getConn.getErrorStream() != null)
					in = new BufferedReader(new InputStreamReader(getConn.getErrorStream()));
				else
					in = new BufferedReader(new InputStreamReader(getConn.getInputStream()));

				String inputLine;
				StringBuffer response = new StringBuffer();

				while ((inputLine = in.readLine()) != null) {
					response.append(inputLine);
				}
				in.close();

				// print result
				System.out.println(response.toString());
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
}
