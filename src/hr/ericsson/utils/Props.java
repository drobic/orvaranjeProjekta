package hr.ericsson.utils;


import java.io.File;
import java.io.FileInputStream;

public class Props extends java.util.Properties {
	private static Props instance;

	private Props() {
		try {
			FileInputStream fis = new FileInputStream(new File("properties.txt"));
			load(fis);
		} catch (Exception e) {
			System.out.println("Puklo kod ucitavanja properties fajle");
		}
	}

	public static Props getInstance() {
		if (instance == null) {
			instance = new Props();
		}
		return instance;
	}
}