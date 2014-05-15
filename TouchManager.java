package com.twopisoftware.snippets;

import android.os.SystemClock;
import android.util.Log;
import android.view.MotionEvent;

/************
 * 
 * @author Adam Piette
 * 
 * TouchManager
 * ------------
 * 
 * 
 * using an instance of the class allows for touch events to be translated better (Drag, double tap) into specific use actions.
 * Use TouchManager.Event.[enum] to identify the action.
 * use toString() to debug.
 * 
 * usage:
 *  TouchManager touch = new TouchManager(w,h);
 *  int actionEvent = touch.getAction(event.getActionMasked(), event.getX(),event.getY());
 *  if (actionEvent == TouchManager.Event.DOWN ) { ... };
 *  
 */

public class TouchManager {
	
	public enum Event {
		DOWN,
		UP,
		MOVE,
		DRAG,
		DROP,
		HIT, //cant do hit and up
		DOUBLETAP,
		NONE;
		
		public String toString() {
			String s="NONE";
			if (this==DOWN) s="DOWN";
			if (this==UP) s="UP";
			if (this==MOVE) s="MOVE";
			if (this==DROP) s="DROP";
			if (this==HIT) s="HIT";
			if (this==DOUBLETAP) s="DOUBLETAP";
			if (this==DRAG) s="DRAG";
			return s;
		}
	}
	
	public float x,y;
	
	public static String printData;
	
	private int touchDown=0, touchDrag=0;
	private float startX=0.0f, startY=0.0f;
	private float oldX=0.0f, oldY=0.0f, maxX=0.0f, maxY=0.0f;
	
	private int up_down=0, left_right=0, tapCount=0;
	private float deltaX=0.0f, deltaY=0.0f, origX, origY, tapX, tapY;
	
	
	private long lastTime, doubleTap;
	private Event lastEvent;
	
	private static float DIR_SENSITIVITY;
	
	
	public TouchManager() {
		DIR_SENSITIVITY = 20.0f;
	}
	
	//-- an approximate way to determine drag sensitivity based on resolution
	public TouchManager(int w, int h) {
		DIR_SENSITIVITY = w*40.0f/1000.0f;
		//Log.d("DEBUG","sens "+Float.toString(DIR_SENSITIVITY));
	}
	
	public Event getAction(int eventInt, float eventX, float eventY) {
		
		Event evt = Event.NONE;
		
		int action = eventInt; //event.getActionMasked();
		x = eventX; //event.getX();
		y = eventY; //event.getY();
		
		long ms = SystemClock.elapsedRealtime();
		
		//Log.d("DEBUG",eventInt+"  "+MotionEvent.ACTION_DOWN+"  ");	
		
		if (action == MotionEvent.ACTION_DOWN ) {
			
			//eat some time to get rid of inadvertant taps
			if (ms-lastTime > 50 ) {
				//printData = eventX+" "+eventY;
				
				touchDown = 1;
				startX = x; startY = y;
				origX=x; origY=y;
				
				evt = Event.DOWN;
				lastTime = ms;
				
				//-- reset touchdrag?
				if (ms-lastTime > 300 ) {
	
					touchDrag=0;
					
				}
			}
			
		} else if (action == MotionEvent.ACTION_MOVE ) {
			//printData = event.getX()+" "+event.getY();
			printData = "LR/UD "+Integer.toString(left_right)+"  "+Integer.toString(up_down);
			//printData = "max "+Float.toString(maxX)+"  "+Float.toString(maxY);
			
			float ww = Math.abs(x-startX);
			float hh = Math.abs(y-startY);
			
			if (ww>maxX) maxX=ww; //max bounds
			if (hh>maxY) maxY=hh;
			
			evt=Event.DRAG;

			
			//sensitivity for acting as a hold, eat event
			if ((maxX>DIR_SENSITIVITY || maxY>DIR_SENSITIVITY) ) {
				if (maxX>maxY) {
					left_right = (int) (x-startX);
					up_down = 0;
					
				} else {
					up_down = (int) (y-startY);
					left_right = 0;
					
				}
				
				touchDrag=1;
				
			} else {
				if (touchDrag==0) {
					evt= Event.NONE;
					printData = "EAT EVENT "+Integer.toString(touchDrag);
				}
				
			}

			//-- reset every n ms to allow for movement changes
			//-- when movement lessons (ww/hh) then reset the max bounds, allows for shift
			if (ms-lastTime > 300 ) {
				
				startX = x; startY = y;
				lastTime = ms;
				
				if (ww<5.0f && hh<5.0f) {
					maxX=0.0f;
					maxY=0.0f;
					
				}
				
			}

		}else if (action == MotionEvent.ACTION_UP) {
			
			//-- set default first
			if (touchDrag>0) {
				evt=Event.DROP;
				tapCount=0;
			} else {
				evt=Event.UP;
				
				if (tapCount==0) {
					//Log.d("DEBUG","firsttap ");
					doubleTap = ms+1;
					tapX = x; tapY=y;
				}
				if (tapCount==1) {
					//Log.d("DEBUG","double! ");
				}
				tapCount++;
			}

			touchDown =0;
			touchDrag=0;
			maxX=0.0f;
			maxY=0.0f;
		}
		
		if (evt == Event.UP && tapCount>1) {
			//Log.d("DEBUG","doubletap "+Integer.toString((int)(ms-doubleTap)));
			if(ms-doubleTap < 400 && (Math.abs(tapX-x)<DIR_SENSITIVITY*2.0f  && Math.abs(tapY-y)<DIR_SENSITIVITY*2.0f )) {
				evt=Event.DOUBLETAP;
				
				//doubleTap=0;
				tapCount=0;
			}
		}
		
		//reset tapCount every 400ms
		if(ms-doubleTap > 400) {
			doubleTap=ms;
			tapCount=0;
		}
		
		oldX=x; oldY=y;
		lastEvent = evt;
		
		return evt;
	}
	
	public int getDragUpDown() {
		return up_down;
	}
	public int getDragLeftRight() {
		return left_right;
	}
	public float getDeltaX() {
		return x-origX;
	}
	public float getDeltaY() {
		return y-origY;
	}
	
	public boolean isTap() {
		return (tapCount>0);
	}
	public boolean isDoubleTap() {
		return (lastEvent==Event.DOUBLETAP);
	}
	
}
