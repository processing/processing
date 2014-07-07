package processing.sound;
import processing.core.*;

public class Env {
	
	PApplet parent;
	private Engine m_engine;
	int m_nodeId;
		
	public Env (PApplet theParent) {	
		this.parent = theParent;
		parent.registerMethod("dispose", this);
		m_engine.setPreferences(theParent, 512, 44100);
    	m_engine.start();
   	}
	
	public void play(SoundObject input, float attackTime, float sustainTime, float sustainLevel, float releaseTime){
		m_nodeId = m_engine.envelopePlay(input.returnId(), attackTime, sustainTime, sustainLevel, releaseTime);
	}

	public int returnId(){
		return m_nodeId;
	}
	
	public void dispose(){
		//m_engine.synthStop(m_nodeId);
	}
};
