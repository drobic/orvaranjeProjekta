package hr.ericsson.services.client;

import hr.ericsson.services.CustomHttpsConnector;
import hr.ericsson.services.DigitalnaArhivaZKServis;
import hr.ericsson.services.MPUOglasna;
import hr.ericsson.services.OIBServis;
import hr.ericsson.services.PostaServis;
import hr.ericsson.services.ZISservisi;
import hr.ericsson.utils.Props;

public class VanjskiServisiCaller {

	public static void main(String args[]) {
		System.setProperty("com.sun.xml.ws.transport.http.client.HttpTransportPipe.dump", "true");
		System.setProperty("com.sun.xml.internal.ws.transport.http.client.HttpTransportPipe.dump", "true");
		System.setProperty("com.sun.xml.ws.transport.http.HttpAdapter.dump", "true");
		System.setProperty("com.sun.xml.internal.ws.transport.http.HttpAdapter.dump", "true");
		String soapAction = "";
		try {
			boolean b=false;
			System.out.println("Starting calling services...");
			if(Boolean.parseBoolean(Props.getInstance().getProperty("posta.checkStatus"))) {
				PostaServis ps = new PostaServis();
				ps.callWS();
			}else if (Boolean.parseBoolean(Props.getInstance().getProperty("oib.checkStatus"))) {
				OIBServis os = new OIBServis();
				System.out.println("OIB servis...");
				os.checkAll();
			}else if (Boolean.parseBoolean(Props.getInstance().getProperty("oglasna.checkStatus"))) {
				MPUOglasna mpuo = new MPUOglasna();
				System.out.println("Oglasna servis...");
				mpuo.checkAll();
			}
			else if (Boolean.parseBoolean(Props.getInstance().getProperty("da.checkStatus"))) {
				DigitalnaArhivaZKServis da = new DigitalnaArhivaZKServis();
				System.out.println("ZK digitalna arhiva servis...");
				da.checkAll();
			}

		} catch (Exception e) {
			e.printStackTrace();
		}

	}
}
