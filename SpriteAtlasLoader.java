package com.twopisoftware.yukonsolitaire;

import java.lang.reflect.Field;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.IntBuffer;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import android.app.Activity;
import android.util.DisplayMetrics;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.graphics.Rect;
import android.graphics.RectF;
import android.util.Log;

/***************
 * 
 * @author Adam Piette
 *
 * Sprite Atlas Loader
 * -------------------
 * 
 * loads xml files in the following format
 * 
 * <?xml version="1.0"?>
 * <atlas version="1.0">
 *   <sheets total="1">
 *     <sheet index="0" file="items_0.png">
 *       <items>
 *         <item name="new_set/item1" x="6" y="6" width="200" height="300"/>
 *         <item name="new_set/item2" x="207" y="6" width="200" height="300"/>
 *       </items>
 *     </sheet>
 * 
 */

class StringMap<T> extends LinkedHashMap<String,T> {
	
}

public class SpriteAtlasLoader {
	
	//private Bitmap image_file[];
	StringMap<Bitmap> map = new StringMap<Bitmap>();
	StringMap<ByteBuffer> bmap = new StringMap<ByteBuffer>();
	
	static private float scale=1.0f;
	static private boolean mipmap=false;
	
	public static void SetScale( float sc ) {
		scale = sc;
	}
	
	//-- api 18 only, not used
	public static void SetMipMap() {
		mipmap = true;
	}
	
	public Bitmap Get(String file){
		return map.get(file);
	}
	
	public Bitmap Get(String file, Matrix mat, Boolean filter){
		Bitmap b = map.get(file);
		if (b != null) {
			return b; //Bitmap.createBitmap(b, 0,0,b.getWidth(), b.getHeight(), mat, filter);
		} else {
			return null;
		}
	}
	
	public static SpriteAtlasLoader Load(String file, Activity activity, String packageName) {
		
		DocumentBuilder builder = null;
		Document doc = null;
		
		SpriteAtlasLoader atlas = new SpriteAtlasLoader();
		DisplayMetrics metrics = new DisplayMetrics();
		
		try {
			
			builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
			
		} catch (ParserConfigurationException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		if (builder != null ) {
			try {
				//Log.d("DEBUG",file);
				
				//int rr = getResources().getIdentifier(file, "assets", "com.example.test1");
				doc = builder.parse(activity.getResources().getAssets().open( file ) );
				
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		
		if (doc == null) return null;
		
		//atlas.image_file = new Bitmap[list.getLength()];
		Bitmap image_file;
		
		BitmapFactory.Options opts=new BitmapFactory.Options();
		//opts.inSampleSize = 2;
		opts.inDither=false; //Disable Dithering mode
		opts.inPurgeable=true;
		opts.inScaled=false;
		//opts.inMutable=false;
		//opts.inTargetDensity=metrics.densityDpi;
		//opts.inPreferredConfig = Bitmap.Config.RGB_565;
		
		//scale = 0.25f;
		Matrix mat = new Matrix();
		mat.reset();
		mat.setScale(scale, scale);
		
		NodeList list = doc.getDocumentElement().getElementsByTagName("sheet");
		NodeList items;
		String res_file, item_name;
		int x,y,width,height;

		ByteBuffer buffer = null;
		
		for (int i=0; i<list.getLength(); i++) {
			Node node = list.item(i);
			//Log.d("DEBUG","node");
			
			res_file = node.getAttributes().getNamedItem("file").getNodeValue();
			res_file = res_file.substring(0, res_file.indexOf('.'));
			//Log.d("DEBUG",res_file);
			int rr = activity.getResources().getIdentifier(res_file, "drawable", packageName);
			
			image_file = BitmapFactory.decodeResource(activity.getResources(), rr, opts);
			if (image_file==null) Log.d("ERROR","**Resource file not found:"+res_file+" "+packageName);
			
			image_file = Bitmap.createBitmap(image_file, 0, 0, image_file.getWidth(), image_file.getHeight(), mat , true);
			
			image_file.setDensity(Bitmap.DENSITY_NONE);
			//image_file.setHasMipMap(true);
			
			/*Node nn = node.getFirstChild();
			if (nn instanceof Element) {
			    Element childElement = (Element) nn;
			    Log.d("DEBUG", childElement.getTagName() );
			  }*/
			
			items = ((Element)node).getElementsByTagName("item");
			//items = node.getChildNodes();
			
			if (items == null || image_file == null) continue;
			
			for (int j=0; j<items.getLength(); j++) {
				Node node2 = items.item(j);

				//Log.d("DEBUG",((Element)node2).getTagName());
				
				if ( ((Element)node2).getTagName().equals("item") ) {
					
					item_name = node2.getAttributes().getNamedItem("name").getNodeValue();
					
					
					x = (int) (Integer.parseInt(node2.getAttributes().getNamedItem("x").getNodeValue())*1.0f); ///opts.inSampleSize;
					y = (int) (Integer.parseInt(node2.getAttributes().getNamedItem("y").getNodeValue())*1.0f); ///opts.inSampleSize;
					width = (int) (Integer.parseInt(node2.getAttributes().getNamedItem("width").getNodeValue())*1.0f); ///opts.inSampleSize;
					height = (int) (Integer.parseInt(node2.getAttributes().getNamedItem("height").getNodeValue())*1.0f); ///opts.inSampleSize;
					
					//Log.d("DEBUG",item_name+" "+x+" "+y+" "+width+" "+height);
					
					Bitmap bmp = Bitmap.createBitmap(image_file, (int)(x*scale), (int)(y*scale), (int)(width*scale), (int)(height*scale));//, mat , true);
					//bmp.setHasMipMap(true); //api 18
					
					//float points[] = {width, height};
					//mat.mapPoints(points);
					//bmp = Bitmap.createScaledBitmap(bmp, 100,150,true);
					
					if (bmp != null) atlas.map.put(item_name, bmp);
					//if (image_file != null) atlas.map.put(item_name,  image_file);
					//if (image_file != null) atlas.bmap.put(item_name,  buffer);
					
				}
			}
			
			//image_file.recycle();
		}
		
		return atlas;
	}
}
