package hr.ericsson.services;

import java.net.URL;
import java.security.cert.CertificateException;
import java.net.MalformedURLException;
import java.security.KeyStore;
import java.security.cert.Certificate;
import javax.net.ssl.SSLPeerUnverifiedException;

import java.io.*;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import javax.xml.soap.SOAPConnection;
import javax.xml.soap.SOAPConnectionFactory;
import javax.xml.soap.SOAPMessage;

import hr.ericsson.utils.Props;

import java.security.SecureRandom;


public class CustomHttpsConnector {

	public void testSSLHandshake() {
		try {
			String keystoreUrl = Props.getInstance().getProperty("keystore.url");
			String keystorePassword = Props.getInstance().getProperty("keystore.password");
			String sslhost = Props.getInstance().getProperty("ssl.url.host");
			String sslport = Props.getInstance().getProperty("ssl.url.port");

			
			//SSLSocketFactory sslSocketFactory = (SSLSocketFactory) SSLSocketFactory.getDefault();
		    SSLContext sslContext = SSLContext.getInstance("TLS");
			TrustManager[] trustAll = new TrustManager[] { new TrustAllCertificates() };
		    KeyStore keystore = KeyStore.getInstance(KeyStore.getDefaultType());
			try (InputStream in = new FileInputStream(keystoreUrl)) {
			   keystore.load(in, keystorePassword.toCharArray());
			}catch (Exception e) {
				e.printStackTrace();
			}
			KeyManagerFactory keyManagerFactory =
				    KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
				  keyManagerFactory.init(keystore, keystorePassword.toCharArray());
			sslContext.init(
						    keyManagerFactory.getKeyManagers(),
						    trustAll,
						    new SecureRandom());

		    
		    SSLSocketFactory sslSocketFactory = sslContext.getSocketFactory();
		    SSLSocket sslSocket = (SSLSocket) sslSocketFactory.createSocket(sslhost, Integer.parseInt(sslport));
		    sslSocket.startHandshake();

            // Check if handshake is successful
            if (sslSocket.getSession().isValid()) {
                System.out.println("SSL handshake established successfully.");
            } else {
                System.out.println("SSL handshake failed.");
            }

            // Close the socket
            sslSocket.close();

		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	public HttpsURLConnection createHttpsConnection(String httpsUrl) {
		HttpsURLConnection httpsConnection = null;
		try {
			String keystoreUrl = Props.getInstance().getProperty("keystore.url");
			String keystorePassword = Props.getInstance().getProperty("keystore.password");
			SSLContext sslContext = SSLContext.getInstance("TLS");
			TrustManager[] trustAll = new TrustManager[] { new TrustAllCertificates() };
//			sslContext.init(null, trustAll, new java.security.SecureRandom());
			  KeyStore keystore = KeyStore.getInstance(KeyStore.getDefaultType());
			  try (InputStream in = new FileInputStream(keystoreUrl)) {
			   keystore.load(in, keystorePassword.toCharArray());
			  }
			KeyManagerFactory keyManagerFactory =
				    KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
				  keyManagerFactory.init(keystore, keystorePassword.toCharArray());
			
				  sslContext.init(
						    keyManagerFactory.getKeyManagers(),
						    trustAll,
						    new SecureRandom());

				  
			// Set trust all certificates context to HttpsURLConnection
			HttpsURLConnection.setDefaultSSLSocketFactory(sslContext.getSocketFactory());

				  
			// Open HTTPS connection
			URL url = new URL(httpsUrl);
			httpsConnection = (HttpsURLConnection) url.openConnection();
			// Trust all hosts
			httpsConnection.setHostnameVerifier(new TrustAllHosts());
			httpsConnection.setDefaultHostnameVerifier(new NullHostnameVerifier());

		} catch (Exception e) {
			System.err.println(
					"\nError occurred while sending SOAP Request to Server!\nMake sure you have the correct endpoint URL and SOAPAction!\n");
			e.printStackTrace();
		}

		return httpsConnection;
	}
	
	   public void testConnection(String httpsUrl){

		      URL url;
		      try {

			     url = new URL(httpsUrl);
			     HttpsURLConnection con = (HttpsURLConnection)url.openConnection();
					
			     //dumpl all cert info
			     print_https_cert(con);
					
			     //dump all the content
			     print_content(con);
					
		      } catch (MalformedURLException e) {
			     e.printStackTrace();
		      } catch (IOException e) {
			     e.printStackTrace();
		      }

		   }

	   private void print_https_cert(HttpsURLConnection con){
		     
		    if(con!=null){
					
		      try {
						
			System.out.println("Response Code : " + con.getResponseCode());
			System.out.println("Cipher Suite : " + con.getCipherSuite());
			System.out.println("\n");
						
			Certificate[] certs = con.getServerCertificates();
			for(Certificate cert : certs){
			   System.out.println("Cert Type : " + cert.getType());
			   System.out.println("Cert Hash Code : " + cert.hashCode());
			   System.out.println("Cert Public Key Algorithm : " 
		                                    + cert.getPublicKey().getAlgorithm());
			   System.out.println("Cert Public Key Format : " 
		                                    + cert.getPublicKey().getFormat());

			   System.out.println("\n");
			}
						
			} catch (SSLPeerUnverifiedException e) {
				e.printStackTrace();
			} catch (IOException e){
				e.printStackTrace();
			}

		     }
			
		   }
			
		   private void print_content(HttpsURLConnection con){
			if(con!=null){
					
			try {
				
			   System.out.println("****** Content of the URL ********");			
			   BufferedReader br = 
				new BufferedReader(
					new InputStreamReader(con.getInputStream()));
						
			   String input;
						
			   while ((input = br.readLine()) != null){
			      System.out.println(input);
			   }
			   br.close();
						
			} catch (IOException e) {
			   e.printStackTrace();
			}
					
		       }
				
		   }
		   
		   public KeyManagerFactory getClientCertificate() {
			   KeyManagerFactory keyManagerFactory=null;
			   try {
					String keystoreUrl = Props.getInstance().getProperty("oib.keystore.url");
					String keystorePassword = Props.getInstance().getProperty("oib.keystore.password");

					//SSLSocketFactory sslSocketFactory = (SSLSocketFactory) SSLSocketFactory.getDefault();
				    SSLContext sslContext = SSLContext.getInstance("TLS");
					TrustManager[] trustAll = new TrustManager[] { new TrustAllCertificates() };
				    KeyStore keystore = KeyStore.getInstance(KeyStore.getDefaultType());
					try (InputStream in = new FileInputStream(keystoreUrl)) {
					   keystore.load(in, keystorePassword.toCharArray());
					}catch (Exception e) {
						e.printStackTrace();
					}
					keyManagerFactory =
						    KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
						  keyManagerFactory.init(keystore, keystorePassword.toCharArray());
				} catch (Exception e) {
					e.printStackTrace();
				}
				return keyManagerFactory;
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
