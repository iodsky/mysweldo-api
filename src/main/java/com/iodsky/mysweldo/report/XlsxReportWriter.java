package com.iodsky.mysweldo.report;

import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;

@Component
public class XlsxReportWriter implements ReportWriter {

    @Override
    public byte[] write(List<String> headers, List<List<String>> rows) {
        try (Workbook workbook = new XSSFWorkbook();
             ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("Report");

            Row headerRow = sheet.createRow(0);
            for (int i = 0; i < headers.size(); i++) {
                headerRow.createCell(i).setCellValue(headers.get(i));
            }

            for (int r = 0; r < rows.size(); r++) {
                Row row = sheet.createRow(r + 1);
                List<String> cells = rows.get(r);
                for (int c = 0; c < cells.size(); c++) {
                    row.createCell(c).setCellValue(cells.get(c));
                }
            }

            workbook.write(baos);
            return baos.toByteArray();
        } catch (IOException e) {
            throw new IllegalStateException("Failed to write XLSX report", e);
        }
    }

}