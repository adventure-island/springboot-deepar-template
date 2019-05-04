package com.tensorlab.ml;

import java.io.IOException;

import tech.tablesaw.api.Table;
import tech.tablesaw.plotly.api.TimeSeriesPlot;
import tech.tablesaw.plotly.components.Figure;
import tech.tablesaw.plotly.components.Page;

/**
 * @author JJ
 *
 */
public class PlotUtil {
    /**
     * Plots the time series data saved in a file as Javascript string
     * @param title The title of the graph
     * @param sourceFilePath The path of the file containing the data to plot
     * @return the Java script string representing the plotted graph
     * @throws IOException
     */
	public static String plotTimeSeriesToHtml(String title, String sourceFilePath) throws IOException {
		Table t = Table.read().csv(sourceFilePath);
		Figure figure = TimeSeriesPlot.create(title, t, "date", "value", "category");
        Page page = Page.pageBuilder(figure, "target").build();
        String output = page.asJavascript();
		return output;
	}
}
