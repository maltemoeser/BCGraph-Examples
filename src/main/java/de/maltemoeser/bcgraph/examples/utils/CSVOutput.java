package de.maltemoeser.bcgraph.examples.utils;

import au.com.bytecode.opencsv.CSVWriter;

import java.io.FileWriter;
import java.io.IOException;
import java.util.List;

/**
 * Wrapper class that provides convenience functionality to write data to a csv file.
 */
public class CSVOutput {

    private CSVWriter csvWriter;

    private final char SEPARATOR = ';';

    public void initializeCSVWriter(String filename) {
        try {
            csvWriter = new CSVWriter(new FileWriter(filename), SEPARATOR, CSVWriter.NO_QUOTE_CHARACTER);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void closeCSVWriter() {
        try {
            csvWriter.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void appendToCSVFile(List<String[]> content) {
        content.forEach(this::appendToCSVFile);
    }

    public void appendToCSVFile(String[] content) {
        csvWriter.writeNext(content);
    }

    public void appendToCSVFile(String content) {
        csvWriter.writeNext(new String[] {content});
    }
}
