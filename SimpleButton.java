package com.twopisoftware.snippets;

import java.util.LinkedList;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.PorterDuffXfermode;
import android.graphics.PorterDuff;


/************
 * 
 * @author Adam Piette
 * 
 * SimpleButton
 * ------------
 * 
 * 
 *  
 */

public class SimpleButton {
	//private static LinkedList<SimpleButton> blist = new LinkedList<SimpleButton>();
	
	public Bitmap bitmap, activeBitmap;
	public int x,y;
	public float sx=1.0f, sy=1.0f;
	
	public Tween tween;
	
	private Paint img_paint = new Paint();
	private Matrix mat = new Matrix();
	private boolean wasDrawn = false, wasDown = false, useActiveState=false;
	private int activeTick=0, totalTick=6;
	private int[] rgb = {-1,-1,-1};
	private PorterDuffXfermode pduff;
	
	SimpleButton() {
		
	}
	
	SimpleButton(Bitmap b) {
		this.bitmap = b;
	}
	
	//-- x,y = position, sx,sy =scale
	SimpleButton(Bitmap b, int x, int y, float sx, float sy) {
		this.bitmap = b;
		this.x=x; this.y=y; this.sx=sx;this.sy=sy;
		
		//blist.add(this);
	}
	
	void delete() {
		bitmap=null;
		//blist.remove(this);
	}
	
	void create(Bitmap b, float x, float y, float sx, float sy) {
		Create(b,(int)x,(int)y,sx,sy);
	}
	
	void create(Bitmap b, int x, int y, float sx, float sy) {
		this.bitmap = b;
		this.x=x; this.y=y; this.sx=sx;this.sy=sy;

	}
	
	//--length of time that xor or bitmap is displayed when pressed
	void setActiveTimer(int t) {
		totalTick = t;
	}
	
	//--bitmap to display when pressed
	void setActiveBitmap(Bitmap b) {
		activeBitmap = b;
		useActiveState=true;
	}
	
	//--instead of bitmap, xor with this clor when pressed
	void setActiveXor(int r, int g, int b) {
		rgb = new int[]{r,g,b};
		useActiveState=true;
		pduff = new PorterDuffXfermode( PorterDuff.Mode.XOR);
	}
	
	void draw(Canvas c, float x, float y, float sx, float sy) {
		this.x=(int)x; this.y=(int)y; this.sx=sx; this.sy=sy;
		draw(c);
	}
	
	private float[] mtemp = new float[9];
	
	void draw(Canvas c, Matrix mat) {
		//overwrite mtemp each time
		mat.getValues(mtemp);
		this.x = (int)mtemp[Matrix.MTRANS_X];
		this.y = (int)mtemp[Matrix.MTRANS_Y];
		this.sx = mtemp[Matrix.MSCALE_X];
		this.sy = mtemp[Matrix.MSCALE_Y];
		draw(c);
	}
	
	void draw(Canvas c) {
		float tx=0.0f,ty=0.0f,tz=1.0f;
		if (tween!=null) {
			tx = tween.x; ty= tween.y;
			if (tween.getComp() !=null) tz=tween.getCompX();
		}
		mat.setTranslate(x+tx,y+ty);
		mat.preScale(sx*tz, sy*tz);
		if (activeTick>0 && useActiveState) {
			
			if (rgb[0] >=0) {
				img_paint.setXfermode(pduff);
			}
			if (activeBitmap!=null) {
				c.drawBitmap(activeBitmap,  mat,  img_paint);
			} else {
				c.drawBitmap(bitmap, mat, img_paint);
			}
			
			img_paint.setXfermode(null);
			
			activeTick +=1;
			if (activeTick>totalTick) activeTick=0;
			
		} else {
			c.drawBitmap(bitmap, mat, img_paint);
		}
		wasDrawn=true;
	}
	
	boolean isDown(int tx, int ty) {
		return isDown(tx,ty,0.0f);
	}
	
	boolean isDown(float tx, float ty) {
		return isDown((int)tx,(int)ty,0.0f);
	}
	
	boolean isDown(int tx, int ty, float offset) {
		boolean r= false;
		if (bitmap==null) return r;
		
		if (tx>x-offset && tx<=bitmap.getWidth()*sx+x+offset && ty>y-offset && ty<=bitmap.getHeight()*sy+y+offset) {
			if (wasDrawn==true) {
				r=true;
				activeTick = 1;
			}
		}
		
		//*** this needs something better, as false touches will register on ghost buttons...
		wasDrawn=false; //clear this flag here, so buttons not drawn are disabled
		return r;
	}
}
