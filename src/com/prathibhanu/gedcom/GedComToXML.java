/**
 *
 * GEDCOM file to XML converter
 * Copyright (c) 2011, Prathibhanu Kumar
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 * 		http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * 
 */

package com.prathibhanu.gedcom;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Stack;

import org.apache.commons.lang.StringEscapeUtils;

/**
 * 
 * @author prathibhanu
 * @since 19 Dec 2011
 */
public class GedComToXML {

	/**
	 * Converts a GEDCOM file into an XML file.
	 * 
	 * @param args
	 */
	public static void main(String[] args) {
		if(args.length != 2) {
			usage();
			return;
		}
		
		String inputFile = args[0];
		File input = new File(inputFile);
		
		if(!input.exists()) {
			System.out.println("Input file does not exists... exiting!");
			return;
		}
		
		if(input.isDirectory()) {
			System.out.println("Input file is a directory... exiting!");
			return;
		}
		
		String outputFile = args[1];
		File output = new File(outputFile);
		
		if(output.exists()) {
			System.out.println("Output file already exists... exiting!");
			return;
		}
		
		if(output.isDirectory()) {
			System.out.println("Output file is a directory... exiting!");
			return;
		}
		
		final long start = System.currentTimeMillis();
		convertToXML(input, output);
		final long end = System.currentTimeMillis();
		
		System.out.println("Total time taken: " + (end - start) + " ms.");
	}

	/**
	 * Open streams and convert the file.
	 * 
	 * @param input
	 * @param output
	 */
	private static void convertToXML(File input, File output) {
		BufferedReader reader = null;
		BufferedWriter writer = null;
		try {
			reader = new BufferedReader(new FileReader(input));
			writer = new BufferedWriter(new FileWriter(output));
			
			convertToXML(reader, writer);
		} catch(IOException e) {
			System.out.println("Unable to convert GEDCOM file to XML.");
			e.printStackTrace();
		} finally {
			if(reader != null) {
				try {
					reader.close();
				} catch (IOException e) {
					// do nothing
				}
			}
			
			if(writer != null) {
				try {
					writer.close();
				} catch (IOException e) {
					// do nothing
				}
			}
		}
	}

	/**
	 * Convert the file to XML form using minimal memory (via memoization)
	 * 
	 * @param reader
	 * @param writer
	 * @throws IOException
	 */
	private static void convertToXML(BufferedReader reader, BufferedWriter writer) throws IOException {
		// output the XML prologue
		writer.write("<?xml version=\"1.0\" encoding=\"utf-8\"?>\n");
		writer.write("<gedcom>\n");
		
		// start conversion
		int level = -1;
		
		Stack<String> tags = new Stack<String>();
		String lastValue = null;
		String currentTag = null;
		boolean first = true;
		
		String line = null;
		while((line = reader.readLine()) != null) {
			line = line.trim();
			
			// ignore blank lines
			if(line.length() == 0) {
				continue;
			}
			
			// read the level
			int index = line.indexOf(' ');
			String levelString = line.substring(0, index);
			final int currentLevel = Integer.parseInt(levelString);

			// read the tag
			line = line.substring(index + 1);
			line = line.trim();
			index = line.indexOf(' ');
			final String tagName;
			if(index != -1) {
				tagName = line.substring(0, index);
			} else {
				tagName = line;
			}
			
			// read the data
			final String data;
			if(index != -1) {
				data = line.substring(index + 1).trim();
			} else {
				data = null;
			}
			
			if(level > -1) {
				if(lastValue != null) {
					if(currentLevel > level) {
						writer.write(" value=\"");
						writer.write(StringEscapeUtils.escapeXml(lastValue));
						writer.write("\" >");
					} else {
						// currentLevel == level
						writer.write(">");
						writer.write(StringEscapeUtils.escapeXml(lastValue));
					}
				} else {
					writer.write(">");
				}
			}
			
			// if the level is less than the previous level, print the closing tag
			if(tags.size() > 0) {
				if(currentLevel == level) {
					writer.write("</");
					writer.write(tags.pop());
					writer.write(">");
				} else if(currentLevel < level) {
					boolean firstIndex = true;
					for(int indice = currentLevel; indice <= level; indice++) {
						if(!firstIndex) {
							writer.write("\n");
							spaceOut(writer, tags.size() - 1);
						}
						
						writer.write("</");
						writer.write(tags.pop());
						writer.write(">");
						
						firstIndex = false;
					}
				}
			}
			
			// set the values
			level = currentLevel;
			if(tagName.startsWith("@")) {
				// if tag or id starts with @, switch data and values
				// it indicates a reference to another field
				tags.push(data);
				lastValue = tagName;
				currentTag = data;
			} else {
				tags.push(tagName);
				lastValue = data;
				currentTag = tagName;
			}
			
			// write the node - but dont close it
			if(level > -1) {
				if(!first) {
					writer.write("\n");
				}

				spaceOut(writer, level);
			}
			
			writer.write("<");
			writer.append(currentTag);
			first = false;
		}
		
		// now check and close any open tags that we may have
		writer.write(">\n");
		spaceOut(writer, level);
		writer.write("</");
		writer.write(currentTag);
		writer.write(">");
		
		// close with gedcom
		writer.write("\n</gedcom>");
	}
	
	/**
	 * Space out the XML tag.
	 * 
	 * @param writer
	 * @param level
	 * @throws IOException
	 */
	private static void spaceOut(BufferedWriter writer, int level) throws IOException {
		for(int space = 0; space < level; space++) {
			writer.write("  ");
		}
	}

	/**
	 * Output the app usage
	 */
	private static void usage() {
		System.out.println("$ java -jar gedcom <input> <output>");
		System.out.println("");
		System.out.println("    input		the input GEDCOM file");
		System.out.println("    output		the filename of XML that needs to be written");
	}

}
