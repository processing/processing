package processing.sound;

import processing.core.PApplet;

public class SqrOsc implements SoundObject {
		
	PApplet parent;
	private Engine m_engine;
	private int[] m_nodeId = {-1,-1};
	private float m_freq = 440;
	private float m_amp = 0.5f;
	private float m_add = 0;
	private float m_pos = 0;
	
	public SqrOsc(PApplet theParent) {
		this.parent = theParent;
		parent.registerMethod("dispose", this);
		m_engine.setPreferences(theParent, 512, 44100);
    	m_engine.start();		
	}
	
	public void play(){
			m_nodeId = m_engine.sqrPlay(m_freq, m_amp, m_add-1, m_pos);
	};	
	
	public void play(float freq, float amp, float add, float pos){
		m_freq=freq; m_amp=amp; m_add=add; m_pos=pos;
		this.play();
	};
	
	public void play(float freq, float amp, float add){
		m_freq=freq; m_amp=amp; m_add=add;
		this.play();
	};
	
	public void play(float freq, float amp){
		m_freq=freq; m_amp=amp;
		this.play();
	};
	
	public void play(float freq){
		m_freq=freq; 
		this.play();
	};
	
	private void set(){
		if(m_nodeId[0] != -1 ) {
			m_engine.oscSet(m_freq, m_amp, m_add, m_pos, m_nodeId);
		}	
	}
	
	public void set(float freq, float amp, float add, float pos){
		m_freq=freq; m_amp=amp; m_add=add; m_pos=pos;
		this.set();
	};
	
	public void freq(float freq){
		m_freq=freq;
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


