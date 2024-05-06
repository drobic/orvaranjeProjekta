package hr.ericsson.services;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.io.StringWriter;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Security;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.Scanner;

import javax.xml.crypto.dom.DOMStructure;
import javax.xml.crypto.dsig.Reference;
import javax.xml.crypto.dsig.SignedInfo;
import javax.xml.crypto.dsig.Transform;
import javax.xml.crypto.dsig.XMLSignatureFactory;
import javax.xml.crypto.dsig.dom.DOMSignContext;
import javax.xml.crypto.dsig.keyinfo.KeyInfoFactory;
import javax.xml.crypto.dsig.spec.C14NMethodParameterSpec;
import javax.xml.crypto.dsig.spec.TransformParameterSpec;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.soap.MessageFactory;
import javax.xml.soap.MimeHeader;
import javax.xml.soap.MimeHeaders;
import javax.xml.soap.SOAPBody;
import javax.xml.soap.SOAPConnection;
import javax.xml.soap.SOAPConnectionFactory;
import javax.xml.soap.SOAPConstants;
import javax.xml.soap.SOAPElement;
import javax.xml.soap.SOAPEnvelope;
import javax.xml.soap.SOAPException;
import javax.xml.soap.SOAPHeader;
import javax.xml.soap.SOAPMessage;
import javax.xml.soap.SOAPPart;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.apache.xml.security.c14n.Canonicalizer;
import org.apache.xml.security.transforms.Transforms;
import org.apache.xml.security.utils.Constants;
import org.w3c.dom.Document;
import org.xml.sax.InputSource;

import hr.ericsson.utils.Props;

public class PostaServis {

    static final String 
    WSSE_NS = "http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-wssecurity-secext-1.0.xsd",
    WSU_NS  = "http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-wssecurity-utility-1.0.xsd",
    DSIG_NS = "http://www.w3.org/2000/09/xmldsig#", // javax.xml.crypto.dsig.XMLSignature.XMLNS, Constants.SignatureSpecNS
    
    binarySecurityToken_Encoding = "http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-soap-message-security-1.0#Base64Binary",
    binarySecurityToken_Value = "http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-x509-token-profile-1.0#X509v3",
    
    signatureMethodAlog_SHA1 = DSIG_NS + "rsa-sha1", // XMLSignature.ALGO_ID_SIGNATURE_RSA_SHA1
    digestMethodAlog_SHA1  = Constants.ALGO_ID_DIGEST_SHA1, // DSIG_NS + "sha1", // Constants.ALGO_ID_DIGEST_SHA1
    transformAlog = Transforms.TRANSFORM_C14N_EXCL_OMIT_COMMENTS, //"http://www.w3.org/2001/10/xml-exc-c14n#";
    canonicalizerAlog = Canonicalizer.ALGO_ID_C14N_EXCL_OMIT_COMMENTS; //"http://www.w3.org/2001/10/xml-exc-c14n#"; CanonicalizationMethod.EXCLUSIVE
    
	static final String serviceUrl = Props.getInstance().getProperty("posta.url.request");
	static final String privateKeyFilePath = Props.getInstance().getProperty("posta.privatekey.path");
	static final String publicKeyFilePath = Props.getInstance().getProperty("posta.publickey.path");
	static final String inputFile = Props.getInstance().getProperty("posta.request.file");
	static final String outputFile = Props.getInstance().getProperty("posta.response.file");

    
    static {
        Security.addProvider(new org.bouncycastle.jce.provider.BouncyCastleProvider());
    }
    public static X509Certificate loadPublicKeyX509(InputStream cerFileStream) throws CertificateException, NoSuchProviderException {
        CertificateFactory  certificateFactory = CertificateFactory.getInstance("X.509", "BC");
        X509Certificate x509Certificate = (X509Certificate) certificateFactory.generateCertificate(cerFileStream);
        return x509Certificate;
    }
    public static PrivateKey loadPrivateKeyforSigning(InputStream cerFileStream, String password) throws UnrecoverableKeyException, KeyStoreException, NoSuchAlgorithmException, CertificateException, IOException, NoSuchProviderException {
        KeyStore keyStore = KeyStore.getInstance("PKCS12"); //, "BC");
        keyStore.load(cerFileStream, password.toCharArray());
        
        Enumeration<String> keyStoreAliasEnum = keyStore.aliases();
        PrivateKey privateKey = null;
        String alias = null;
        if ( keyStoreAliasEnum.hasMoreElements() ) {
            alias = keyStoreAliasEnum.nextElement();
            if (password != null) {
                privateKey = (PrivateKey) keyStore.getKey(alias, password.toCharArray());
            }
        }
        return privateKey;
    }
    static X509Certificate loadPublicKeyX509;
    static PrivateKey privateKey;
    
    public void callWS() {
    	
        try {
			InputStream pkcs_FileStream = new FileInputStream(privateKeyFilePath);
			privateKey = loadPrivateKeyforSigning(pkcs_FileStream, "Sso5516Pm");//Password of PFX file
			System.out.println("privateKey : "+privateKey);
			
			InputStream cerFileStream = new FileInputStream(publicKeyFilePath);
			loadPublicKeyX509 = loadPublicKeyX509(cerFileStream);
			PublicKey publicKey = loadPublicKeyX509.getPublicKey();
			System.out.println("loadPublicKey : "+ publicKey);
			
			System.setProperty("javax.xml.soap.MessageFactory", "com.sun.xml.internal.messaging.saaj.soap.ver1_2.SOAPMessageFactory1_2Impl");
			System.setProperty("javax.xml.bind.JAXBContext", "com.sun.xml.internal.bind.v2.ContextFactory");
			
			SOAPMessage soapMsg = WS_Security_signature(inputFile, false);
			outputSOAPMessageToFile(soapMsg);
			/*ByteArrayOutputStream out = new ByteArrayOutputStream();
			soapMsg.writeTo(out);
			String strMsg = new String(out.toByteArray());
			System.out.println("+++++++++++++++++++++++++++++++++++");
			System.out.println(strMsg);
			System.out.println("+++++++++++++++++++++++++++++++++++");*/
			new PostaServis().callTheWebServiceFromFile(soapMsg);
			//System.out.println("Signature Succesfull. Verify the Signature");
      // boolean soapXmlWSSEDigitalSignatureValid = isSOAPXmlWSSEDigitalSignatureValid(outputFile, publicKey);
      // System.out.println("isSOAPXmlDigitalSignatureValid :"+soapXmlWSSEDigitalSignatureValid);
		} catch (Exception e) {
			e.printStackTrace();
		} 
    }
    
    public static void outputSOAPMessageToFile(SOAPMessage soapMessage) throws SOAPException, IOException {
        File outputFileNew = new File(outputFile);
        java.io.FileOutputStream fos = new java.io.FileOutputStream(outputFileNew);
        soapMessage.writeTo(fos);
        fos.close();
    }
    
    public static String toStringDocument(Document doc) throws TransformerException {
        StringWriter sw = new StringWriter();
        TransformerFactory tf = TransformerFactory.newInstance();
        Transformer transformer = tf.newTransformer();
        transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "no");
        transformer.setOutputProperty(OutputKeys.METHOD, "xml");
        transformer.setOutputProperty(OutputKeys.INDENT, "yes");
        transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");

        transformer.transform(new DOMSource(doc), new StreamResult(sw));
        return sw.toString();
    }
    public static String getFileString(String xmlFilePath) throws FileNotFoundException {
        File file = new File(xmlFilePath);
        //FileInputStream parseXMLStream = new FileInputStream(file.getAbsolutePath());
        
        Scanner scanner = new Scanner( file, "UTF-8" );
        String xmlContent = scanner.useDelimiter("\\A").next();
        scanner.close(); // Put this call in a finally block
        System.out.println("Str:"+xmlContent);
        return xmlContent;
    }
    public static Document getDocument(String xmlData, boolean isXMLData) throws Exception {
        DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
        dbFactory.setNamespaceAware(true);
        dbFactory.setIgnoringComments(true);
        DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
        Document doc;
        if (isXMLData) {
            InputSource ips = new org.xml.sax.InputSource(new StringReader(xmlData));
            doc = dBuilder.parse(ips);
        } else {
            doc = dBuilder.parse( new File(xmlData) );
        }
        return doc;
    }
 
    private void callTheWebServiceFromFile(SOAPMessage msg) throws IOException, SOAPException {
         ByteArrayOutputStream out = new ByteArrayOutputStream();
        msg.writeTo(out);
        
        // Set the soapPart Content with the stream source
        //soapPart.setContent(ss);
        SOAPPart soapPart = msg.getSOAPPart();
        
        // Create a webService connection
        SOAPConnectionFactory soapConnectionFactory = SOAPConnectionFactory.newInstance();
        SOAPConnection soapConnection = soapConnectionFactory.createConnection();

        // Invoke the webService.
        String soapEndpointUrl = "https://mirsal2gwytest.dubaitrade.ae/customsb2g/oilfields";
        SOAPMessage resp = soapConnection.call(msg, soapEndpointUrl);

        // Reading result
        resp.writeTo(System.out);

        //fis.close();
        soapConnection.close();
    }

    
    public static SOAPMessage WS_Security_signature(String inputFile, boolean isDataXML) throws Exception {
        SOAPMessage soapMsg;
        Document docBody;
        if (isDataXML) {
            System.out.println("Sample DATA xml - Create SOAP Message");
            MessageFactory messageFactory = MessageFactory.newInstance();
            SOAPMessage soapMessage = messageFactory.createMessage();
            soapMsg = soapMessage;
            
            String xmlContent = getFileString(inputFile);
            docBody = getDocument(xmlContent.trim(), true);
            System.out.println("Data Document: "+docBody.getDocumentElement());
        } else {
            System.out.println("SOAP XML with Envelope");
            
            Document doc = getDocument(inputFile, false); // SOAP MSG removing comment elements
            String docStr = toStringDocument(doc); // https://stackoverflow.com/a/2567443/5081877
            ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(docStr.getBytes());
            
            MimeHeaders mimeHeaders = new MimeHeaders();
            mimeHeaders.removeHeader("Content-Type");
            mimeHeaders.addHeader("Content-Type", "text/xml;charset=utf-8");
            mimeHeaders.addHeader("SOAPAction", "process");
            SOAPMessage message = MessageFactory.newInstance(SOAPConstants.SOAP_1_1_PROTOCOL).createMessage(mimeHeaders, byteArrayInputStream);
            soapMsg = message;
            
            docBody = soapMsg.getSOAPBody().extractContentAsDocument();
            System.out.println("SOAP DATA Document: "+docBody.getDocumentElement());
        }
        // A new SOAPMessage object contains: •SOAPPart object •SOAPEnvelope object •SOAPBody object •SOAPHeader object 
        SOAPPart soapPart = soapMsg.getSOAPPart();
        soapPart.setMimeHeader("Content-Type", "text/xml;charset=UTF-8");
        SOAPEnvelope soapEnv = soapPart.getEnvelope();
        SOAPHeader soapHeader = soapEnv.getHeader(); // soapMessage.getSOAPHeader();
        SOAPBody soapBody = soapEnv.getBody(); // soapMessage.getSOAPBody()
        
        soapBody.addDocument(docBody);
        soapBody.addAttribute(soapEnv.createName("Id", "wsu", WSU_NS), "Body");
        
        if (soapHeader == null) {
            soapHeader = soapEnv.addHeader();
            System.out.println("Provided SOAP XML does not contains any Header part. So creating it.");
        }
        // <wsse:Security> element adding to Header Part
        SOAPElement securityElement = soapHeader.addChildElement("Security", "wsse", WSSE_NS);
        securityElement.addNamespaceDeclaration("wsu", WSU_NS);

        String certEncodedID = "X509Token", timeStampID = "TS", signedBodyID = "Body";
        // (ii) Add Binary Security Token.
        // <wsse:BinarySecurityToken EncodingType="...#Base64Binary" ValueType="...#X509v3" wsu:Id="X509Token">The base64 encoded value of the ROS digital certificate.</wsse:BinarySecurityToken>
        SOAPElement binarySecurityToken = securityElement.addChildElement("BinarySecurityToken", "wsse");
        binarySecurityToken.setAttribute("ValueType", binarySecurityToken_Value);
        binarySecurityToken.setAttribute("EncodingType", binarySecurityToken_Encoding);
        binarySecurityToken.setAttribute("wsu:Id", certEncodedID);
            byte[] certByte = loadPublicKeyX509.getEncoded();
            String encodeToString = Base64.getEncoder().encodeToString(certByte);
        binarySecurityToken.addTextNode(encodeToString);
        
        //(iii) Add TimeStamp element - <wsu:Timestamp wsu:Id="TS">
        SOAPElement timestamp = securityElement.addChildElement("Timestamp", "wsu");
        timestamp.addAttribute(soapEnv.createName("Id", "wsu", WSU_NS), timeStampID);
            String DATE_TIME_PATTERN = "yyyy-MM-dd'T'HH:mm:ss.SSSX";
            DateTimeFormatter timeStampFormatter = DateTimeFormatter.ofPattern(DATE_TIME_PATTERN);
        timestamp.addChildElement("Created", "wsu").setValue(timeStampFormatter.format(ZonedDateTime.now().toInstant().atZone(ZoneId.of("UTC"))));
        timestamp.addChildElement("Expires", "wsu").setValue(timeStampFormatter.format(ZonedDateTime.now().plusSeconds(30).toInstant().atZone(ZoneId.of("UTC"))));

        // (iv) Add signature element
        // <wsse:Security> <ds:Signature> <ds:KeyInfo> <wsse:SecurityTokenReference>
        SOAPElement securityTokenReference = securityElement.addChildElement("SecurityTokenReference", "wsse");
        SOAPElement reference = securityTokenReference.addChildElement("Reference", "wsse");
        reference.setAttribute("URI", "#"+certEncodedID); // <wsse:BinarySecurityToken wsu:Id="X509Token"
        
        // <ds:SignedInfo>
        String providerName = System.getProperty("jsr105Provider", "org.jcp.xml.dsig.internal.dom.XMLDSigRI");
        XMLSignatureFactory xmlSignatureFactory = XMLSignatureFactory.getInstance("DOM", (java.security.Provider) Class.forName(providerName).newInstance());

        //Digest method - <ds:SignatureMethod Algorithm="http://www.w3.org/2000/09/xmldsig#rsa-sha1"/>
        javax.xml.crypto.dsig.DigestMethod digestMethod = xmlSignatureFactory.newDigestMethod(digestMethodAlog_SHA1, null);
        
        ArrayList<Transform> transformList = new ArrayList<Transform>();
        //Transform - <ds:Reference URI="#Body">
        Transform envTransform = xmlSignatureFactory.newTransform(transformAlog, (TransformParameterSpec) null);
        transformList.add(envTransform);
            //References <ds:Reference URI="#Body">
            ArrayList<Reference> refList = new ArrayList<Reference>();
                Reference refTS   = xmlSignatureFactory.newReference("#"+timeStampID,  digestMethod, transformList, null, null);
                Reference refBody = xmlSignatureFactory.newReference("#"+signedBodyID, digestMethod, transformList, null, null);
            refList.add(refBody);
            refList.add(refTS);

        javax.xml.crypto.dsig.CanonicalizationMethod cm = xmlSignatureFactory.newCanonicalizationMethod(canonicalizerAlog, (C14NMethodParameterSpec) null);

        javax.xml.crypto.dsig.SignatureMethod sm = xmlSignatureFactory.newSignatureMethod(signatureMethodAlog_SHA1, null);
        SignedInfo signedInfo = xmlSignatureFactory.newSignedInfo(cm, sm, refList);

        DOMSignContext signContext = new DOMSignContext(privateKey, securityElement);
        signContext.setDefaultNamespacePrefix("ds");
        signContext.putNamespacePrefix(DSIG_NS, "ds");
        signContext.putNamespacePrefix(WSU_NS, "wsu");

        signContext.setIdAttributeNS(soapBody, WSU_NS, "Id");
        signContext.setIdAttributeNS(timestamp, WSU_NS, "Id");

        KeyInfoFactory keyFactory = KeyInfoFactory.getInstance();
        DOMStructure domKeyInfo = new DOMStructure(securityTokenReference);
        javax.xml.crypto.dsig.keyinfo.KeyInfo keyInfo = keyFactory.newKeyInfo(java.util.Collections.singletonList(domKeyInfo));
        javax.xml.crypto.dsig.XMLSignature signature = xmlSignatureFactory.newXMLSignature(signedInfo, keyInfo);
        signContext.setBaseURI("");

        signature.sign(signContext);
        return soapMsg;
    }

}
