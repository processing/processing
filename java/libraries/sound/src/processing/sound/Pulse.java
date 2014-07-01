package processing.sound;

import processing.core.PApplet;

public class Pulse implements SoundObject {
		
	PApplet parent;
	MethClaInterface m_engine;
	private int[] m_nodeId = {-1,-1};
	private float m_freq = 440;
	private float m_width = 0.5f;	
	private float m_amp = 0.5f;
	private float m_add = 0;
	private float m_pos = 0;
	
	public Pulse(PApplet theParent) {
		this.parent = theParent;
		parent.registerMethod("dispose", this);
		m_engine = new MethClaInterface();			
	}
	
	public void play(){
		m_nodeId = m_engine.pulsePlay(m_freq, m_width, m_amp, m_add, m_pos);
	};	
	
	public void play(float freq, float width, float amp, float add, float pos){
		m_freq=freq; m_width=width; m_amp=amp; m_add=add; m_pos=pos;
		this.play();
	};
	
	public void play(float freq, float width, float amp, float add){
		m_freq=freq; m_width=width; m_amp=amp; m_add=add;
		this.play();
	};
	
	public void play(float freq, float width, float amp){
		m_freq=freq; m_width=width; m_amp=amp;
		this.play();
	};
	
	public void play(float freq, float width){
		m_freq=freq; m_width=width;
		this.play();
	};
	
	public void play(float freq){
		m_freq=freq; 
		this.play();
	};	
	
	private void set(){
		if(m_nodeId[0] != -1 ) {
			m_engine.pulseSet(m_freq, m_width, m_amp, m_add, m_pos, m_nodeId);
		}	
	}
	
	public void set(float freq, float width, float amp, float add, float pos){
		m_freq=freq; m_width=width; m_amp=amp; m_add=add; m_pos=pos;
		this.set();
	};
	
	public void freq(float freq){
		m_freq=freq;
		this.set();		
	}
	
	public void width(float width){
		m_width=width;
		this.set();
	}
	
	public void amp(float amp){
		m_amp=amp;
		this.set();
	}
	
	public void add(float add){
		m_add=add;
		this.set();
	}
	
	public void pan(float pos){
		m_pos=pos;
		this.set();
	}
	
	public void stop(){
		m_engine.synthStop(m_nodeId);
	};
	
	public int[] returnId(){
		return m_nodeId;
	};
	
	public void dispose(){
		m_engine.synthStop(m_nodeId);
	};
}


