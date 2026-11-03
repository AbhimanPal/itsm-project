package com.itsm.itsm.report;

import java.util.List;

// The data for one chart: the names under the bars and the number for each bar.
// labels.get(0) belongs to values.get(0), and so on. Chart.js wants exactly these two lists.
public record ChartData(List<String> labels, List<Long> values) {
}
