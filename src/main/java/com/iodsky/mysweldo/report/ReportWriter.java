package com.iodsky.mysweldo.report;

import java.util.List;

public interface ReportWriter {

    byte[] write(List<String> headers, List<List<String>> rows);

}