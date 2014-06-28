package processing.sound;

import processing.core.PApplet;

public class AudioIn {
	
	PApplet parent;
	private MethClaInterface m_engine;
	private int[] m_nodeId = {-1,-1};
	private float m_freq = 0;
	private float m_amp = 0;
	private float m_add = 0;
	private float m_pos = 0;
	
	public AudioIn(PApplet theParent) {
		this.parent = theParent;
		parent.registerMethod("dispose", this);
		m_engine = new MethClaInterface();		
	}
	
	public void play(float freq, float amp, float add, float pos){
		m_freq=freq; m_amp=amp; m_add=add; m_pos=pos;
		m_nodeId = m_engine.sawPlay(m_freq, m_amp, m_add, m_pos);
	}
	
	public void play(float freq, float amp, float add){
		m_freq=freq; m_amp=amp; m_add=add;
		m_nodeId = m_engine.sawPlay(m_freq, m_amp, m_add, m_pos);
	}
	
	public void play(float freq, float amp){
		m_freq=freq; m_amp=amp;
		m_nodeId = m_engine.sawPlay(m_freq, m_amp, m_add, m_pos);
	}
	
	public void play(){
		m_nodeId = m_engine.sawPlay(m_freq, m_amp, m_add, m_pos);
	}
	
	private void set(){
		m_engine.oscSet(m_freq, m_amp, m_add, m_pos, m_nodeId);
	}
	
	public void set(float freq, float amp, float add, float pos){
		m_freq=freq; m_amp=amp; m_add=add; m_pos=pos;
		this.set();
	}
	
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
	}
	
	public int[] returnId(){
		return m_nodeId;
	}

	public void dispose() {
		m_engine.synthStop(m_nodeId);
	}
}
