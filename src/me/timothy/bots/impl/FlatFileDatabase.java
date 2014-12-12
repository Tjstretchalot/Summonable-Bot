package me.timothy.bots.impl;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import me.timothy.bots.Database;

/**
 * A basic flat-file database that uses 1 line in 
 * each file to save a fullname. It is not recommended
 * you use this if you plan on having a more sophisticated
 * database where SQL is more appropriate
 * 
 * @author Timothy
 */
public class FlatFileDatabase extends Database {
	private List<String> fullnames;
	
	/**
	 * Creates a flat file database
	 */
	public FlatFileDatabase() {
		fullnames = new ArrayList<>();
	}
	
	@Override
	public void addFullname(String id) {
		fullnames.add(id);
	}

	@Override
	public boolean containsFullname(String id) {
		return fullnames.contains(id);
	}

	/**
	 * Saves the database to the specified file
	 * @param file the file to save to
	 */
	public void save(File file) {
		try(BufferedWriter bw = new BufferedWriter(new FileWriter(file))) {
			for(String str : fullnames) {
				bw.append(str).append("\n");
			}
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}
	
	/**
	 * Loads the database from thet specified file
	 * @param file the file to load from
	 */
	public void load(File file) {
		try(BufferedReader br = new BufferedReader(new FileReader(file))) {
			String ln;
			
			while((ln = br.readLine()) != null) {
				addFullname(ln);
			}
		}catch(IOException e) {
			throw new RuntimeException(e);
		}
	}
}
