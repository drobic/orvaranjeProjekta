package hr.ericsson.services;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.io.IOException;
import java.security.cert.CertificateException;
import java.util.Iterator;

import java.net.URL;
import java.net.URLPermission;
import java.net.HttpURLConnection;


import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import javax.xml.soap.MessageFactory;
import javax.xml.soap.MimeHeaders;
import javax.xml.soap.SOAPConnection;
import javax.xml.soap.SOAPConnectionFactory;
import javax.xml.soap.SOAPException;
import javax.xml.soap.SOAPMessage;
import javax.xml.soap.SOAPPart;
import javax.xml.transform.stream.StreamSource;

import hr.ericsson.utils.Props;



public class ZISservisi {

	private String soapEndpointUrl;
	private String soapAction;
	
	public ZISservisi() {
        soapEndpointUrl = Props.getInstance().getProperty("url.request");
        soapAction="";
	};
	
	public void pozoviSoapWS() {
		callSoapWebService(soapEndpointUrl, soapAction);
	}
	
	public void pozoviPostaWS() {
		callSoapWebService(soapEndpointUrl, soapAction);
	}

	
	private SOAPMessage callSoapWebService(String soapEndpointUrl, String soapAction) {
		HttpsURLConnection httpsConnection = null;
		SOAPMessage soapResponse = null;
		try {
			SSLContext sslContext = SSLContext.getInstance("SSL");

			TrustManager[] trustAll = new TrustManager[] { new TrustAllCertificates() };
			CustomHttpsConnector chc= new CustomHttpsConnector();
			KeyManagerFactory keyManagerFactory=chc.getClientCertificate();
		
			sslContext.init(keyManagerFactory.getKeyManagers(), trustAll, new java.security.SecureRandom());
			// Set trust all certificates context to HttpsURLConnection
			//default socket factory se korisiti od strane SoapConnectionFactoryja
			HttpsURLConnection.setDefaultSSLSocketFactory(sslContext.getSocketFactory());
			// Open HTTPS connection
			URL url = new URL(soapEndpointUrl);
			httpsConnection = (HttpsURLConnection) url.openConnection();
			// Trust all hosts
			httpsConnection.setHostnameVerifier(new TrustAllHosts());
			httpsConnection.setDefaultHostnameVerifier(new NullHostnameVerifier());
			// Connect
//			httpsConnection.connect();
			// Create SOAP Connection
			SOAPConnectionFactory soapConnectionFactory = SOAPConnectionFactory.newInstance();
			SOAPConnection soapConnection = soapConnectionFactory.createConnection();

			// Send SOAP Message to SOAP Server
			SOAPMessage sm = createSOAPRequest(soapAction);
//			System.out.println("Pozivam soapConnection.get...");
//			soapResponse = soapConnection.get(soapEndpointUrl);
//			soapResponse.writeTo(System.out);
			System.out.println("Pozivam soapConnection.call...");
			soapResponse = soapConnection.call(sm, soapEndpointUrl);

			// Print the SOAP Response
			System.out.println("Response SOAP Message:");
			soapResponse.writeTo(System.out);

			soapConnection.close();
		} catch (Exception e) {
			System.err.println(
					"\nError occurred while sending SOAP Request to Server!\nMake sure you have the correct endpoint URL and SOAPAction!\n");
			e.printStackTrace();
		}
		return soapResponse;
	}
	
    private static SOAPMessage createSOAPRequest(String soapAction) throws Exception {
        MessageFactory messageFactory = MessageFactory.newInstance();
        SOAPMessage soapMessage = messageFactory.createMessage();
     
        createSoapEnvelopeFromFile(soapMessage);
        //createSoapEnvelope(soapMessage);

        MimeHeaders headers = soapMessage.getMimeHeaders();
        headers.addHeader("SOAPAction", soapAction);
        headers.addHeader("Authorization", "Basic "+Props.getInstance().getProperty("auth.string"));
        headers.addHeader("Content-Type", "text/xml");
        Iterator iter=headers.getAllHeaders();
        while (iter.hasNext()){
        	System.out.println(iter.next().toString());	
        }
        
        headers.getAllHeaders();;
        soapMessage.saveChanges();
        
        /* Print the request message, just for debugging purposes */
        System.out.println("Request SOAP Message:");
        soapMessage.writeTo(System.out);
        System.out.println("\n");

        return soapMessage;
    }
    
    private static SOAPMessage getOIBData(String soapAction) throws Exception {
        MessageFactory messageFactory = MessageFactory.newInstance();
        SOAPMessage soapMessage = messageFactory.createMessage();
     
        createSoapEnvelopeFromFile(soapMessage);
        //createSoapEnvelope(soapMessage);

        MimeHeaders headers = soapMessage.getMimeHeaders();
        headers.addHeader("SOAPAction", soapAction);
        headers.addHeader("Authorization", "Basic "+Props.getInstance().getProperty("auth.string"));
        headers.addHeader("Content-Type", "text/xml");
        Iterator iter=headers.getAllHeaders();
        while (iter.hasNext()){
        	System.out.println(iter.next().toString());	
        }
        
        headers.getAllHeaders();;
        soapMessage.saveChanges();
        
        /* Print the request message, just for debugging purposes */
        System.out.println("Request SOAP Message:");
        soapMessage.writeTo(System.out);
        System.out.println("\n");

        return soapMessage;
    }

    
	private static void createSoapEnvelopeFromFile(SOAPMessage soapMessage) throws SOAPException {
		SOAPPart soapPart = soapMessage.getSOAPPart();
		try {
			FileInputStream is = new FileInputStream(Props.getInstance().getProperty("file.request"));
			soapPart.setContent(new StreamSource(is));

		} catch (IOException e) {
			e.printStackTrace();
		}
	}
    
	
	public void sendSOAP_over_http() {
		try {
			
			URL obj=new URL(Props.getInstance().getProperty("url.request"));
			HttpsURLConnection conn = (HttpsURLConnection) obj.openConnection();
			conn.setRequestMethod("POST");
			conn.setRequestProperty("", "");
			String xml="";
			conn.setDoInput(true);
			DataOutputStream wr = new DataOutputStream(conn.getOutputStream());
			wr.writeBytes(xml);
			wr.flush();
			wr.close();
			String responseStatus = conn.getResponseMessage();
			System.out.println(responseStatus);
			
			//BufferedReader in = new BufferedReader(new InputStremReader(conn.getInputStream()));
			
		} catch (Exception e) {
			e.printStackTrace();
		}

	}	
	
    private static class TrustAllCertificates implements X509TrustManager {


    	@Override
    	public void checkClientTrusted(java.security.cert.X509Certificate[] chain, String authType)
    			throws CertificateException {
    		// TODO Auto-generated method stub
    		
    	}

    	@Override
    	public void checkServerTrusted(java.security.cert.X509Certificate[] chain, String authType)
    			throws CertificateException {
    		// TODO Auto-generated method stub
    		
    	}

    	@Override
    	public java.security.cert.X509Certificate[] getAcceptedIssuers() {
    		// TODO Auto-generated method stub
    		return null;
    	}
    }
    
    private static class TrustAllHosts implements HostnameVerifier {
    	public boolean verify(String hostname, SSLSession session) {
    		return true;
    	}
    }
    
    private static class NullHostnameVerifier implements HostnameVerifier {
        public boolean verify(String hostname, SSLSession session) {
            return true;
        }
    }
	
}
