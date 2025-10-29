package frontend;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Shape;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;

import javax.swing.JFrame;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.annotations.XYShapeAnnotation;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;
import org.jfree.util.ShapeUtilities;

import general.*;

public class Viewer
{
	private JFrame _frame;
	private Color[] _seriesColor = { Color.RED, Color.BLUE, Color.GREEN, Color.GRAY, Color.BLACK };
	
	public Viewer(Instance instance, Solution solution, String title)
	{
		createView(instance, solution, null, title);
	}

	public Viewer(Instance instance, Solution solution) 
	{
		createView(instance, solution, null, "");
	}

	public Viewer(Instance instance, Solution solution, ArrayList<Point> centroids) 
	{
		createView(instance, solution, centroids, "");
	}

	public Viewer(Instance instance, Solution solution, ArrayList<Point> centroids, String title) 
	{
		createView(instance, solution, centroids, title);
	}

	private void createView(Instance instance, Solution solution, ArrayList<Point> centroids, String title) 
	{
		XYSeriesCollection dataset = createDataset(instance, solution, centroids);
		JFreeChart xylineChart = ChartFactory.createXYLineChart("", "", "", dataset, PlotOrientation.VERTICAL, true, true, false);
		ChartPanel chartPanel = new ChartPanel(xylineChart);
		XYPlot plot = xylineChart.getXYPlot();
		XYLineAndShapeRenderer renderer = new XYLineAndShapeRenderer();
		
		for(int i=0; i<dataset.getSeriesCount(); ++i)
		{
			renderer.setSeriesLinesVisible(i, false);
			renderer.setSeriesShape(i, ShapeUtilities.createRegularCross(2, 2));
			renderer.setSeriesPaint(i, _seriesColor[i % _seriesColor.length]);
		}
		
		plot.setRenderer(renderer);
		plot.setBackgroundPaint(Color.WHITE);
		xylineChart.removeLegend();
		
		if( solution != null )
		{
			for(Cluster cluster: solution.getClusters()) if( cluster.size() > 0 )
			{
				Shape rectangle = new Rectangle2D.Double(cluster.getMin(0), cluster.getMin(1), cluster.getMax(0) - cluster.getMin(0), cluster.getMax(1) - cluster.getMin(1));
				XYShapeAnnotation shapeAnnotation = new XYShapeAnnotation(rectangle, new BasicStroke(0.5f), _seriesColor[cluster.getClassID() % _seriesColor.length]);
				plot.addAnnotation(shapeAnnotation);
			}
		}
		
		_frame = new JFrame();
		_frame.setTitle(title);
		_frame.setBounds(100, 100, 622, 640);
		_frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		_frame.getContentPane().add(chartPanel);
		_frame.setVisible(true);
	}
	
	private XYSeriesCollection createDataset(Instance instance, Solution solution, ArrayList<Point> centroids)
	{
		XYSeriesCollection dataset = new XYSeriesCollection();
		XYSeries[] series = new XYSeries[instance.getClasses()+1];
		
		for(int i=0; i<instance.getClasses()+1; ++i)
		{
			series[i] = new XYSeries("Class " + i);
			dataset.addSeries(series[i]);
		}
		
		if( solution != null )
		{
			for(Cluster cluster: solution.getClusters())
			for(Point point: cluster.asSet())
				series[cluster.getClassID()].add(point.get(0), point.get(1));
		}
		
		if( centroids != null )
		{
			for(Point point: centroids) if( point != null )
				series[instance.getClasses()].add(point.get(0), point.get(1));
		}
		
		return dataset;
	}
}
