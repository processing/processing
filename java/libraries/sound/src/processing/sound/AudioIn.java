package processing.sound;

import processing.core.PApplet;

public class AudioIn {
	
	PApplet parent;
	private Engine m_engine;
	private int[] m_nodeId = {-1,-1};
	private float m_amp = 1.f;
	private float m_add = 0;
	private boolean m_out = true;
	private float m_pos = 0;
	
	public AudioIn(PApplet theParent) {
		this.parent = theParent;
		parent.registerMethod("dispose", this);
		m_engine.setPreferences(theParent, 512, 44100);
    	m_engine.start();
   	}
	
	public void play(){
		m_nodeId = m_engine.audioInPlay(m_amp, m_add, m_pos, m_out);
	}

	public void play(float amp, float add, float pos){
		m_amp=amp; m_add=add; m_pos=pos;
		this.play();
	}
	
	public void play(float amp, float add){
		m_amp=amp; m_add=add;
		this.play();
	}
	
	public void play(float amp){
		m_amp=amp;
		this.play();
	}
	
	private void set(){
		m_engine.audioInSet(m_amp, m_add, m_pos, m_nodeId);
	}
	
	public void set(float amp, float add, float pos){
		m_amp=amp; m_add=add; m_pos=pos;
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
