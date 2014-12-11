/*
 * ImageRecognizer reads an image from ImageGenerator then
 * crawls each pixel to map shapes in the image
 */
package imageRecognizer;

import java.awt.Color;
import java.awt.Point;
import java.awt.Polygon;
import java.lang.management.GarbageCollectorMXBean;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.awt.geom.Area;
import java.awt.geom.Line2D;
import java.awt.geom.PathIterator;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.awt.geom.Line2D;

import javax.swing.JPanel;

public class ImageRecognizer {
	//variables
	JPanel panel;
	Color bgColor, shapeColor, newBGColor;
	//attempt iteration instead of recursion by going line-by line
	private int width, height, bgRGB, shapeRGB;
	private BufferedImage image;
	private ArrayList<Point> pointList;
	private ArrayList<Point> tempVertexList;
	private ArrayList<Point> vertexList;
	private Point center;
	private double radius;
	//constructor
	public ImageRecognizer(BufferedImage image){
		//initialize variables
		pointList = new ArrayList<Point>();
		vertexList = new ArrayList<Point>();
		//set variables
		this.image = image;
		bgColor = new Color(image.getRGB(0, 0)); //assume the top left pixel is the background since the shape never reaches the corner
		width = image.getWidth();
		height = image.getHeight();
		bgRGB = image.getRGB(0, 0); //assume top left corner is background since shapes cannot reach
		center = new Point(image.getWidth() / 2, image.getHeight() / 2);	//using the cheat method knowledge of IG making the cetner {width / 2, height / 2}
		//run methods
		map();							//get all the potential vertices and place into pointList
		radius = createRadius(center, pointList);
		vertexList = createVertices(pointList);	//get all vertices from center 
//		testArea();
		intersect();
	}	
	//return number of sides mapped by IR
	public int getNumberOfSidesMapped(){
		return vertexList.size();
	}
	//find the farthest point from center - that distance = radius
	private double createRadius(Point center, ArrayList<Point> pointList) {
		//check whether the center exists and there are vertices
		double createRadius = 0.0;
		if (center != null) {
			for(Point pt : pointList) {
				if (Math.sqrt(Math.pow(pt.getX() - center.getX(), 2) + Math.pow(pt.getY() - center.getY(), 2)) > createRadius){
					createRadius = Math.sqrt(Math.pow(pt.getX() - center.getX(), 2) + Math.pow(pt.getY() - center.getY(), 2));
				}
			}
			return createRadius;
		}
		else
			return createRadius = (Double) null;
	}
	/*
	 * map taking advantage of the two-dimension comvex shapes by approaching in a line from the left
	 * then approaching from the right to capture all the points outlining the shape and skipping the 
	 * inside of the shape
	 * 
	 */
	private void map(){
		/*
		 * get border of polygon by sending the first pixels with changing color to an arraylist 
		 */
		//top to bottom, left to right
		for (int y = 0; y < height; y++){				//rows by height value *This is the Y-Coordinate*
			for (int x = 0; x < width; x++){			//columns by width *This is the X-Coordinate*
				if (image.getRGB(x, y) != bgRGB){		//different from bg = shape
					pointList.add(new Point(x, y));		//add point to the list as a potential vertex
					break;								//found boundary, move to next row
				}
			}
		}
		//top to bottom, right to left
		for (int y = height - 1; y > -1; y--){			//rows by height value *This is the Y-Coordinate*
			for (int x = width - 1; x > -1; x--){		//columns by width *This is the X-Coordinate*
				if (image.getRGB(x, y) != bgRGB) {		//different than background = shape
					pointList.add(new Point(x, y)); 	//add point to the list as a potential vertex
					break;								//found boundary, move to next row
				}
			}
		}
	}
	
	public int getSideNumber(){
		return vertexList.size();
	}
	/*
	 * using the radial theory, iterate through all points placing the points farthest from the center in the 
	 * vertex list
	 */
	private ArrayList<Point> createVertices(ArrayList<Point> pointList){
		ArrayList<Point> createVertexList = new ArrayList<Point>();
		double  distance = 0.0;
		HashMap<Point, Double> distanceHash = new HashMap<Point, Double>();
		//maintain all the distances from center to iterate through after
		for (Point pt : pointList){
			distance = Math.sqrt(Math.pow(pt.getX() - center.getX(), 2) + Math.pow(pt.getY() - center.getY(), 2));
			distanceHash.put(pt, distance);
			
		}
		//iterate through distance to find radii and vertices
		for (Map.Entry<Point, Double> entry : distanceHash.entrySet()){
			if (entry.getValue() >= 0.9995 * radius) {	//percentage allows for fluctuation in precision
				createVertexList.add(entry.getKey());
			}
		}
		return createVertexList;
	}
	//test the area method
	private void testArea(){
		int[] xPoints = new int[pointList.size()];
		int[] yPoints = new int[pointList.size()];
		Polygon polygon = new Polygon();
		for (int point = 0; point < pointList.size(); point++){
			polygon.addPoint((int) pointList.get(point).getX(), (int) pointList.get(point).getY());

			xPoints[point] = (int) pointList.get(point).getX();
			yPoints[point] = (int) pointList.get(point).getY();
		}
		Area area = new Area(polygon); // The value is set elsewhere in the code    
		ArrayList<double[]> areaPoints = new ArrayList<double[]>();
		ArrayList<Line2D.Double> areaSegments = new ArrayList<Line2D.Double>();
		double[] coords = new double[6];
	
		for (PathIterator pi = area.getPathIterator(null); !pi.isDone(); pi.next()) {
		    // The type will be SEG_LINETO, SEG_MOVETO, or SEG_CLOSE
		    // Because the Area is composed of straight lines
		    int type = pi.currentSegment(coords);
		    // We record a double array of {segment type, x coord, y coord}
		    double[] pathIteratorCoords = {type, coords[0], coords[1]};
		    areaPoints.add(pathIteratorCoords);
		}
	
		double[] start = new double[3]; // To record where each polygon starts
	
		for (int i = 0; i < areaPoints.size(); i++) {
		    // If we're not on the last point, return a line from this point to the next
		    double[] currentElement = areaPoints.get(i);
	
		    // We need a default value in case we've reached the end of the ArrayList
		    double[] nextElement = {-1, -1, -1};
		    if (i < areaPoints.size() - 1) {
		        nextElement = areaPoints.get(i + 1);
		    }
	
		    // Make the lines
		    if (currentElement[0] == PathIterator.SEG_MOVETO) {
		        start = currentElement; // Record where the polygon started to close it later
		    } 
	
		    if (nextElement[0] == PathIterator.SEG_LINETO) {
		        areaSegments.add(
		                new Line2D.Double(
		                    currentElement[1], currentElement[2],
		                    nextElement[1], nextElement[2]
		                )
		            );
		    } else if (nextElement[0] == PathIterator.SEG_CLOSE) {
		        areaSegments.add(
		                new Line2D.Double(
		                    currentElement[1], currentElement[2],
		                    start[1], start[2]
		                )
		            );
		    }
		}
		
		// areaSegments now contains all the line segments
		System.out.println("segs: " + areaSegments.size());
	}
	/*
	 * Intersect uses an empty polygon (only colored boundary) then iterates
	 * through each point in the polygon to make rays from a point on the polygon to a
	 * recursive point rotated based on result that hones in on whether the point is a vertex.
	 * If it intersects once then the ray does not yet contain the vertex
	 * If it intersects zero times then the ray does not yet contain the vertex
	 * If it intersect 2+ times then it overlaps a side
	 * 	follow that side until it is a vertex
	 */
	public void intersect(){
		double maxTheta = (3600 * (3601 / 2)) - (11 * (12 / 2)) / 10;
		System.out.println("sum1: " + maxTheta);
		Line2D.Double testLine = new Line2D.Double(0.0, 0.0, 10.0, 10.0);
        //from: http://stackoverflow.com/questions/6339328/iterate-through-each-point-on-a-line-path-in-java
		List<Point2D> ary = new ArrayList<Point2D>();
        Line2D line = new Line2D.Double(0, 0, 10, 10);
        Point2D current;
//        for(Iterator<Point2D> iter = new LineIterator(line); iter.hasNext();) {
//            current =iter.next();
//            System.out.println(current);
//            ary.add(current);
//        }
//		Line2D line = new Line2D.Double();
		int[] xPoints = new int[pointList.size()];
		int[] yPoints = new int[pointList.size()];
		int maxRadius = (image.getWidth() > image.getHeight()) ? image.getWidth() : image.getHeight();
		Polygon polygon = new Polygon();
		Line2D currentSide = new Line2D.Double();
		int sides = 0;
		for (int point = 0; point < pointList.size(); point++){
			//at max, use beginning point
			if (point == pointList.size() - 1) interesectingRay(pointList.get(point), pointList.get(0), 0, maxRadius, 360, 360, 0); 
			else currentSide = 	interesectingRay(pointList.get(point), pointList.get(point + 1), 0, maxRadius, 360, 360, 0);	
			
			if (currentSide != null){
				System.out.println("found: " + currentSide.getP1() + ", " + currentSide.getP2()); //print vertices for testing
				point = pointList.indexOf(new Point((int) currentSide.getP2().getX(),(int) currentSide.getP2().getY())) - 1;
				point = (point > 1) ? point : pointList.size() - 1;
				currentSide = null;
				//set point = to the end point to skip points in the middle
				sides++;
			}
		}
		System.out.println(sides);
	}
	/*
	 * intersectiongRay recursively moves a line from a starting point on the polygon to see if that line will overlap the side of the
	 * polygon - if so, the line is a side and its end points are vertices 
	 * startPoint is the point to test whether it is a vertex 
	 * refPoint is the next counter-clockwise point used to determine the next angular rotation
	 * intersections is the number of intersections used to determine whether inside polygon, outside polygon, or on polygon
	 * radius is the length that will assure coverage of the entire image (greater of width/Height)
	 * previousTheta is the angle out of 360 used in the ray transformation will be used to calculate the next angle
	 * direction is whether to move counterclockwise (1) or clockwise (-1)
	 */
	public Line2D interesectingRay(Point startPt, Point refPoint, int intersections, int radius, double theta, double thetaDelta, int direction){
//		System.out.println(startPt + " & theta: " + theta);
		//calculate theta
		double previousTheta = theta;
		thetaDelta /= 2;
		//polygon is clockwise from last ray
		if (direction == 0) { //start case with 360 theta
			theta -= thetaDelta;	//subtract half the last theta;
		}
		else if (direction == -1) {
			theta += thetaDelta;	//add half the last theta
		}
		//polygon is counterclockwise from last ray
		else {
			theta -= thetaDelta;	//subtract half the last theta;
		} //we should not receive an else because that means the last ray should have overlapped the line and not thrown a direction number or recursed
		
		//a base case, theta change is so small we will never achieve match so return
		if (thetaDelta < 0.1) return null;
		
		Point2D endPoint = new Point2D.Double((startPt.getX() + (radius * Math.cos(Math.toRadians(theta)))),
				(startPt.getY() + (radius * Math.sin(Math.toRadians(theta)))));
		Line2D ray = new Line2D.Double((double) startPt.getX(), (double) startPt.getY(), endPoint.getX(), endPoint.getY());	//calculate and set new ray
		
//		direction = ray.relativeCCW(refPoint);	//reset the direction in case it recurses
		Point2D point;
		Line2D side = new Line2D.Double();
		//reset instersections
		intersections = 0;
		//starting with the first point, track the side to the next vertex
		for(Iterator<Point2D> iter = new LineIterator(ray); iter.hasNext();) {
			//get the next point on the Bresenham's line
			point = iter.next();
			//check whether the line is colored differently than the background (i.e. is the polygon)
			if (point.getX() >= 0 && point.getY() >= 0 && point.getX() < image.getWidth() && point.getY() < image.getHeight()) {	//ensure within image
				if (image.getRGB((int) point.getX(), (int) point.getY()) != bgRGB) {
					intersections++;
					side.setLine(startPt, point);
				}
			}
        }		
		//base case return if it is on the path or 
		if (intersections > 20){	//is a side
			return side;
		}
		else {
			return interesectingRay(startPt, refPoint, intersections, radius, theta, thetaDelta, ray.relativeCCW(refPoint));
		}
		//starts inside the polygon
		
//		vertices.add(new Point(center[0] + (int) (radius * Math.cos(Math.toRadians(theta))),
//				center[1] + (int) (radius * Math.sin(Math.toRadians(theta)))));
	}
}
