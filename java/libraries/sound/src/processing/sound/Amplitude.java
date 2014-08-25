package processing.sound;
import processing.core.*;

public class Amplitude {
	
	PApplet parent;
	private Engine m_engine;
	private long ptr;
	
	public Amplitude(PApplet theParent) {
		this.parent = theParent;
		parent.registerMethod("dispose", this);
		m_engine.setPreferences(theParent, 512, 44100);
    	m_engine.start();
	}
	
	public void input(SoundObject input){
		ptr = m_engine.amplitude(input.returnId());
	}
	
	public float analyze(){
		return m_engine.poll_amplitude(ptr);
	}
	/*
	public void stop(){
		m_engine.synthStop(m_nodeId);
	}
	
	public int returnId(){
		return m_nodeId;
	}
	*/
	public void dispose() {
		m_engine.destroy_amplitude(ptr);
		//m_engine.synthStop(m_nodeId);
	}
}
