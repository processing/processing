package processing.sound;

import processing.core.PApplet;

public class Reverb implements SoundObject{
	
	PApplet parent;
	private Engine m_engine;
	private int[] m_nodeId = {-1, -1};
	private float m_room = 1;
	private float m_damp = 0;
	private float m_wet = 0.5f;
	
	public Reverb(PApplet theParent) {
		this.parent = theParent;
		parent.registerMethod("dispose", this);
		m_engine.setPreferences(theParent, 512, 44100);
    	m_engine.start();
   	}
	
	public void process(SoundObject input, float room, float damp, float wet){
		m_room=room; m_damp=damp; m_wet=wet;
		m_nodeId = m_engine.reverbPlay(input.returnId(), m_room, m_damp, m_wet);
	}
	
	public void process(SoundObject input, float room, float damp){
		m_room=room; m_damp=damp;
		m_nodeId = m_engine.reverbPlay(input.returnId(), m_room, m_damp, m_wet);
	}

	public void process(SoundObject input, float room){
		m_room=room;
		m_nodeId = m_engine.reverbPlay(input.returnId(), m_room, m_damp, m_wet);
	}

	public void process(SoundObject input){
		m_nodeId = m_engine.reverbPlay(input.returnId(), m_room, m_damp, m_wet);
	}	
	
	private void set(){
		m_engine.reverbSet(m_room, m_damp, m_wet, m_nodeId[0]);
	}
	
	public void set(float room, float damp, float wet){
		m_room=room; m_damp=damp; m_wet=wet;
		this.set();
	}
	
	public void room(float room){
		m_room=room;
		this.set();
	}

	public void damp(float damp){
		m_damp=damp;
		this.set();
	}
	
	public void wet(float wet){
		m_wet=wet;
		this.set();
	}

	public int[] returnId(){
		return m_nodeId;
	}
	
	public void stop(){
		m_engine.synthStop(m_nodeId);
	}

	public void dispose() {
		m_engine.synthStop(m_nodeId);
	}
}
