package com.iodsky.mysweldo.report;

import com.opencsv.CSVWriter;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.util.List;

@Component
public class CsvReportWriter implements ReportWriter {

    @Override
    public byte[] write(List<String> headers, List<List<String>> rows) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        baos.write(0xEF);
        baos.write(0xBB);
        baos.write(0xBF);

        try (OutputStreamWriter osw = new OutputStreamWriter(baos, StandardCharsets.UTF_8);
             CSVWriter writer = new CSVWriter(osw)) {
            writer.writeNext(headers.toArray(String[]::new));
            for (List<String> row : rows) {
                writer.writeNext(row.toArray(String[]::new));
            }
        } catch (IOException e) {
            throw new IllegalStateException("Failed to write CSV report", e);
        }
        return baos.toByteArray();
    }

}