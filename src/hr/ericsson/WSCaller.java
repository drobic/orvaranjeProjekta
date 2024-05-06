package hr.ericsson;

import java.io.*;
import java.net.URL;
import java.security.KeyStore;
import java.security.cert.CertificateException;
import java.util.Base64;
import java.util.Iterator;

import org.w3c.dom.*;

import com.sun.org.apache.xml.internal.security.utils.XMLUtils;
import com.sun.xml.internal.ws.util.xml.XmlUtil;

import javax.net.ssl.*;
import javax.security.cert.X509Certificate;
import javax.xml.namespace.QName;
import javax.xml.soap.*;
import javax.xml.transform.stream.StreamSource;

import hr.ericsson.services.CustomHttpsConnector;
import hr.ericsson.services.RestSvcCaller;
import hr.ericsson.services.ZISservisi;
import hr.ericsson.utils.Props;
import hr.ericsson.utils.XmlUtils;

public class WSCaller {

    // SAAJ - SOAP Client Testing
    public static void main(String args[]) {

    	//ovo je samo komentar za git
    	System.setProperty("com.sun.xml.ws.transport.http.client.HttpTransportPipe.dump", "true");
    	System.setProperty("com.sun.xml.internal.ws.transport.http.client.HttpTransportPipe.dump", "true");
    	System.setProperty("com.sun.xml.ws.transport.http.HttpAdapter.dump", "true");
    	System.setProperty("com.sun.xml.internal.ws.transport.http.HttpAdapter.dump", "true");
    	 String soapAction = "";
    	try {

    		if(args.length>0 && args[0].equalsIgnoreCase("HTTPS"))
            {
            	String endpointUrl = Props.getInstance().getProperty("https.url.request");
            	CustomHttpsConnector conn = new CustomHttpsConnector();
            	conn.testSSLHandshake();
            	
            	//RestSvcCaller caller = new RestSvcCaller();
            	//caller.getPosta(endpointUrl);

                String soapEndpointUrl = Props.getInstance().getProperty("url.request");
                ZISservisi servis = new ZISservisi();
                servis.pozoviSoapWS();
                ////SOAPMessage sm= callSoapWebService(soapEndpointUrl, soapAction);
                System.out.println("*****"+soapEndpointUrl);

            
            }else{
            String soapEndpointUrl = Props.getInstance().getProperty("url.request");
            ZISservisi servis = new ZISservisi();
            servis.pozoviSoapWS();
            //SOAPMessage sm= callSoapWebService(soapEndpointUrl, soapAction);
            System.out.println("*****"+soapEndpointUrl);
//            createPDF(sm);
            }
    	} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

    	
//    	String soapEndpointUrl = "https://testoss.uredjenazemlja.hr/OssWebServices/services/";
       
        
        
    }

 
    
    private static void createPDF(SOAPMessage message) {
       try {
    	ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        message.writeTo(outputStream);
        byte[] byteArray = outputStream.toByteArray();
        ByteArrayInputStream inputStream = new ByteArrayInputStream(byteArray);

//		FileInputStream is = new FileInputStream(Props.getInstance().getProperty("file.request"));
		XmlUtils xu = new XmlUtils();
		System.out.println();
		File file = new File("./test.pdf");
		FileOutputStream fos = new FileOutputStream(file);
		byte[] decoder = Base64.getDecoder().decode(xu.getIzvadak(inputStream));
	      fos.write(decoder);
	      System.out.println("PDF File Saved");
       } catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
    }
    
    private static void createSoapEnvelope(SOAPMessage soapMessage) throws SOAPException {
        SOAPPart soapPart = soapMessage.getSOAPPart();


   
        String myNamespace = "oss";
        String myNamespaceURI = "http://oss.authentication.hr";

        // SOAP Envelope
        SOAPEnvelope envelope = soapPart.getEnvelope();
        envelope.addNamespaceDeclaration(myNamespace, myNamespaceURI);

            /*
            Constructed SOAP Request Message:
            <SOAP-ENV:Envelope xmlns:SOAP-ENV="http://schemas.xmlsoap.org/soap/envelope/" xmlns:myNamespace="https://www.w3schools.com/xml/">
                <SOAP-ENV:Header/>
                <SOAP-ENV:Body>
                    <myNamespace:CelsiusToFahrenheit>
                        <myNamespace:Celsius>100</myNamespace:Celsius>
                    </myNamespace:CelsiusToFahrenheit>
                </SOAP-ENV:Body>
            </SOAP-ENV:Envelope>
            */

        // SOAP Body
        SOAPBody soapBody = envelope.getBody();
        SOAPElement soapBodyAuthenticationRequest = soapBody.addChildElement("authenticationRequest", myNamespace);
        SOAPElement soapBodyUsername = soapBodyAuthenticationRequest.addChildElement("username", myNamespace);
        SOAPElement soapBodyPassword = soapBodyAuthenticationRequest.addChildElement("password", myNamespace);
        soapBodyUsername.addTextNode("enotartest");
        soapBodyPassword.addTextNode("admin123");

    }
    
}


