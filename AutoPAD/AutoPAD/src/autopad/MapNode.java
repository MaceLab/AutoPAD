package autopad;

import java.awt.image.BufferedImage;
import java.awt.image.WritableRaster;
import java.io.File;
import java.io.IOException;

import javax.imageio.ImageIO;

public class MapNode {
	private int[] col = {255,255,255}; // the color of the node, in 256RGB format. By default: white (255,255,255)
	
	private boolean circle = false; // whether or not the node is a circle. Exclusive with rect, poly, and corner.
	private double circle_radius = 0; // radius of the circle
	private boolean precise = false; // whether or not this node is precise (affects circle + rectangle connections to reduce overlap)
	private double circle_inner_radius = -1; // inner radius of the circle (i.e., makes it into a disk)
	
	private boolean rect = false; // whether or not the node is a rectangle. Exclusive with circle and corner.
	private double rect_len = 0; // length of the rectangle (i.e., extending in the forward direction from its source)
	private double rect_wid = 0; // width of the rectangle (i.e., extending in the direction perpendicular from its source)
	private int rect_start_neighbor = -1; // which neighbor is this rectangle's source?
	
	private boolean corner=false; // whether or not the node is a corner. Exclusive with rect, poly, and circle.
	private double corner_base = 0; // length of the corner's base
	private double corner_height = 0; // misnomer; this is the length of the corner's second side, not necessarily its height
	private double corner_angle = 90; // the angle of the corner (i.e., the angle between the two sides specified by corner_base and corner_height)
	private double cornerspacingmod=0.96; // used to make corners overlap slightly with their sources by reducing their spacing by this factor
	private double corner_curveweight = 0; // the amount of curvature to the corner (e.g., 1.0 is a full curve)
	
	private boolean square=false; // like a circle, but without curvature
	private double square_len = 0;
	
	private double buffer_x = 0; // amount of buffer in the length direction (in the forward axis from its source)
	private double buffer_y = 0; // amount of buffer in the width direction (in the axis perpendicular to its source)
	private double extra_x = 0; // amount of extra whitespace in the length direction
	private double extra_y = 0; // amount of extra whitespace in the width direction
	
	private double perp_space=0; // perpendicular space to be used with the corner functionality so that the 
	// corner pieces stop placing things in their center but rather at the center of the connection to the
	// next piece (i.e. on the leading (not inv) edge)
	// positive/negative controls direction here
	
	private boolean cut = false; // whether or not this node's outline should be included in cut maps
	
	private int neighbor_max = 64; // arbitrary: maximum number of nodes that can be sourced from this one
	private MapNode[] neighbors = new MapNode[neighbor_max]; // list of connected nodes (neighbors)
	private double[] neighbor_angles = new double[neighbor_max]; // angle at which each neighbor is connected
	private double[] neighbor_distances = new double[neighbor_max]; // distance from each neighbor
	private int[] neighbor_zs = new int[neighbor_max]; // which layer each neighbor is on
	private int neighbor_cur = 0; 
	
	private int placeid=-1; // if this node has a place assigned to it, what is it? placeids are used to locate nodes in the script
	
	private double fill_x=0; // x position
	private double fill_y=0; // y position
	private int fill_z=0; // what layer is this node on?
	private boolean got_fill_xy=false; // whether or not this node's x,y coordinate has been finalized
	
	private boolean polygon = false; // is this node a polygon? exclusive from circle, rect, corner
	private double[] polygon_x = new double[1]; // x coords for vertices
	private double[] polygon_y = new double[1]; // y coords for vertices
	private int polygon_vs = 0; // how many vertices
	private int[] polygon_ctype = new int[1]; // connector type between the vertices
	// ex. ctype 0 is between 0 and 1
	// 0 = straight line
	// 1 = curve up
	// 2 = curve down
	private double[] polygon_b = new double[1]; // buffer space at each vertex
	private double[] polygon_extra = new double[1]; // extra whitespace at each vertex
	// polygon info: goes from each vertex to the next in order listed, loops back to first at end
	// interior will be filled in. 
	private double polygon_crotx=0; // center of rotation's x position
	private double polygon_croty=0; // center of rotation's y position ( relative to center of this node )
	private boolean polygon_filled=true; // whether or not the polygon is an outline or filled in
	
	private int[][] polygon_nearpoints = new int[1][4]; // 
	private double[] polygon_cweight = new double[1]; // curve weight for each vertex connection
	
	private double polygon_endx=0; // extra spacing for the polygon
	private boolean polygon_hasendpoint=false; // whether to use said spacing

	private String text=""; // this node's text
	private String fontname = "Verdana"; // font to use
	private double fontsize=1; // font scale (not in pt., this is how large the font is in the pixel scale)
	private boolean textspace=true; // whether or not to give the text automatic spacing based on its content
	
	private int nodeid=0; // absolute nodeid of this node
	
	private boolean[][] testmap; 
	
	private boolean first_vex_map=true;
	

	public MapNode(int nodevalue, int zval){
		nodeid=nodevalue; 
		fill_z=zval;
	}
	
	public void rescale(double rs){
		// rescales all dimensions by the rs value
		// make sure to add dimensions here as you add new variables
		circle_radius*=rs;
		circle_inner_radius*=rs;
		rect_len*=rs;
		rect_wid*=rs;
		corner_base*=rs;
		corner_height*=rs;
		square_len*=rs;
		buffer_x*=rs;
		buffer_y*=rs;
		extra_x*=rs;
		extra_y*=rs;
		perp_space*=rs;
		if(polygon){
			int i=0;
			while(i<polygon_vs){
				polygon_x[i]*=rs;
				polygon_y[i]*=rs;
				polygon_b[i]*=rs;
				polygon_extra[i]*=rs;
				i++;
			}
			polygon_crotx*=rs;
			polygon_croty*=rs;
			polygon_endx*=rs;
		}
		fontsize*=rs;
		if(!(neighbor_distances.length<=neighbor_cur-1 || neighbor_cur-1<0)){
			neighbor_distances[neighbor_cur-1]*=rs;
			neighbors[neighbor_cur-1].setDistanceByNode(this,neighbor_distances[neighbor_cur-1]);
		}
		
		
	}
	
	
	public int getFillZ(){
		return fill_z;
	}
	public void setFillZ(int inz){
		fill_z=inz;
	}
	public int getNodeID(){
		return nodeid;
	}
	public void setFillXY(double x,double y){
		fill_x=x;
		fill_y=y;
		got_fill_xy=true;
	}
	public boolean isFillXY(){
		return got_fill_xy;
	}
	public double getFillX(){
		return fill_x;
	}
	public double getFillY(){
		return fill_y;
	}
	
	public double getPerpSpace(){
		return perp_space;
	}
	public void setPerpSpace(double inps){
		perp_space += inps; // note this function is additive in case another function such as setCorner applies some perpSpace
	}
	
	public void setPlaceID(int a){
		placeid=a;
	}
	public int getPlaceID(){
		if(placeid==-1){
			return nodeid; 
		}
		return placeid;
	}
	
	public void setText(String intext){
		text=intext;
		rect_start_neighbor=neighbor_cur-1; // required in order to make sure the text has an angle to be drawn at
	}
	public String getText(){
		return text;
	}
	public void setFontName(String infn){
		fontname=infn;
	}
	public String getFontName(){
		return fontname;
	}
	public void setFontSize(double ins){
		fontsize=ins; // scaled by pixconv during post
	}
	public double getFontSize(){
		return fontsize;
	}
	public void setTextSpace(boolean inb){
		textspace=inb;
	}
	public boolean getTextSpace(){
		return textspace;
	}
	
	// getValueFromString(): renders all of this node's data accessible via script commands
	// takes the name of a variable and returns its value
	public double getValueFromString(String textid){
		System.out.println(" TEXTID " + textid);
		if(textid.equalsIgnoreCase("COLOR_RED")){
			return (double)(col[0]);
		}
		if(textid.equalsIgnoreCase("COLOR_GREEN")){
			return (double)(col[1]);
		}
		if(textid.equalsIgnoreCase("COLOR_BLUE")){
			return (double)(col[2]);
		}
		if(textid.equalsIgnoreCase("CORNER")){
			if(corner){return 1;}else{return 0;}
		}
		if(textid.equalsIgnoreCase("CORNER_BASE")){
			return corner_base;
		}
		if(textid.equalsIgnoreCase("CORNER_HEIGHT")){
			return corner_height;
		}
		if(textid.equalsIgnoreCase("CORNER_ANGLE")){
			return corner_angle;
		}
		if(textid.equalsIgnoreCase("CORNER_CURVE")){
			return corner_curveweight;
		}
		if(textid.equalsIgnoreCase("CIRCLE")){
			if(circle){return 1;}else{return 0;}
		}
		if(textid.equalsIgnoreCase("CIRCLE_RADIUS") || textid.equalsIgnoreCase("RADIUS")){
			return circle_radius;
		}
		if(textid.equalsIgnoreCase("CIRCLE_INNER_RADIUS") || textid.equalsIgnoreCase("INNER_RADIUS")){
			return circle_inner_radius;
		}
		if(textid.equalsIgnoreCase("PRECISE")){
			if(precise){return 1;}else{return 0;}
		}
		if(textid.equalsIgnoreCase("RECT")){
			if(rect){return 1;}else{return 0;}
		}
		if(textid.equalsIgnoreCase("RECT_LEN") || textid.equalsIgnoreCase("RECT_LENGTH")){
			return rect_len;
		}
		if(textid.equalsIgnoreCase("RECT_WID") || textid.equalsIgnoreCase("RECT_WIDTH") ){
			return rect_wid;
		}
		if(textid.equalsIgnoreCase("RECT_START")){
			return (double)(rect_start_neighbor);
		}
		if(textid.equalsIgnoreCase("PERP_SPACE")){
			return perp_space;
		}
		if(textid.equalsIgnoreCase("POLY") || textid.equalsIgnoreCase("POLYGON")){
			if(polygon){return 1;}else{return 0;}
		}
		if(textid.equalsIgnoreCase("POLY_ENDPOINT") || textid.equalsIgnoreCase("POLYGON_ENDPOINT")){
			if(polygon_hasendpoint){return 1;}else{return 0;}
		}
		if(textid.equalsIgnoreCase("POLY_ENDPOINTX") || textid.equalsIgnoreCase("POLYGON_ENDPOINTX")
				|| textid.equalsIgnoreCase("POLY_ENDX") || textid.equalsIgnoreCase("POLYGON_ENDX")
				|| textid.equalsIgnoreCase("POLY_ENDPOINTLEN") || textid.equalsIgnoreCase("POLYGON_ENDPOINTLEN")
				|| textid.equalsIgnoreCase("POLY_ENDLEN") || textid.equalsIgnoreCase("POLYGON_ENDLEN")){
			return getPolygonEndpointX();
		}
		if(textid.equalsIgnoreCase("POLY_LEN") || textid.equalsIgnoreCase("POLYGON_LEN")){
			return getPolygonLen();
		}
		if(textid.equalsIgnoreCase("POLY_WID") || textid.equalsIgnoreCase("POLYGON_WID")){
			return getPolygonWid();
		}
		if(textid.equalsIgnoreCase("POLY_VS") || textid.equalsIgnoreCase("POLYGON_VS") || textid.equalsIgnoreCase("POLYGON_VERTS")
				|| textid.equalsIgnoreCase("POLY_VERTS") || textid.equalsIgnoreCase("POLY_VERTICES") || 
				textid.equalsIgnoreCase("POLYGON_VERTICES")){
			return polygon_vs;
		}
		if(textid.equalsIgnoreCase("BUFFER_X") || textid.equalsIgnoreCase("BUFFER_LEN")){
			return buffer_x;
		}
		if(textid.equalsIgnoreCase("BUFFER_Y") || textid.equalsIgnoreCase("BUFFER_WID")){
			return buffer_y;
		}
		if(textid.equalsIgnoreCase("EXTRA_X") || textid.equalsIgnoreCase("EXTRA_LEN")){
			return extra_x;
		}
		if(textid.equalsIgnoreCase("EXTRA_Y") || textid.equalsIgnoreCase("EXTRA_WID")){
			return extra_y;
		}
		if(textid.equalsIgnoreCase("Z") || textid.equalsIgnoreCase("LAYER")){
			return fill_z;
		}
		if(textid.equalsIgnoreCase("CUT")){
			if(cut){return 1;}else{return 0;}
		}
		if(textid.equalsIgnoreCase("NEIGHBORS")){
			return (double)(neighbor_cur);
		}
		if(textid.equalsIgnoreCase("STARTANGLE") || textid.equalsIgnoreCase("ANGLE") || textid.equalsIgnoreCase("SANGLE")){
			return (getNeighborAngle(0));
		}
		if(textid.length()>9){
			if(textid.substring(0,9).equalsIgnoreCase("NEIGHBOR(")){
				// ex NEIGHBOR(RECT_LEN)_1 -> neighbor[1]'s rect len
				// NEIGHBOR(NEIGHBOR(RECT_LEN)_1)_1 -> neighbor[1]'s neighbor[1]'s rect len
				int[][] regout = Interpreter.regex(textid, "\\(.+\\)", 1);
				String result = textid.substring(regout[0][0]+1,regout[0][1]-1);
				regout = Interpreter.regex(textid, "[\\d]+", 30);
				int last = 0;
				int i=0;
				while(i<30){
					if(regout[i][0]!=-1){
						last=i;
					}
					i++;
				}
				int numid=0;
				try{
					numid = Integer.parseInt(textid.substring(regout[last][0],regout[last][1]));
				}catch(Exception e){return 0;}
				return neighbors[numid].getValueFromString(result);
			}
			if(textid.substring(0,9).equalsIgnoreCase("NEIGHBOR_")){
				int numid=0;
				try{
					numid = Integer.parseInt(textid.substring(9));
				}catch(Exception e){return 0;}
				return (double)(neighbors[numid].getPlaceID());
			}
		}
		if(textid.length()>15){
			if(textid.substring(0,15).equalsIgnoreCase("NEIGHBOR_ANGLE_")){
				int numid=0;
				try{
					numid = Integer.parseInt(textid.substring(15));
				}catch(Exception e){return 0;}
				return (double)(neighbor_angles[numid]);
			}
		}
		if(textid.length()>11){
			if(textid.substring(0,11).equalsIgnoreCase("NEIGHBOR_Z_")){
				int numid=0;
				try{
					numid = Integer.parseInt(textid.substring(11));
				}catch(Exception e){return 0;}
				return (double)(neighbor_zs[numid]);
			}
		}
		if(textid.length()>15){
			if(textid.substring(0,15).equalsIgnoreCase("NEIGHBOR_LAYER_")){
				int numid=0;
				try{
					numid = Integer.parseInt(textid.substring(11));
				}catch(Exception e){return 0;}
				return (double)(neighbor_zs[numid]);
			}
		}
		if(textid.length()>14){
			if(textid.substring(0,14).equalsIgnoreCase("NEIGHBOR_DIST_")){
				int numid=0;
				try{
					numid = Integer.parseInt(textid.substring(14));
				}catch(Exception e){return 0;}
				return (double)(neighbor_distances[numid]);
			}
		}
		
		return -1;
	}
	// setNeighborMax(): used in the rare occasion that the maximum number of neighbors needs to be increased 
	public void setNeighborMax(int max){
		MapNode[] newneighbors = new MapNode[max];
		double[] newneighbor_angles = new double[max];
		double[] newneighbor_distances = new double[max];
		int[] newneighbor_zs = new int[max];
		int i=0;
		while(i<neighbor_max){
			newneighbors[i]=neighbors[i];
			newneighbor_angles[i]=neighbor_angles[i];
			newneighbor_distances[i]=neighbor_distances[i];
			newneighbor_zs[i] = neighbor_zs[i];
			i++;
		}
		neighbors=newneighbors;
		neighbor_angles=newneighbor_angles;
		neighbor_distances=newneighbor_distances;
		neighbor_zs=newneighbor_zs;
		neighbor_max=max;
	}
	// setNeighbor(): used to add a neighbor to this node at some angle and distance
	public void setNeighbor(MapNode ne, double angle, double distance, int z){
		neighbors[neighbor_cur]=ne;
		neighbor_angles[neighbor_cur]=angle;
		neighbor_distances[neighbor_cur]=distance;
		neighbor_zs[neighbor_cur]=z;
		neighbor_cur++;
	}
	// setLastNeighborDistance(): used to change how far this node is from its last added neighbor
	public void setLastNeighborDistance(double distance){
		if(neighbor_distances.length<=neighbor_cur-1 || neighbor_cur-1<0){
			return;
		}
		neighbor_distances[neighbor_cur-1]=distance;
		neighbors[neighbor_cur-1].setDistanceByNode(this,distance);
	}
	// setDistance(): used to change how far this node is from any given neighbor
	public void setDistance(int i, double distance){
		neighbor_distances[i]=distance;
	}
	// setDistanceByNode(): same as above, but instead of the neighbor's neighbor id, takes in the neighbor's MapNode object to identify it
	public void setDistanceByNode(MapNode in_node, double distance){
		int i=0;
		while(i<neighbor_cur){
			if(in_node.equals(neighbors[i])){
				neighbor_distances[i]=distance;
				break;
			}
			i++;
		}
	}
	public MapNode getNeighbor(int i){
		return neighbors[i];
	}
	public int getNeighbors(){
		return neighbor_cur;
	}
	// getNeighborID(): from a given MapNode, returns its neighbor id
	public int getNeighborID(MapNode ne){
		int i=0;
		while(i<neighbor_cur){
			if(neighbors[i].equals(ne)){
				return i;
			}
			i++;
		}
		return -1;
	}
	public double getNeighborAngle(int i){
		return neighbor_angles[i];
	}
	public double getNeighborDistance(int i){
		return neighbor_distances[i];
	}
	public int getNeighborZ(int i){
		return neighbor_zs[i];
	}
	// hasNeighborAtAngle(): iterates over the neighbors list and returns true if any neighbor exists at the given angle
	public boolean hasNeighborAtAngle(double angle){
		int i=0;
		while(i<neighbor_cur){
			if(neighbor_angles[i]==angle && neighbor_zs[i]==fill_z){
				return true;
			}
			i++;
		}
		return false;
	}
	// getNeighborAtAngle(): same as above, except returns the MapNode object of the given neighbor instead of true/false
	public MapNode getNeighborAtAngle(double angle){
		int i=0;
		while(i<neighbor_cur){
			if(neighbor_angles[i]==angle && neighbor_zs[i]==fill_z){
				return neighbors[i];
			}
			i++;
		}
		return this;
	}
	// hasNeighborAtZ(): iterates over neighbors list, returns true if any neighbor exists at given Z level
	public boolean hasNeighborAtZ(int z){
		int i=0;
		while(i<neighbor_cur){
			if(neighbor_zs[i]==z){
				return true;
			}
			i++;
		}
		return false;
	}
	// getNeighborAtZ(): see above, returns MapNode instead of true/false
	public MapNode getNeighborAtZ(int z){
		int i=0;
		while(i<neighbor_cur){
			if(neighbor_zs[i]==z){
				return neighbors[i];
			}
			i++;
		}
		return this;
	}
	// isNeighbor(): given a MapNode, returns true if that node exists anywhere in the neighbors list
	public boolean isNeighbor(MapNode ne){
		int i=0;
		while(i<neighbor_cur){
			if(neighbors[i].equals(ne)){
				return true;
			}
			i++;
		}
		return false;
	}
	// getRealDistanceToNeighbor(): neighbor_distance is only the desired spacing; this takes into account various other factors such as precise
	public double getRealDistanceToNeighbor(int i){
		double dist = getNeighborDistance(i);
		MapNode snode = getNeighbor(i);
		
		// special care needs to be taken with this function that it works both ways:
		// i.e. the distance from A->B is the same as that from B->A
		
		boolean other_rect_start=false;
		if(snode.getRectStartNeighbor() != -1){
			if(snode.getNeighbor(snode.getRectStartNeighbor()).equals(this)){
				other_rect_start=true;
			}
		}
		if(i==rect_start_neighbor){
			dist+=rect_len;
			if(isPolygon()){
				dist += getPolygonLen();
				// add the whole length of the polygon to the distance
			}
			if(isCorner()){
				dist += getRealCornerHeight()/2;// * Math.sin(corner_angle);
				dist*=cornerspacingmod;
			}
			
			if(snode.isPrecise() || precise){
				if(snode.isCircle()){
					double swid = getRectWid();
					if(isPolygon()){
						swid=getPolygonStartWid();
					}
					dist+=/*snode.getCircleRadius()-*/getDeepnessOfCircleWidth(snode.getCircleRadius(), swid);
				}
			}
		}else if(other_rect_start){
			dist+=snode.getRectLen();
			if(snode.isPolygon()){
				dist += snode.getPolygonLen();
			}
			if(snode.isCorner()){
				// needs to change to depend on the endangle
				
				dist += snode.getRealCornerHeight()/2;// * Math.sin(snode.getCornerAngle());
				dist*=cornerspacingmod;
			}
			if(snode.isPrecise() || precise){
				if(isCircle()){
					double swid = snode.getRectWid();
					if(snode.isPolygon()){
						swid=snode.getPolygonStartWid();
					}
					dist+=/*getCircleRadius()-*/getDeepnessOfCircleWidth(getCircleRadius(), swid);
				}
			}
		}
		boolean other_start=false;
		if(snode.getNeighbor(0).equals(this)){
			other_start=true;
		}
		if((i==0 || other_start) && !(i==rect_start_neighbor || other_rect_start)){
			// if your 1st neighbor is this one
			if((precise && i==0) || (other_start && snode.isPrecise())){
				if(isCircle()){
					double swid = snode.getRectWid();
					if(snode.isPolygon()){
						swid=snode.getPolygonStartWid();
					}
					dist+=/*getCircleRadius()-*/getDeepnessOfCircleWidth(getCircleRadius(), swid);
				}else if(snode.isCircle()){
					double swid = getRectWid();
					if(isPolygon()){
						swid=getPolygonStartWid();
					}
					dist+=/*snode.getCircleRadius()-*/getDeepnessOfCircleWidth(snode.getCircleRadius(), swid);
				}
			}
		}
		return dist;
	}
	
	// getRealCornerHeight(): corner_height specified by script is only the second side length, which is only the corner's height if the
	// corner's angle is 90deg. This finds the actual height of the corner.
	public double getRealCornerHeight(){
		double endangle = Math.toRadians(Math.abs(corner_angle));
		double cosendangle = Math.cos(endangle);
		double sinendangle = Math.sin(endangle);
		if(Math.abs(cosendangle)<0.01){ // this clamping is necessary because, for instance, math likes to return
			cosendangle=0;				 // "1.22x10^-16" instead of "0" for angle = 180deg = pi
		}							 // which leads to slight, unwanted rotations at large distances from the pivot
		if(Math.abs(sinendangle)<0.01){
			sinendangle=0;
		}
		if(Math.abs(cosendangle)>0.99){
			cosendangle=cosendangle/Math.abs(cosendangle); // the sign function would also work here
		}
		if(Math.abs(sinendangle)>0.99){
			sinendangle=sinendangle/Math.abs(sinendangle);
		}	
		double len=corner_height;
		double wid=corner_base-1; // empirical adjustment
		
		double slope = 99999; // default case is endangle = 90, slope would be infinite
		double invslope = -((double)(len)/(double)(wid));
		double vertx=0;
		double verty=len;
		if(cosendangle!=0){ // if the angle is 90, don't bother with this (esp. don't divide by 0)
			slope=sinendangle/cosendangle;
			vertx = (Math.sqrt(len*len/(1+slope*slope)));
			verty = (slope*vertx);
			if(wid-vertx!=0){
				invslope = -(verty/(wid-vertx));
			}else{
				invslope=-9999;
			}
		}
		return verty;
	}
	// getCornerPerpSpace(): determines how much perpendicular space to give the corner piece. New nodes off a corner start at the middle of
	// the second side as opposed to in the center.
	public double getCornerPerpSpace(){
		double endangle = Math.toRadians(Math.abs(corner_angle));
		double cosendangle = Math.cos(endangle);
		double sinendangle = Math.sin(endangle);
		if(Math.abs(cosendangle)<0.01){ // this clamping is necessary because, for instance, math likes to return
			cosendangle=0;				 // "1.22x10^-16" instead of "0" for angle = 180deg = pi
		}							 // which leads to slight, unwanted rotations at large distances from the pivot
		if(Math.abs(sinendangle)<0.01){
			sinendangle=0;
		}
		if(Math.abs(cosendangle)>0.99){
			cosendangle=cosendangle/Math.abs(cosendangle); // the sign function would also work here
		}
		if(Math.abs(sinendangle)>0.99){
			sinendangle=sinendangle/Math.abs(sinendangle);
		}	
		double len=corner_height;
		double wid=corner_base; // empirical adjustment
		
		double slope = 99999; // default case is endangle = 90, slope would be infinite
		double invslope = -((double)(len)/(double)(wid));
		double vertx=0;
		double verty=len;
		
		if(cosendangle!=0){ // if the angle is 90, don't bother with this (esp. don't divide by 0)
			slope=sinendangle/cosendangle;
			vertx = (Math.sqrt(len*len/(1+slope*slope)));
			verty = (slope*vertx);
			if(wid-vertx!=0){
				invslope = -(verty/(wid-vertx));
			}else{
				invslope = -9999;
			}
		}
		
		// something about this is reversed
		// the distance is larger when the angle is smaller but that's when it should be closer to the core
		// what's the deal?
		double cenx  = wid/2;
		double midx  = (wid-vertx)/2;
		double diffx = midx;//cenx;//+midx;//cenx-midx;
		
		System.out.println(" CENX " + cenx + " MIDX " + midx + " DIFFX " + diffx);
		if(corner_angle<0){
			diffx=-diffx;
		}
		diffx*=cornerspacingmod; // goes too far, need to pull it back slightly so the shapes overlay
		return -diffx;
	}
	// getDeepnessOfCircleWidth(): used to determine precise spacing
	public double getDeepnessOfCircleWidth(double rad, double wid){
		double widint = rad*rad - ((wid/2)*(wid/2));
		// finds the chord in the circle of radius rad that has a given width, wid
		
		if(widint<0){
			return 0;
		}
		widint = Math.sqrt(widint);
		return /*rad -*/widint;
	}
	public void setColor(int c1, int c2, int c3){
		col[0]=c1; col[1]=c2; col[2]=c3;
	}
	public int[] getColor(){
		return col;
	}
	public void setCorner(double base, double height, double angle){
		corner=true;
		corner_base=base;
		corner_height=height;
		corner_angle=angle;
		rect_start_neighbor=neighbor_cur-1;
		// need to set perpspace automatically here
		perp_space += getCornerPerpSpace();
	}
	public double getCornerAngle(){
		return corner_angle;
	}
	public double getCornerBase(){
		return corner_base;
	}
	public double getCornerHeight(){
		return corner_height;
	}
	public void setCornerCurveweight(double incw){
		corner_curveweight=incw;
	}
	public double getCornerCurveweight(){
		return corner_curveweight;
	}
	public boolean isCorner(){
		return corner;
	}
	public void setCircle(double radius){
		circle=true;
		circle_radius=radius;
	}
	public double getCircleRadius(){
		return circle_radius;
	}
	public void setCircleInner(double inner){
		circle_inner_radius=inner;
	}
	public double getCircleInner(){
		return circle_inner_radius;
	}
	public boolean isCircle(){
		return circle;
	}
	public boolean isRect(){
		return rect;
	}
	public void setRect(double len, double wid){
		// temporarily disabled to test polygon rectangles
		rect=true;
		rect_len=len;
		rect_wid=wid;
		// temp code:
		/*polygon=true;
		addPolygonVertex(0,wid/2,0,0,0);
		addPolygonVertex(len,wid/2,0,0,0);
		addPolygonVertex(len,-wid/2,0,0,0);
		addPolygonVertex(0,-wid/2,0,0,0);*/
		// end
		rect_start_neighbor=neighbor_cur-1;
	}
	public int getRectStartNeighbor(){
		return rect_start_neighbor;
	}
	public double getRectLen(){ return rect_len;}
	public double getRectWid(){ return rect_wid;}
	public void setSpace(double sp){
		setLastNeighborDistance(sp);
	}
	public void setBuffer(double x, double y){
		buffer_x=x;
		buffer_y=y;
	}
	public double getBufferX(){ return buffer_x;}
	public double getBufferY(){ return buffer_y;}
	public double getExtraX(){ return extra_x;}
	public double getExtraY(){ return extra_y;}
	public void setExtra(double len, double wid){
		extra_x=len; // extra only applies to whatever element 'fill' is called on
		extra_y=wid;
	}
	public void setCut(){
		cut=true;
	}
	public boolean getCut(){
		return cut;
	}
	public void setPrecise(){
		precise=true;
	}
	public boolean isPrecise(){
		return precise;
	}
	
	public void setPolygon(){
		polygon=true;
		rect_start_neighbor=neighbor_cur-1;
	}
	public boolean isPolygon(){
		return polygon;
	}
	public void setPolygonEndpoint(double inx){
		polygon_hasendpoint=true;
		polygon_endx=inx;
	}
	public void removePolygonEndpoint(){
		polygon_hasendpoint=false;
	}
	public boolean hasPolygonEndpoint(){
		return polygon_hasendpoint;
	}
	public double getPolygonEndpointX(){
		return polygon_endx;
	}
	public void addPolygonVertex(double x, double y, double buffer, int ctype, double extra){
		polygon_vs++;
		double[] new_pvx = new double[polygon_vs]; // need to resize the polygon lists
		double[] new_pvy = new double[polygon_vs]; // ... all of them
		double[] new_pvb = new double[polygon_vs];
		double[] new_pvex = new double[polygon_vs];
		double[] new_pcw = new double[polygon_vs];
		int[][] new_pvnp = new int[polygon_vs][4];
		int[] new_pvct = new int[polygon_vs];
		int i=0;
		while(i<polygon_vs-1){
			new_pvx[i]=polygon_x[i];
			new_pvy[i]=polygon_y[i];
			new_pvb[i]=polygon_b[i];
			new_pvex[i]=polygon_extra[i];
			new_pvnp[i][0]=polygon_nearpoints[i][0];
			new_pvnp[i][1]=polygon_nearpoints[i][1];
			new_pvnp[i][2]=polygon_nearpoints[i][2];
			new_pvnp[i][3]=polygon_nearpoints[i][3];
			new_pvct[i] = polygon_ctype[i];
			new_pcw[i] = polygon_cweight[i];
			i++;
		}
		new_pvx[polygon_vs-1]=x;
		new_pvy[polygon_vs-1]=y;
		new_pvb[polygon_vs-1]=buffer;
		new_pvex[polygon_vs-1]=extra;
		new_pvct[polygon_vs-1]=ctype;
		new_pcw[polygon_vs-1]=1;
		
		polygon_x=new_pvx;
		polygon_y=new_pvy;
		polygon_b=new_pvb;
		polygon_extra=new_pvex;
		polygon_nearpoints=new_pvnp;
		polygon_ctype=new_pvct;
		polygon_cweight=new_pcw;
	}
	public double getPolygonX(int i){
		return polygon_x[i];
	}
	public double[] getPolygonXs(){
		return polygon_x;
	}
	public double getPolygonY(int i){
		return polygon_y[i];
	}
	public double[] getPolygonYs(){
		return polygon_y;
	}
	public double getPolygonB(int i){
		return polygon_b[i];
	}
	public double getPolygonExtra(int i){
		return polygon_extra[i];
	}
	public double getPolygonCurveWeight(int i){
		return polygon_cweight[i];
	}
	public double[] getPolygonCurveWeights(){
		return polygon_cweight;
	}
	public void setPolygonCurveWeight(int i, double weight){
		polygon_cweight[i]=weight;
	}
	// getNearestPoint(): given a boolean map, finds the nearest true point to a certain position
	public int[] getNearestPoint(boolean[][] inmap, int sx, int sy){
		int[] thepoint = new int[2];
		
		int radius = 1;
		while(radius<10){
			int i=-radius;
			while(i<=radius){
				int o=-radius;
				while(o<=radius){
					if(sx + i > 0 && sx + i < inmap.length && sy + o > 0 && sy + o < inmap[0].length){
						
						if(inmap[sx + i][sy + o]){
							thepoint[0]=sx+i;
							thepoint[1]=sy+o;
							
							return thepoint;
						}
					}
					o++;
				}
				i++;
			}
			radius++;
		}
		
		return thepoint;
	}
	// getPolygonBAngle(): given a vertex, determines what from what angle the buffer vertex should be extended from that vertex
	// (i.e., finds the angle between the vertex and the buffer'd vertex)
	public double getPolygonBAngle(double pixconv, int i){
		int cona = i-1;
		int conb = i+1;
		if(conb>=polygon_vs){
			conb=0;
		}
		if(cona<0){
			cona=polygon_vs-1;
		}
		
		
		boolean[][] map = getPolygonOutlineMap(pixconv,0,0,0,false);
		
		int mcx = map.length;
		int mcy = map[0].length;
		
		double ix = getPolygonX(i);
		double iy = getPolygonY(i);
		double ax = getPolygonX(cona); 
		double ay = getPolygonY(cona); 
		double bx = getPolygonX(conb); 
		double by = getPolygonY(conb); 
		
		//System.out.println(" OX/Y " + ax + " , " + ay + " B " + bx + " , " + by);
		ax = (double)(polygon_nearpoints[i][0])/(double)(pixconv);
		ay = (double)(polygon_nearpoints[i][1])/(double)(pixconv);
		bx = (double)(polygon_nearpoints[i][2])/(double)(pixconv);
		by = (double)(polygon_nearpoints[i][3])/(double)(pixconv);
		testmap[mcx/2+polygon_nearpoints[i][0]][mcy/2+polygon_nearpoints[i][1]]=true;
		testmap[mcx/2+polygon_nearpoints[i][2]][mcy/2+polygon_nearpoints[i][3]]=true;
		//System.out.println(" NX/Y " + ax + " , " + ay + " B " + bx + " , " + by);
		//System.out.println(" , ");
		
		//output_nearpoints_map(10,100, mcx, mcy, " " + (int)(Math.random()*3000),i);
		
		double aix = ax-ix;
		double aiy = ay-iy;
		double bix = bx-ix;
		double biy = by-iy;
		
		double ailen = Math.sqrt(Math.pow(aix, 2) + Math.pow(aiy, 2));
		double bilen = Math.sqrt(Math.pow(bix, 2) + Math.pow(biy, 2));
		double ablen = Math.sqrt(Math.pow(aix-bix, 2) + Math.pow(aiy-biy, 2));
		double det = (Math.pow(ailen, 2)+Math.pow(bilen, 2)-Math.pow(ablen, 2))/(2*ailen*bilen);
		if(Math.abs(det)-1 < 0.1 && Math.abs(det)>1){
			det=Math.signum(det); // occasionaly rounding errors give 
			// results like det = 1.00000001
			// which obviously should just be rounded to 1
		}
		double angle_1=0;
		if(Math.abs(det)<=1){ // acos only valid between -1 and 1
			angle_1 = Math.acos(det); // angle between a and 0
		}else{
			System.out.println(" !!! ERROR: invalid polygon angles (this is impossible) " + det);
		}

		double angle_2 = Math.PI*2.0-angle_1;
		angle_1/=2; angle_2/=2;
		double angle_small = angle_1;
		double angle_large = angle_2;
		if(angle_2<angle_1){
			angle_small=angle_2;
			angle_large=angle_1;
		}
		
		double angle_0a = getAngleBetweenPoints(ix,iy,ax,ay,ix+10,iy+0);
		if(iy>ay){
			angle_0a = Math.PI*2-angle_0a;
		}
		double angle_0b = getAngleBetweenPoints(ix,iy,bx,by,ix+10,iy+0);
		if(iy>by){
			angle_0b = Math.PI*2-angle_0b;
		}
		double angle_a = angle_0a+angle_small; 
		double angle_b = angle_0b+angle_large; 
		double angle_c = angle_0a+angle_large;
		double angle_d = angle_0b+angle_small;

		double angle_diff_12 = Math.abs(Math.PI-Math.abs(angle_b-angle_a));
		double angle_diff_34 = Math.abs(Math.PI-Math.abs(angle_d-angle_c));
		if(angle_diff_34<angle_diff_12){
			angle_c = angle_0a+angle_small; 
			angle_d = angle_0b+angle_large; 
			angle_a = angle_0a+angle_large;
			angle_b = angle_0b+angle_small;
		}

		// now, angle_a and angle_b are your finalist angles (c and d are nonsolutions)
		//int pixconv= 100;

		boolean[][] map2 = new boolean[mcx][mcy];
		i=0;
		while(i<mcx){
			int o=0;
			while(o<mcy){
				map2[i][o]=map[i][o];
				o++;
			}
			i++;
		}
		//double testa_x = ix;
		//double testa_y = iy;
		double testb_x = ix;
		double testb_y = iy;
		//double y_unit_a = Math.sin(angle_a);
		//double x_unit_a = Math.cos(angle_a);
		double y_unit_b = Math.sin(angle_b);
		double x_unit_b = Math.cos(angle_b);
		
		//while(Math.abs(y_unit_a)>0.5/pixconv || Math.abs(x_unit_a)>0.5/pixconv){ // 0.5/100 where 100 is pixconv, 0.5 is constant
		//	y_unit_a/=2;
		//	x_unit_a/=2;
		//}
		while(Math.abs(y_unit_b)>0.5/pixconv || Math.abs(x_unit_b)>0.5/pixconv){
			y_unit_b/=2;
			x_unit_b/=2;
		}
		//int counts_a = 0;
		int counts_b = 0;
		//boolean on_black_a = true;
		boolean on_black_b = true;
		//boolean done_a = false;
		boolean done_b = false;
		while(!done_b){//!done_a || !done_b){
			//testa_x += x_unit_a;
			//testa_y += y_unit_a;
			testb_x += x_unit_b;
			testb_y += y_unit_b;
			//int reala_x = (int)(pixconv*(testa_x * 100)) + mcx/2;
			//int reala_y = (int)(pixconv*(testa_y * 100)) + mcy/2;
			int realb_x = (int)(pixconv*(testb_x)) + mcx/2;
			
			int realb_y = (int)(pixconv*(testb_y)) + mcy/2;
			if((int)(pixconv*(testb_x))==0){
				realb_x+=Math.signum(testb_x);
			}
			if((int)(pixconv*(testb_y))==0){
				realb_y+=Math.signum(testb_y);
			}
			//System.out.println("testb_x " + testb_x + " y " + testb_y);
			//System.out.println("realb_x " + realb_x + " y " + realb_y);
			/*if(reala_x>=0 && reala_x<mcx && reala_y>=0 && reala_y<mcy){
				if(map[reala_x][reala_y]){
					if(!on_black_a){
						on_black_a=true;
						counts_a++;
					}
				}else{
					on_black_a=false;
				}
			}else{
				done_a=true;
			}*/
			boolean near_black=false;
			if(realb_x>=0 && realb_x<mcx && realb_y>=0 && realb_y<mcy){
				if(map[realb_x][realb_y]){
					near_black=true;
				}
				if(realb_x>0){
					if(map[realb_x-1][realb_y]){
						near_black=true;
					}
					if(realb_y>0){
						if(map[realb_x-1][realb_y-1]){
							near_black=true;
						}
					}
					if(realb_y<mcy-1){
						if(map[realb_x-1][realb_y+1]){
							near_black=true;
						}
					}
				}
				if(realb_x<mcx-1){
					if(map[realb_x+1][realb_y]){
						near_black=true;
					}
					if(realb_y>0){
						if(map[realb_x+1][realb_y-1]){
							near_black=true;
						}
					}
					if(realb_y<mcy-1){
						if(map[realb_x+1][realb_y+1]){
							near_black=true;
						}
					}
				}
				if(realb_y>0){
					if(map[realb_x][realb_y-1]){
						near_black=true;
					}
				}
				if(realb_y<mcy-1){
					if(map[realb_x][realb_y+1]){
						near_black=true;
					}
				}
			}
			if(realb_x>=0 && realb_x<mcx && realb_y>=0 && realb_y<mcy){
				if(near_black){
					if(!on_black_b){
						on_black_b=true;
						counts_b++;
					}
				}else{
					on_black_b=false;
				}
			}else{
				done_b=true;
			}
			if(!done_b){
				map2[realb_x][realb_y]=true;
			}
		}
		//Interpreter.output_map(map2, mcx, mcy, "TEST !" + ((int)(Math.random()*1000) + " ( " + counts_b + ")" +
		//" XS " + (int)(x_unit_b*10000) + " YS " + (int)(y_unit_b*10000)));
		if(counts_b % 2 == 0){
			return angle_b;
		}
		/*double mod = 0.1;
		double testa_x = ix + mod * Math.cos(angle_a);
		double testa_y = iy + mod * Math.sin(angle_a);
		double testb_x = ix + mod * Math.cos(angle_b);
		double testb_y = iy + mod * Math.sin(angle_b);
		
		boolean b_in = is_point_inside_polygon(testb_x,testb_y);
		boolean a_in = is_point_inside_polygon(testa_x,testa_y);
		System.out.println(" AIN " + a_in + " BIN " + b_in);
		
		while(!b_in && !a_in){
			mod *= 0.9;
			testa_x = ix + mod * Math.cos(angle_a);
			testa_y = iy + mod * Math.sin(angle_a);
			testb_x = ix + mod * Math.cos(angle_b);
			testb_y = iy + mod * Math.sin(angle_b);
			b_in = is_point_inside_polygon(testb_x,testb_y);
			a_in = is_point_inside_polygon(testa_x,testa_y);
			System.out.println(" --> AIN " + a_in + " BIN " + b_in);
		}

		boolean got_finalist = false;
		while(!got_finalist){
			b_in = is_point_inside_polygon(testb_x,testb_y);
			a_in = is_point_inside_polygon(testa_x,testa_y);
			if(b_in && !a_in){
				System.out.println(" --> OUT A");
				return angle_a;
			}else if(a_in && !b_in){
				System.out.println(" --> OUT B");
				return angle_b;
			}else if(!a_in && !b_in){
				System.out.println(" --> OUT A (FAILURE)");
				testa_x -= mod * Math.cos(angle_a);
				testa_y -= mod * Math.sin(angle_a);
				testb_x -= mod * Math.cos(angle_b);
				testb_y -= mod * Math.sin(angle_b);
				mod/=2;
				//return angle_a;
			}
			testa_x += mod * Math.cos(angle_a);
			testa_y += mod * Math.sin(angle_a);
			testb_x += mod * Math.cos(angle_b);
			testb_y += mod * Math.sin(angle_b);
		}*/
		//if(is_point_inside_polygon(testa_x,testa_y)){
		//	return angle_b;
		//}
		return angle_a;
	}
	/**
	 * @deprecated Use {@link #getPolygonBPoints(int,double)} instead
	 */
	public double[][] getPolygonBufferPoints(double pixconv, double buffer_override){
		return getPolygonBPoints(pixconv, buffer_override);
	}
	// getPolygonBPoints(): the buffer for a polygon needs to be drawn with a different set of vertices (i.e., expanded outward)
	// this function finds that set of vertices based on the regular set
	public double[][] getPolygonBPoints(double pixconv, double buffer_override){
		double[][] points = new double[polygon_vs][2];
		
		// start building testmap
		testmap = new boolean[1][1];

		testmap = getPolygonOutlineMap(pixconv,0,0,0,false);
		//find the polygon center
		
		int i=0;
		while(i<polygon_vs){
			double ix = polygon_x[i];
			double iy = polygon_y[i];
			double ib = polygon_b[i];
			if(buffer_override !=0){
				ib=buffer_override;
			}
			double iex = polygon_extra[i];
			double iangle = getPolygonBAngle(pixconv,i);
			boolean[][] map = getPolygonOutlineMap(pixconv,0,0,0,true);
			
			points[i][0] = ix + (iex+ib) * Math.cos(iangle);
			points[i][1] = iy + (iex+ib) * Math.sin(iangle);
			// find first empty space in that direction
			double dtestx = ix;
			double dtesty = iy;
			int testx = (int)(dtestx*pixconv)+map.length/2;
			int testy = (int)(dtesty*pixconv)+map[0].length/2;
			if(testx>=0 && testx<map.length && testy>=0 && testy<map[0].length){
				while(map[testx][testy]){
					dtestx+=0.01*Math.cos(iangle);
					dtesty+=0.01*Math.sin(iangle);
					testx = (int)(dtestx*pixconv)+map.length/2;
					testy = (int)(dtesty*pixconv)+map[0].length/2;
					if(!(testx>=0 && testx<map.length && testy>=0 && testy<map[0].length)){
						break;
					}
				}
			}
			if(isPointInsidePolygon(pixconv,dtestx,dtesty)){//points[i][0],points[i][1])){
				iangle = Math.PI+iangle;//get_polygon_b_angle(pixconv,i);
				while(iangle>Math.PI*2){
					iangle-=Math.PI*2;
				}
				while(iangle<0){
					iangle+=Math.PI*2;
				}
				points[i][0] = ix + (iex+ib) * Math.cos(iangle);
				points[i][1] = iy + (iex+ib) * Math.sin(iangle);
				double px = points[i][0];
				double py = points[i][1];
			}
			i++;
		}
		
		// output testmap
		/*BufferedImage image = new BufferedImage(testmap.length,testmap[0].length,BufferedImage.TYPE_INT_RGB);
		i=0;
		while(i<testmap.length){
			int o=0;
			while(o<testmap[0].length){
				int[] col = new int[3];
				col[0]=255; col[1]=255; col[2]=255;
				int dist = dist_from_true(testmap,i,o,3);
				if(dist!=-1){
					col[1]=0;
					col[2]=0;
					col[0]=255-(dist*20);
				}
				image.getRaster().setPixel(i,o,col);
				o++;
			}
			i++;
		}
		System.out.println("SAVING IMAGE AT ./out/test_map_[" + ((int)(Math.random()*3000)) + "].png");
		try{
			ImageIO.write(image, "PNG", new File("./out/test_map_[" + ((int)(Math.random()*3000)) + "].png"));
		}catch(IOException ioe){}*/
		
		return points;
	}
	
	// distFromTrue(): given a boolean map and a point (& a max search radius), returns how far that point is from a true point
	public int distFromTrue(boolean[][] inmap, int sx, int sy, int rad){
		int bestdist = rad+1;
		int a =-rad;
		while(a<=rad){
			int b =-rad;
			while(b<=rad){
				if(sx+a>=0 && sx+a<inmap.length && sy+b>=0 && sy+b<inmap[0].length){
					if(inmap[sx+a][sy+b]){
						int dist = (int)(Math.sqrt(a*a+b*b));
						if(dist<bestdist){
							bestdist=dist;
						}
					}
				}
				b++;
			}
			a++;
		}
		if(bestdist==rad+1){
			bestdist=-1;
		}
		return bestdist;
	}

	// getAngleBetweenPoints(): simply attempts to determine the angle between two lines that come to an intersection at x1,y1
	public double getAngleBetweenPoints(double x1, double y1, double x2, double y2, double x3, double y3){
		// assumes x1,y1 is the center point (the angle point)
		double ailen = Math.sqrt(Math.pow(x2-x1, 2) + Math.pow(y2-y1, 2));
		double bilen = Math.sqrt(Math.pow(x3-x1, 2) + Math.pow(y3-y1, 2));
		double ablen = Math.sqrt(Math.pow((x2-x1)-(x3-x1), 2) + Math.pow((y2-y1)-(y3-y1), 2));
		double det = (Math.pow(ailen, 2)+Math.pow(bilen, 2)-Math.pow(ablen, 2))/(2*ailen*bilen);
		double angle_1=0;
		if(Math.abs(det)-1<0.1 && Math.abs(det)>1){
			det=Math.signum(det);
		}
		if(Math.abs(det)<=1){
			angle_1 = Math.acos(det);
		}else{
			System.out.println(" !!! ERROR: det of polygon angle over 1, acos nonfunctional " + det);
		}
		if(ailen==0 || bilen==0){
			// two of the points must be the same
			System.out.println(" x1 " + x1 + " y1 " + y1 + " x2 " + x2 + " y2 " + y2 + " x3 " + x3 + " y3 " + y3);
			System.out.println(" !!! ERROR: Two vertices in one of the input polygons are the same!");
			return 0;
		}
		return angle_1;
	}
	// isPointInsidePolygon(): returns true if a given point is inside of the polygon rather than outside
	public boolean isPointInsidePolygon(double pixconv, double inpx, double inpy){
		//int pixconv = 20;
		/*int len = 0;
		int wid = 0;
		int negx = 0;
		int posx = 0;
		int negy = 0;
		int posy = 0;
		
		int[] vx = new int[polygon_vs];
		int[] vy = new int[polygon_vs];
		// rotate the points
		int i=0;
		while(i<polygon_vs){
			vx[i] = (int)(pixconv*polygon_x[i]);
			vy[i] = (int)(pixconv*polygon_y[i]);
			if((int)(pixconv*polygon_x[i])<negx){ negx = (int)(pixconv*polygon_x[i]); }
			if((int)(pixconv*polygon_x[i])>posx){ posx = (int)(pixconv*polygon_x[i]); }
			if((int)(pixconv*polygon_y[i])<negy){ negy = (int)(pixconv*polygon_y[i]); }
			if((int)(pixconv*polygon_y[i])>posy){ posy = (int)(pixconv*polygon_y[i]); }
			i++;
		}
		len = Math.abs(negx)+posx;
		wid = Math.abs(negy)+posy;
		int mcx = len+wid;
		int mcy = wid+len;*/
		// map the polygon
		boolean[][] map = getPolygonOutlineMap(pixconv,0,0,0,true);//new boolean[mcx*2][mcy*2];
		boolean[][] map_fill = new boolean[map.length][map[0].length];
		int mcx = map.length/2;
		int mcy = map[0].length/2;
		int i=0;
		while(i<mcx*2){
			int o=0;
			while(o<mcy*2){
				map_fill[i][o]=false;
				o++;
			}
			i++;
		}
		//map = get_polygon_outline_map(pixconv,0,0,0,true);
		
		int px = (int)(inpx*pixconv)+mcx;
		int py = (int)(inpy*pixconv)+mcy;
		if(px>=mcx*2 || py>=mcy*2){
			System.out.println(" !!! ERROR: Testing whether point was within polygon failed due to test point being beyond outer edge.");
			System.out.println(" (This error may be insignificant) ");
			return false;
		}
		if(map[px][py]==false){
			int valids = 0;
			//pos x
			i=px;
			int count=0;
			boolean onblack=false;
			
			// this may be a little obtuse looking but the gist of it is:
			// when we find a filled region, that's a 'line'
			// we don't want to double count this region, so we turn off counting
			// until we hit unfilled space again (hence "onblack", which is
			// true when we're already in a filled space and if it is true we
			// don't increment the count value.

			while(i<mcx*2){
				if(!onblack && map[i][py]){
					count++;
					onblack=true;
				}else if (onblack && !map[i][py]){
					onblack=false;
				}
				i++;
			}
			if(count%2>0){
				valids++;
			}
			
			i=px; // neg x
			count=0;
			onblack=false;
			
			while(i>=0){
				if(!onblack && map[i][py]){
					count++;
					onblack=true;
				}else if (onblack && !map[i][py]){
					onblack=false;
				}
				i--;
			}
			if(count%2>0){
				valids++;
			}
			
			i=py; // pos y
			count=0;
			onblack=false;
			
			while(i<mcx*2){
				if(!onblack && map[px][i]){
					count++;
					onblack=true;
				}else if (onblack && !map[px][i]){
					onblack=false;
				}
				i++;
			}
			if(count%2>0){
				valids++;
			}
			
			i=py; // neg y
			count=0;
			onblack=false;
			
			while(i>=0){
				if(!onblack && map[px][i]){
					count++;
					onblack=true;
				}else if (onblack && !map[px][i]){
					onblack=false;
				}
				i--;
			}
			if(count%2>0){
				valids++;
			}
			//System.out.println("VALIDS " + valids);
			if(valids>=4){
				return true;
			}
		}else{
			return true;
		}
		return false;
	}
	/**
	 * @deprecated Use {@link #getPolygonVs()} instead
	 */
	public int getPolygonPointcount(){
		return getPolygonVs();
	}
	public int getPolygonVs(){
		return polygon_vs;
	}
	public int getPolygonCtype(int i){
		return polygon_ctype[i];
	}
	public int[] getPolygonCtypes(){
		return polygon_ctype;
	}
	public double getPolygonCrotX(){
		return polygon_crotx;
	}
	public double getPolygonCrotY(){
		return polygon_croty;
	}
	public boolean getPolygonFilled(){
		return polygon_filled;
	}
	public void setPolygonFilled(boolean in){
		polygon_filled=in;
	}
	public void setPolygonCrot(double cx, double cy){
		polygon_crotx=cx; // crot = center of rotation
		polygon_croty=cy;
	}
	// getPolygonLen(): figures out how long the polygon is
	public double getPolygonLen(){
		if(polygon_hasendpoint){
			// if the polygon has a set endpoint by the user, override this function
			// and return that value instead.
			return polygon_endx;
		}
		double negx = 0;
		double posx = 0;
		int i=0;
		while(i<polygon_vs){
			double ix = polygon_x[i];
			if(ix>posx){
				posx=ix;
			}
			if(ix<negx){
				negx=ix;
			}
			i++;
		}
		return Math.abs(negx) + posx;
	}
	// getPolygonWid(): figures out how wide the polygon is
	public double getPolygonWid(){
		double negx = 0;
		double posx = 0;
		int i=0;
		while(i<polygon_vs){
			double ix = polygon_y[i];
			if(ix>posx){
				posx=ix;
			}
			if(ix<negx){
				negx=ix;
			}
			i++;
		}
		return Math.abs(negx) + posx;
	}
	// getPolygonStartWid(): figures out how wide the polygon is
	public double getPolygonStartWid(){
		double furthest_left = getPolygonWid()*-10;
		double fl_y = 0;
		double furthest_left_2 = furthest_left; // second furthest point
		double fl_y2 = 0;
		int i=0;
		while(i<polygon_vs){
			double ix = polygon_x[i];
			double iy = polygon_y[i];
			if(ix>furthest_left){
				furthest_left=ix;
				fl_y=iy;
			}else if(ix>furthest_left_2){
				furthest_left_2=ix;
				fl_y2=iy;
			}
			
			i++;
		}
		// finds the two furthest left points and adds their yvalues together
		return Math.abs(fl_y)+Math.abs(fl_y2);
	}
	// setPolygonBuffer(): sets ALL vertices to have this buffering
	public void setPolygonBuffer(double bf){
		int i=0;
		while(i<polygon_vs){
			polygon_b[i]=bf;
			i++;
		}
	}
	// setPolygonExtra(): set ALL vertices to have this extra whitespace
	public void setPolygonExtra(double bf){
		int i=0;
		while(i<polygon_vs){
			polygon_extra[i]=bf;
			i++;
		}
	}
	// finalizePolygonVs(): fixes the polygon's vertices depending on curvature and some other issues
	// readjusts them so they are properly aligned over the center
	public void finalizePolygonVs(){
		// takes the vertices of the polygon and inverts them so they'll rest properly
		int i=0;
		double poslen=0;
		double neglen=0;
		while(i<polygon_vs){
			double ix = polygon_x[i];
			double iy = polygon_y[i];
			if(ix>poslen){
				poslen=ix;
			}
			if(ix<neglen){
				neglen=iy;
			}
			i++;
		}
		double totallen = Math.abs(neglen)+Math.abs(poslen);
		double centerlen = totallen/2;
		i=0;
		while(i<polygon_vs){
			double dist_to_cent = polygon_x[i]-centerlen;
			polygon_x[i] -= dist_to_cent*2; 
			//polygon_y[i]=-1*polygon_y[i];
			i++;
		}
		i=0;
		while(i<polygon_vs){
			// do some testing; this is to correct the ctypes
			// this is a brute-force solution that makes the two options align with expectations
			// i.e. that the higher route will be taken via ctype 1
			// and the lower route via ctype 2.
			if(polygon_ctype[i]>0){
				int nexti = i+1;
				if(nexti>=polygon_vs){
					nexti=0;
				}
				double ox = polygon_x[i];
				double oy = polygon_y[i];
				double nx = polygon_x[nexti];
				double ny = polygon_y[nexti];

				double[] pack = Interpreter.curveOrientation((double)(ox),(double)(oy),(double)(nx),(double)(ny),1);
				double cx = pack[0];
				double cy = pack[1];
				double xmod = (pack[2]);
				double mod = (pack[3]);
				double a = (pack[4]);
				double b = (pack[5]);
				double dist_x = (pack[6]);
				double dist_y = (pack[7]);

				double y_1 = cy+mod*b*Math.sin(Math.toRadians(35))+dist_y;
				
				pack = Interpreter.curveOrientation((double)(ox),(double)(oy),(double)(nx),(double)(ny),2);
				cx = pack[0];
				cy = pack[1];
				xmod = (pack[2]);
				mod = (pack[3]);
				a = (pack[4]);
				b = (pack[5]);
				dist_x = (pack[6]);
				dist_y = (pack[7]);

				double y_2 = cy+mod*b*Math.sin(Math.toRadians(35))+dist_y;
				
				System.out.println(" VX " + i + " y_1 " + y_1 + " y_2 " + y_2 + " CTYPE " + polygon_ctype[i]);
				if(y_1<y_2 && polygon_ctype[i]==2){
					polygon_ctype[i]=1;
				}else if(y_2>y_1 && polygon_ctype[i]==1){
					polygon_ctype[i]=2;
				}
			}
			i++;
		}
	}
	// getPolygonOutlineMap(): gives the boolean map that represents the polygon's outline
	public boolean[][] getPolygonOutlineMap(double pixconv, double angle, int crotx, int croty, boolean all_sharp){
		int len = 0;
		int wid = 0;
		int negx = 0;
		int posx = 0;
		int negy = 0;
		int posy = 0;
		
		int[] vx = new int[polygon_vs];
		int[] vy = new int[polygon_vs];
		// rotate the points
		int i=0;
		while(i<polygon_vs){
			vx[i] = (int)(pixconv*polygon_x[i]);
			vy[i] = (int)(pixconv*polygon_y[i]);
			i++;
		}
		
		// rotate the points
		i=0;
		while(i<polygon_vs){
			int ox = vx[i]-crotx; // old values
			int oy = vy[i]-croty;
			vx[i] = (int)(ox * Math.cos(angle) - oy*Math.sin(angle)); // set new values (rotated)
			vy[i] = (int)(oy * Math.cos(angle) + ox*Math.sin(angle));
			
			i++;
		}
		i=0;
		while(i<polygon_vs){
			if(vx[i]<negx){ negx = vx[i]; }
			if(vx[i]>posx){ posx = vx[i]; }
			if(vy[i]<negy){ negy = vy[i]; }
			if(vy[i]>posy){ posy = vy[i]; }
			i++;
		}
		len = Math.abs(negx)+posx;
		wid = Math.abs(negy)+posy;
		int mcx = len+wid;
		int mcy = wid+len;
		// map the polygon
		boolean[][] map = new boolean[mcx*2][mcy*2];
		i=0;
		while(i<mcx*2){
			int o=0;
			while(o<mcy*2){
				map[i][o]=false;
				o++;
			}
			i++;
		}
		// move the points to center
		i=0;
		while(i<polygon_vs){
			vx[i] = mcx+vx[i]; // set new values (rotated)
			vy[i] = mcy+vy[i];
			map[vx[i]][vy[i]]=true;
			i++;
		}
		i=0;
		while(i<polygon_vs){
			int rad=16;
			int a=-rad;
			while(a<=rad){
				int b= -rad;
				while(b<=rad){
					if(vx[i]+a>=0 && vx[i]+a<mcx*2 && vy[i]+b>=0 && vy[i]+b<mcy*2){
						map[vx[i]+a][vy[i]+b]=true;
					}
					b++;
				}
				a++;
			}
			i++;
		}
		// output testmap
		/*if(first_vex_map){
			
				BufferedImage image = new BufferedImage(map.length,map[0].length,BufferedImage.TYPE_INT_RGB);
				i=0;
				while(i<map.length){
					int o=0;
					while(o<map[0].length){
						int[] col = new int[3];
						col[0]=155; col[1]=155; col[2]=155;
						int dist = dist_from_true(map,i,o,1);
						if(dist!=-1){
							col[1]=0;
							col[2]=0;
							col[0]=255-dist*6;
						}
						image.getRaster().setPixel(i,o,col);
						o++;
					}
					i++;
				}
				System.out.println("SAVING IMAGE AT ./out/vex_test_map_[" + ((int)(Math.random()*3000)) + "].png");
				try{
					ImageIO.write(image, "PNG", new File("./out/vex_test_map_[" + ((int)(Math.random()*3000)) + "].png"));
				}catch(IOException ioe){}
				first_vex_map=false;
		}*/
				
		
		// draw the points
		i=0;
		while(i<polygon_vs){
			// draw lines from vertex to vertex
			int ox = vx[i]; // original point
			int oy = vy[i];
			int nx=0; // next point
			int ny=0;
			int nexti = i+1;
			if(i+1 < polygon_vs){
				nx = vx[i+1];
				ny = vy[i+1];
			}else{ // if we're at the end, loop back to first point
				nx = vx[0];
				ny = vy[0];
				nexti=0;
			}
			double cx = ox; // current position ( tracking )
			double cy = oy; 
			double slope = 0;
			double xchange = 1;
			if(nx<cx){ 
				xchange = -1; // if we're going backwards in x, xchange should be negative   
			}
			if(nx==cx){
				xchange=0;
				slope = 1;
			}else{
				slope = (double)((ny-oy))/(double)((nx-ox)); // rise/run
			}
			if(ny<oy){
				slope=-1*Math.abs(slope);
			}else{
				slope=Math.abs(slope);
			}
			
			while(Math.abs(slope)>0.5){ // reduce slope so we don't skip any steps
				slope/=2;
				xchange/=2;
			}
			
			if(polygon_ctype[i]==0 || all_sharp){
				int enough = -1; // once we are 'close enough' to the point, it'll stop shortly after
				while( ((int)(cx)!=nx || (int)(cy)!=ny  ) && cx<mcx*2 && cy<mcy*2 && cx>=0 && cy>=0 && enough!=0){
					map[(int)(cx)][(int)(cy)]=true;
					cy+=slope;
					cx+=xchange;
					if(enough>0){
						enough--;
					}
					if( ((int)(cx)-1== nx || (int)(cx)+1==nx || (int)(cx)==nx) && 
						((int)(cy)-1== ny || (int)(cy)+1==ny || (int)(cy)==ny) &&
						enough==-1){
						enough=1;   
					}
				}
				polygon_nearpoints[i][0]=nx-mcx;
				polygon_nearpoints[i][1]=ny-mcy;
				polygon_nearpoints[nexti][2]=ox-mcx;
				polygon_nearpoints[nexti][3]=oy-mcy;
			}else if(polygon_ctype[i]==1 || polygon_ctype[i]==2){
				// curve up or down
				//IMPLEMENTING A CURVE:
				// - ellipse = a * cos(phi), b * sin(phi)
				// a = horz distance
				// b = vert distance
				// should just iterate through phi from 0 to 90deg?
				double[] pack = Interpreter.curveOrientation((double)(ox),(double)(oy),(double)(nx),(double)(ny),polygon_ctype[i]);
				cx = pack[0];
				cy = pack[1];
				int xmod = (int)(pack[2]);
				int mod = (int)(pack[3]);
				int a = (int)(pack[4]);
				int b = (int)(pack[5]);
				int dist_x = (int)(pack[6]);
				int dist_y = (int)(pack[7]);
				double iang=0;
				double angend=90;
				map[(int)(ox)][(int)(oy)]=true;
				map[(int)(nx)][(int)(ny)]=true;
				if(ox==cx){
					polygon_nearpoints[i][0]=(int)(ox+xmod*a*Math.cos(Math.toRadians(10))+dist_x)-mcx;
					polygon_nearpoints[i][1]=(int)(oy+mod*b*Math.sin(Math.toRadians(10))+dist_y)-mcy;
					polygon_nearpoints[nexti][2]=(int)(ox+xmod*a*Math.cos(Math.toRadians(80))+dist_x)-mcx;
					polygon_nearpoints[nexti][3]=(int)(oy+mod*b*Math.sin(Math.toRadians(80))+dist_y)-mcy;
				}else{
					polygon_nearpoints[i][0]=(int)(cx+xmod*a*Math.cos(Math.toRadians(10))+dist_x)-mcx;
					polygon_nearpoints[i][1]=(int)(cy+mod*b*Math.sin(Math.toRadians(10))+dist_y)-mcy;
					polygon_nearpoints[nexti][2]=(int)(cx+xmod*a*Math.cos(Math.toRadians(80))+dist_x)-mcx;
					polygon_nearpoints[nexti][3]=(int)(cy+mod*b*Math.sin(Math.toRadians(80))+dist_y)-mcy;
				}
				
				int lastxpos = (int)(cx);
				int lastypos = (int)(cy);
				int best_ox_dist=ox*100;
				int best_cx_dist=ox*100;
				while(iang<=angend){
					double riang = Math.toRadians(iang);
					
					int xpos = (int)(cx+xmod*a*Math.cos(riang)+dist_x);
					int ypos = (int)(cy+mod*b*Math.sin(riang)+dist_y);
					double[] npos = Interpreter.curveWeight(xpos, ypos, polygon_ctype[i], ox, oy, nx, ny, polygon_cweight[i]);
					xpos = (int)(npos[0]);
					ypos = (int)(npos[1]);
					if(iang<80 && iang>10){
						int ox_dist = (int)(Math.sqrt(Math.pow(Math.abs(ox-xpos),2) + Math.pow(Math.abs(oy-ypos),2)));
						int cx_dist = (int)(Math.sqrt(Math.pow(Math.abs(nx-xpos),2) + Math.pow(Math.abs(ny-ypos),2)));
						if(ox_dist<best_ox_dist){
							polygon_nearpoints[i][0]=xpos-mcx;
							polygon_nearpoints[i][1]=ypos-mcy;
							best_ox_dist=ox_dist;
						}
						if(cx_dist<best_cx_dist){
							polygon_nearpoints[nexti][2]=xpos-mcx;
							polygon_nearpoints[nexti][3]=ypos-mcy;
							best_cx_dist=cx_dist;
						}
					}
					if(xpos>=0 && xpos<mcx*2 && ypos>=0 && ypos<mcy*2){
						map[xpos][ypos]=true;
						map=Interpreter.interp(map,lastxpos,lastypos,xpos,ypos);
					}
					lastxpos=xpos;
					lastypos=ypos;
					iang+=0.01;
				}
				int nextx = nx;
				int nexty = ny;
				if(cx==nx && cy==ny){
					nextx=ox;
					nexty=oy;
				}
				map=Interpreter.interp(map, lastxpos, lastypos, nextx, nexty);
			}

			i++;
		}

		return map;
	}
	// outputNearpointMap(): debug function
	public void outputNearpointsMap(int reduce, int pixconv, int mcx, int mcy, String name, int ini){
		BufferedImage image = new BufferedImage(mcx/reduce, mcy/reduce, BufferedImage.TYPE_INT_RGB);
		WritableRaster raster = image.getRaster();
		
		int i=0;
		while(i<mcx/reduce){
			int o=0;
			while(o<mcy/reduce){
				try{
					int[] col = new int[3];
					col[0]=255; col[1]=255; col[2]=255;
					raster.setPixel(i,o,col);
				}catch(Exception e){
					
				}
				o++;
			}
			i++;
		} 
		
		i=0;
		while(i<polygon_vs){
			int ax = polygon_nearpoints[i][0];
			int ay = polygon_nearpoints[i][1];
			int bx = polygon_nearpoints[i][2];
			int by = polygon_nearpoints[i][3];
			try{
				int[] col = new int[3];
				col[0]=(255/polygon_vs)*i; col[1]=255-(255/polygon_vs)*i; col[2]=0;
				int imod=-1; int imodmax=1;
				if(ini==i){
					imod=-3; imodmax = 3;
				}
				while(imod<=imodmax){
					int omod = -1; int omodmax=1;
					if(ini==i){
						omod=-3; omodmax=3;
					}
					while(omod<=omodmax){
						if(i==ini){
							raster.setPixel((ax+mcx/2)/reduce+imod,(ay+mcy/2)/reduce+omod,col);
							raster.setPixel((bx+mcx/2)/reduce+imod,(by+mcy/2)/reduce+omod,col);
						}
						raster.setPixel(((int)(polygon_x[i]*pixconv)+mcx/2)/reduce+imod*2,((int)(polygon_y[i]*pixconv)+mcy/2)/reduce+omod*2,col);
						omod++;
					}
					imod++;
				}
			}catch(Exception e){
				
			}
			int cx = ((int)(polygon_x[i]*pixconv)+mcx/2)/reduce;
			int cy = ((int)(polygon_y[i]*pixconv)+mcy/2)/reduce;
			int nexti = i+1;
			if(nexti>=polygon_vs){
				nexti=0;
			}
			int nx = ((int)(polygon_x[nexti]*pixconv)+mcx/2)/reduce;
			int ny = ((int)(polygon_y[nexti]*pixconv)+mcy/2)/reduce;
			while(cx!=nx || cy!=ny){
				if(cx>nx){ cx--; }
				if(cx<nx){ cx++; }
				if(cy>ny){ cy--; }
				if(cy<ny){ cy++; }
				try{
					int[] col = new int[3];
					col[0]=(255/polygon_vs)*i; col[1]=255-(255/polygon_vs)*i; col[2]=0;
					raster.setPixel(cx,cy,col);
				}catch(Exception e){
					
				}
			}
			i++;
		}
		
		
			
		/*System.out.println("SAVING IMAGE AT ./out/temp_map_" + name + " (" + ini + ")" + ".png");
		try{
			ImageIO.write(image, "PNG", new File("./out/temp_map_" + name  + " (" + ini + ")" +  ".png"));
		}catch(IOException ioe){}*/
		
	}
}
