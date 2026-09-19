package com.iodsky.mysweldo.report;

import java.util.List;

public record ReportData(List<String> headers, List<List<String>> rows) {
}