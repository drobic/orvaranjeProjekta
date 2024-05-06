package hr.ericsson.services;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.security.KeyStore;
import java.security.SecureRandom;
import java.security.cert.CertificateException;
import java.util.Base64;
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
import javax.xml.soap.MimeHeaders;
import javax.xml.soap.SOAPConnection;
import javax.xml.soap.SOAPConnectionFactory;
import javax.xml.soap.SOAPException;
import javax.xml.soap.SOAPMessage;
import javax.xml.soap.SOAPPart;
import javax.xml.transform.stream.StreamSource;

import hr.ericsson.utils.Props;
import hr.ericsson.utils.XmlUtils;

public class DigitalnaArhivaZKServis {

	private String serviceUrl;
	private String sslhost;
	private String sslport;
	private String tlsType;
	private String authString;
	private String requestFileUlozak;
	private String requestFilePretragaCestice;
	private String requestFilePretragaUloska;
	private String requestFilePotpisani;
	private String requestFileDocumentsMerge;


	public DigitalnaArhivaZKServis() {
		try {
			this.serviceUrl = Props.getInstance().getProperty("da.url.request");
			this.sslhost = Props.getInstance().getProperty("da.ssl.url.host");
			this.sslport = Props.getInstance().getProperty("da.ssl.url.port");
			this.tlsType = Props.getInstance().getProperty("da.tls.type");
			this.requestFileUlozak=Props.getInstance().getProperty("da.request.file.ulozak");
			this.requestFilePretragaUloska=Props.getInstance().getProperty("da.request.file.pretragauloska");
			this.requestFilePretragaCestice=Props.getInstance().getProperty("da.request.file.pretragacestice");
			this.requestFilePotpisani=Props.getInstance().getProperty("da.request.file.potpisani");
			this.requestFileDocumentsMerge=Props.getInstance().getProperty("da.request.file.merganje");
			
		} catch (Exception e) {
			e.printStackTrace();
		}

	}

	public void checkAll() {
		testTLShandshake();
		if(this.requestFileUlozak !=null) 
			getUlozak();
		if(this.requestFilePretragaUloska !=null) 
			searchUlozak();
		if(this.requestFilePretragaCestice !=null) 
			searchCestica();
		if(this.requestFilePotpisani !=null) 
			potpisaniIzvadak();
		if(this.requestFileDocumentsMerge !=null) 
			mergeDocuments();

	}
	
	public void getUlozak() {
		//ArhivskiIzvadakRequest
        System.out.println("Response dohvata uloska:");
		createPDF(callSoapWebService(this.serviceUrl, this.requestFileUlozak, ""), "ulozak");

	}

	public void searchUlozak() {
		//SearchRequest
        System.out.println("Response pretrage po ulosku:");
		SOAPMessage sm=callSoapWebService(this.serviceUrl, this.requestFilePretragaUloska, "");
        try {
			sm.writeTo(System.out);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	public void searchCestica() {
		//SearchRequest
		System.out.println("Response pretrage po cestici:");
        SOAPMessage sm=callSoapWebService(this.serviceUrl, this.requestFilePretragaCestice, "");
        try {
			sm.writeTo(System.out);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void potpisaniIzvadak() {
		//SignedArhivskiIzvadakRequest
        // Print the SOAP Response
        System.out.println("Request potpisani izvadak:");
        SOAPMessage sm=callSoapWebService(this.serviceUrl, this.requestFilePotpisani, "");
        try {
			sm.writeTo(System.out);
		} catch (Exception e) {
			e.printStackTrace();
		}

	}
	
	public void mergeDocuments() {
		//MergedDocumentRequest
        System.out.println("Request dohvat skeniranih stranica u jednu merganu:");
		createPDF(callSoapWebService(this.serviceUrl, this.requestFileDocumentsMerge, ""), "mergano");
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
	
	private SOAPMessage callSoapWebService(String soapEndpointUrl, String requestFileName, String soapAction) {
		HttpsURLConnection httpsConnection = null;
		SOAPMessage soapResponse=null;
		try {
			SSLContext sslContext= initSSLContext();
			// Set trust all certificates context to HttpsURLConnection
			HttpsURLConnection.setDefaultSSLSocketFactory(sslContext.getSocketFactory());
			// Open HTTPS connection
			URL url = new URL(this.serviceUrl);
			httpsConnection = (HttpsURLConnection) url.openConnection();
			// Trust all hosts
			httpsConnection.setHostnameVerifier(new TrustAllHosts());
			httpsConnection.setDefaultHostnameVerifier(new NullHostnameVerifier());
			// Connect
			//		httpsConnection.connect();
			// Create SOAP Connection
			SOAPConnectionFactory soapConnectionFactory = SOAPConnectionFactory.newInstance();
			SOAPConnection soapConnection = soapConnectionFactory.createConnection();

			// Send SOAP Message to SOAP Server
			soapResponse = soapConnection.call(createSOAPRequest(requestFileName, soapAction), soapEndpointUrl);

			soapConnection.close();
			httpsConnection.disconnect();
		} catch (Exception e) {
			System.err.println("\nError occurred while sending SOAP Request to Server!\nMake sure you have the correct endpoint URL and SOAPAction!\n");
			e.printStackTrace();
		}
		return soapResponse;
	}


	private SSLContext initSSLContext() {
		SSLContext sslContext=null;
		try {
			sslContext = SSLContext.getInstance(this.tlsType);
			TrustManager[] trustAll = new TrustManager[] { new TrustAllCertificates() };
			sslContext.init(null, trustAll,  new SecureRandom());

		} catch (Exception e) {
			e.printStackTrace();
		}  
		return sslContext;
	}


	private  void createPDF(SOAPMessage message, String naziv) {
		try {
			ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
			message.writeTo(outputStream);
			byte[] byteArray = outputStream.toByteArray();
			ByteArrayInputStream inputStream = new ByteArrayInputStream(byteArray);

			//		FileInputStream is = new FileInputStream(Props.getInstance().getProperty("file.request"));
			XmlUtils xu = new XmlUtils();
			System.out.println();
			File file = new File("./"+naziv+"_response.pdf");
			FileOutputStream fos = new FileOutputStream(file);
			byte[] decoder = Base64.getDecoder().decode(xu.getIzvadak(inputStream));
			fos.write(decoder);
			System.out.println("PDF response je spremljen u datoteku "+naziv+"_response.pdf");
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}




	private  SOAPMessage createSOAPRequest(String requestFileName, String soapAction) throws Exception {
		MessageFactory messageFactory = MessageFactory.newInstance();
		SOAPMessage soapMessage = messageFactory.createMessage();

		createSoapEnvelopeFromFile(soapMessage, requestFileName);
		//createSoapEnvelope(soapMessage);

		MimeHeaders headers = soapMessage.getMimeHeaders();
		headers.addHeader("SOAPAction", soapAction);
		Iterator iter=headers.getAllHeaders();
		while (iter.hasNext()){
			System.out.println(iter.next());	
		}

		headers.getAllHeaders();;
		soapMessage.saveChanges();

		System.out.println("Request SOAP Message:");
		soapMessage.writeTo(System.out);
		System.out.println("\n");

		return soapMessage;
	}

	private void createSoapEnvelopeFromFile(SOAPMessage soapMessage, String rfn) throws SOAPException {
		SOAPPart soapPart = soapMessage.getSOAPPart();
		try {
			FileInputStream is = new FileInputStream(rfn);
			soapPart.setContent(new StreamSource(is));
		} catch (IOException e) {
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
