package hr.ericsson.services;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.security.KeyStore;
import java.security.SecureRandom;
import java.security.cert.CertificateException;
import java.util.Iterator;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import javax.xml.soap.MessageFactory;
import javax.xml.soap.MimeHeader;
import javax.xml.soap.MimeHeaders;
import javax.xml.soap.SOAPConnection;
import javax.xml.soap.SOAPConnectionFactory;
import javax.xml.soap.SOAPException;
import javax.xml.soap.SOAPMessage;
import javax.xml.soap.SOAPPart;
import javax.xml.transform.stream.StreamSource;

import hr.ericsson.utils.Props;


public class MPUOglasna {

	private String serviceUrl;
	private String keystoreUrl;
	private String keystorePassword;
	private String sslhost;
	private String sslport;
	private String tlsType;
	private String authString;
	private String requestFile;
	
	public MPUOglasna() {
		try {
			this.serviceUrl = Props.getInstance().getProperty("oglasna.url.request");
			this.keystoreUrl = Props.getInstance().getProperty("oglasna.ssl.keystore.url");
			this.keystorePassword = Props.getInstance().getProperty("oglasna.ssl.keystore.password");
			this.sslhost = Props.getInstance().getProperty("oglasna.ssl.url.host");
			this.sslport = Props.getInstance().getProperty("oglasna.ssl.url.port");
			this.tlsType = Props.getInstance().getProperty("oglasna.tls.type");
			this.authString = Props.getInstance().getProperty("oglasna.auth.string");
			this.requestFile=Props.getInstance().getProperty("oglasna.request.file");
		} catch (Exception e) {
			e.printStackTrace();
		}

	}
	
	public void checkAll() {
		testTLShandshake();
		testCredentials();
	}

	
	
	private void testCredentials() {
		HttpsURLConnection httpsConnection = null;
		SOAPMessage soapResponse = null;
		try {
			SSLContext sslContext = SSLContext.getInstance("SSL");

			TrustManager[] trustAll = new TrustManager[] { new TrustAllCertificates() };
		
			sslContext.init(null, trustAll, new java.security.SecureRandom());
			// Set trust all certificates context to HttpsURLConnection
			HttpsURLConnection.setDefaultSSLSocketFactory(sslContext.getSocketFactory());
			// Open HTTPS connection
			URL url = new URL(serviceUrl);
			httpsConnection = (HttpsURLConnection) url.openConnection();
			// Trust all hosts
			httpsConnection.setHostnameVerifier(new TrustAllHosts());
			httpsConnection.setDefaultHostnameVerifier(new NullHostnameVerifier());

//Ako nema ovoga gore Soap Connectipon.call javalja "pkix path building failed: com.ibm.security.cert.ibmcertpathbuilderexception: unable to find valid certification path to requested target"			
			
			// Create SOAP Connection
			SOAPConnectionFactory soapConnectionFactory = SOAPConnectionFactory.newInstance();
			SOAPConnection soapConnection = soapConnectionFactory.createConnection();

			// Send SOAP Message to SOAP Server
			SOAPMessage sm = createSOAPRequest("");
			System.out.println("Pozivam soapConnection.call...");
			
			Iterator i= sm.getMimeHeaders().getAllHeaders();
			while(i.hasNext()) {
				MimeHeader mh=(MimeHeader)i.next();
				System.out.println(mh.getName()+"**"+mh.getValue());
			}

			soapResponse = soapConnection.call(sm, serviceUrl);

			// Print the SOAP Response
			System.out.println("Response SOAP Message:");
			soapResponse.writeTo(System.out);
			i= soapResponse.getMimeHeaders().getAllHeaders();
			while(i.hasNext()) {
				MimeHeader mh=(MimeHeader)i.next();
				System.out.println(mh.getName()+"**"+mh.getValue());
			}
			
			soapConnection.close();
		} catch (Exception e) {
			System.err.println(
					"\nError occurred while sending SOAP Request to Server!\nMake sure you have the correct endpoint URL and SOAPAction!\n");
			e.printStackTrace();
		}

	}
	
	
	public void testTLShandshake() {
		try {
            System.out.println("Starting TLS handshake...");
			SSLContext sc=initSSLContext();

		    
		    SSLSocketFactory sslSocketFactory = sc.getSocketFactory();
		    SSLSocket sslSocket = (SSLSocket) sslSocketFactory.createSocket(sslhost, Integer.parseInt(sslport));
		    sslSocket.startHandshake();

            // Check if handshake is successful
            if (sslSocket.getSession().isValid()) {
                System.out.println("SSL handshake established successfully.");
                System.out.println(sslSocket.getSession().getValueNames());
            } else {
                System.out.println("SSL handshake failed.");
            }

            // Close the socket
            sslSocket.close();

		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
    private SOAPMessage createSOAPRequest(String soapAction) throws Exception {
        
    	SOAPMessage soapMessage=null;
		try {
			MessageFactory messageFactory = MessageFactory.newInstance();
			soapMessage = messageFactory.createMessage();
    
			SOAPPart sp=createSoapEnvelopeFromFile(soapMessage);

			MimeHeaders headers = soapMessage.getMimeHeaders();
			headers.addHeader("SOAPAction", soapAction);
			headers.addHeader("Authorization", "Basic "+this.authString);
			headers.addHeader("Content-Type", "text/plain");
			Iterator iter=headers.getAllHeaders();
			while (iter.hasNext()){
				System.out.println(iter.next().toString());	
			}
			        
			headers.getAllHeaders();
			soapMessage.saveChanges();
			
			/* Print the request message, just for debugging purposes */
			System.out.println("Request SOAP Message:");
			soapMessage.writeTo(System.out);
			System.out.println("\n");
		} catch (Exception e) {
			e.printStackTrace();
		}
        return soapMessage;
    }
    
	private SOAPPart createSoapEnvelopeFromFile(SOAPMessage soapMessage) throws SOAPException {
		SOAPPart soapPart = soapMessage.getSOAPPart();
		try {
			FileInputStream is = new FileInputStream(this.requestFile);
			soapPart.setContent(new StreamSource(is));

		} catch (IOException e) {
			e.printStackTrace();
		}
		return soapPart;
	}

	
	private SSLContext initSSLContext() {
		SSLContext sslContext=null;
		try {
			sslContext = SSLContext.getInstance(this.tlsType);
			TrustManager[] trustAll = new TrustManager[] { new TrustAllCertificates() };
			KeyStore keystore = KeyStore.getInstance(KeyStore.getDefaultType());
			try (InputStream in = new FileInputStream(this.keystoreUrl)) {
			   keystore.load(in, this.keystorePassword.toCharArray());
			}catch (Exception e) {
				e.printStackTrace();
			}
			KeyManagerFactory keyManagerFactory =
				    KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
				  keyManagerFactory.init(keystore, keystorePassword.toCharArray());
			sslContext.init(keyManagerFactory.getKeyManagers(),
						    trustAll,
						    new SecureRandom());

	    } catch (Exception e) {
			e.printStackTrace();
		}  
		return sslContext;
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
