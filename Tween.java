package com.twopisoftware.yukonsolitaire;

import java.util.ArrayList;

import android.os.SystemClock;
import android.util.Log;


/*****************
 * 
 * @author Adam Piette
 * 
 * 
 * Usage:
 * UpdateAllTweens() must be called per update tick
 * use .x and .y variables from the Tween instance
 *
 * Example:
 *  Tween mytween = new Tween(Tween.EASEOUT, TIME, card.x, card.y, px, py, Tween.ONCE)
 *
 * Add more equations using the TweenEquation interface.
 *
 */
 

interface TweenEquation {
	public void update(int ms);
}

public class Tween {
	
	//tween equations
	final static int CONSTANT=1;
	final static int EASEOUT=2;
	final static int EASEIN=3;
	final static int EASEINOUT=4;
	
	//end states
	final static int ONCE=64;
	final static int REPEAT=65;
	final static int REVERSE=66;
	
	//use this for quick access
	float x, y;
	
	private float sx, sy, ex, ey, time; //start end
	private float invTick; //helper for lerping
	private TweenEquation equation;
	private int type, endType;
	private int msTick, maxTick;
	
	private static ArrayList<Tween> tweenList;
	private int listLink=-1;
	private static ArrayList<Integer> killList;
	private boolean needsToStop=false;
	
	private Tween compTween, nextTween;
	
	//private static Object tweenMutex = new Object();
	
	static {
		tweenList= new ArrayList<Tween>();
		killList = new ArrayList<Integer>();
	}
	
	//new tween
	Tween(int type, float time, float startx, float starty, float endx, float endy, int endType) {
		Create(type,time,startx,starty,endx,endy,endType);
		this.start();
	}
	
	Tween() {
		//empty constructor
	}
	
	//copy constructor
	//take old tween variables if not finished
	Tween(Tween t) {
		this.Copy(t);
	}
	
	public void Copy(Tween t) {
		x=t.x; y=t.y;
		sx=t.sx;sy=t.sy;ex=t.ex; ey=t.ey;
		this.type=t.type; this.endType=t.endType;
		this.equation = getEquation(t.type);
		
		this.msTick=t.msTick; //pick up where this left off
		this.time = t.time;
		this.maxTick=t.maxTick;
		this.invTick = t.invTick;
		//this.setTimeStep();
		compTween = t.compTween;
		nextTween = t.nextTween;
		
		this.start();
	}
	
	//-- Create() over existing object
	public void Create(int type, float time, float startx, float starty, float endx, float endy, int endType) {
		sx=startx; sy=starty;
		x=startx; y=starty;
		ex=endx-startx; ey=endy-starty; //end relative to start
		this.equation = getEquation(type);
		this.type=type; this.endType=endType;
		this.time = time;
		
		this.msTick=(int)SystemClock.elapsedRealtime();
		maxTick = (int)(time*1000.0f)+this.msTick;
		invTick = 1.0f/(time*1000.0f);
		
		compTween = null;
		nextTween = null;
		
		this.start();
	}
	
	//-- base tween can start/stop these added tweens
	//-- will auto-start
	//-- only one comp tween allowed, but comptweens can be chained
	public void addTween(int type, float time, float startx, float starty, float endx, float endy, int endType) {
		if (compTween==null) compTween = new Tween();
		compTween.Create(type,time,startx,starty,endx,endy,endType);
		compTween.start(this.msTick);
	}
	
	//-- shortcut for keeping values of parent tween, and uses only start/end values
	//-- only one comp tween allowed, but comptweens can be chained
	public void addTween(float startx, float starty, float endx, float endy) {
		if (compTween==null) compTween = new Tween();
		compTween.Create(this.type,this.time,startx,starty,endx,endy,this.endType);
		compTween.start(this.msTick);
	}
	
	//-- add a new tween to the end of current tween, tween chaining
	public void addNextTween(int type, float time, float startx, float starty, float endx, float endy, int endType) {
		
		Tween cur = this;
		while (cur.nextTween !=null) {
			cur = cur.nextTween;
		}
		cur.nextTween = new Tween( type,  time,  startx,  starty,  endx,  endy,  endType);
		cur.nextTween.stop();
		
	}
	
	//-- overwrites tween using current values as start values
	public void tweenTo(int type, float time, float endx, float endy, int endType) {
		Create(type, time, x, y, endx, endy, endType);
	}
	
	//-- shortcut using original tween type and end state
	public void tweenTo(float time, float endx, float endy) {
		Create(this.type, time, x, y, endx, endy, this.endType);
	}
	
	synchronized public static void UpdateAllTweens() {
		
		int ms = (int)SystemClock.elapsedRealtime();
		
		//synchronized (tweenMutex) {
			if ( tweenList.isEmpty() ) return;
			
			Tween tw;
			int sz = tweenList.size();
			
			for (int i=0; i<sz; i++) {
				
				//our list size can change dynamically with addon tweens, which are recursive
				if (i>=tweenList.size()) break;
				
				tw = tweenList.get(i);
				if (tw==null) continue;
				if (tw.listLink == -1) continue;
				
				//instant update for 0.0 time
				if (tw.time==0.0f) {
					tw.x=tw.ex; tw.y=tw.ey;
					tw.stop();
				}
				
				if (ms>=tw.maxTick) {
					//update to final position and remove
					tw.equation.update(tw.maxTick);
					if ( tw.endType == Tween.REPEAT ) {
						tw.repeat();
					} if ( tw.endType == Tween.REVERSE ) {
						tw.reverse();
					} else {
						tw.stop();
						
						//handle last compTween update **currently only handles one, no recursion?
						if(tw.compTween!=null) {
							tw.compTween.equation.update(tw.maxTick);
						}
						
						//handle chained tweens
						if (tw.nextTween !=null) {
							tw.Copy(tw.nextTween);
						}
					}
				} else{
					tw.equation.update(ms);
				}
				
				
			}
			
			for (Integer k: killList) {
				tweenList.remove(k);
			}
			killList.clear();
		//}
	}
	
	public static int numActiveTweens() { return tweenList.size(); }
	
	public float getX() { return x; }
	public float getY() { return y; }
	
	//tween composites used for simultaneous tweening values, controlled by the main tween
	public Tween getComp() {return compTween; }
	public float getCompX() { return (compTween!=null)?compTween.x:0.0f; }
	public float getCompY() { return (compTween!=null)?compTween.y:0.0f; }
	
	
	synchronized public void start() {
		x=sx; y=sy;
		this.msTick=(int)SystemClock.elapsedRealtime();
		maxTick = (int)(time*1000.0f)+this.msTick;
		invTick = 1.0f/(time*1000.0f);
		if (listLink==-1) {
			tweenList.add(this);
			listLink = tweenList.indexOf(this);
		}
		
		//recursive addons
		if (compTween!=null) {
			compTween.start(this.msTick);
		}
	}
	
	synchronized public void start(int syncTime) {
		x=sx; y=sy;

		maxTick = (int)(time*1000.0f)+syncTime;
		invTick = 1.0f/(time*1000.0f);
		if (listLink==-1) {
			tweenList.add(this);
			listLink = tweenList.indexOf(this);
		}
		
		//recursive addons
		if (compTween!=null) {
			compTween.start();
		}
	}
	
	synchronized public void resume() {
		//TODO
	}
	
	synchronized public void stop() {
		//handle compTweens
		if (compTween !=null) {
			compTween.stop();
		}
		
		if (listLink != -1) killList.add((int)listLink);
		listLink=-1;

	}
	
	public void repeat() {
		msTick=(int)SystemClock.elapsedRealtime();
		maxTick = (int)(time*1000.0f)+msTick;
		invTick = 1.0f/(time*1000.0f);
		
		if (compTween !=null) compTween.repeat();
	}
	
	public void reverse() {
		repeat();
		float tx, ty;
		tx = sx; ty=sy;
		sx = sx+ex; sy=sy+ey;
		ex=tx-sx; ey=ty-sy;
		x=sx; y=sy;
		
		if (compTween !=null) compTween.reverse();
	}
	
	
	
	public boolean isAnimating() {
		//-- not in the list, then it's not animating
		return (listLink>=0);
	}
	

	public TweenEquation getEquation(int type) {
		
		if (type==CONSTANT) {
			return new EquConstant();
		} else if (type==EASEOUT) {
			return new EquEaseOut();
		} else if (type==EASEIN) {
			return new EquEaseIn();
		} else if (type==EASEINOUT) {
			return new EquEaseInOut();
		}
		return null;
	}
	
	class EquConstant implements TweenEquation {
		public void update(int ms) {
			//time based LERP!
			float j = 1.0f-(maxTick-ms)*invTick;
			x=sx+ex*(j);
			y=sy+ey*(j);
		}
	}
	
	//quadratic
	class EquEaseOut implements TweenEquation {
		public void update(int ms) {
			float j = 1.0f-(maxTick-ms)*invTick;
			x=sx-ex*(j)*(j-2.0f);
			y=sy-ey*(j)*(j-2.0f);
		}
	}
	
	//quadratic
	class EquEaseIn implements TweenEquation {
		public void update(int ms) {
			float j = 1.0f-(maxTick-ms)*invTick;
			x=sx+ex*(j)*(j);
			y=sy+ey*(j)*(j);
		}
	}
	
	class EquEaseInOut implements TweenEquation {
		public void update(int ms) {
			float j = 1.0f-(maxTick-ms)*invTick;
			
			//0.5f is halfway
			if (j < 0.5f) {
				x=sx+ex*(j)*(j);
				y=sy+ey*(j)*(j);
			} else {
				x=sx-ex*(j)*(j-2.0f);
				y=sy-ey*(j)*(j-2.0f);
			}
		}
	}
}


