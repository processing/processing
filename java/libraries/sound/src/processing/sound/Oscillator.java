package processing.sound;
 
public interface Oscillator extends SoundObject {
	
	public void play(float freq, float amp, float add, float pos);
	public void play(float freq, float amp, float add);
	public void play(float freq, float amp);
	public void play();

	public void freq(float freq);
	public void amp(float amp);
	public void add(float add);
	public void pan(float pos);
	public void set(float freq, float amp, float add, float pan);
	public void stop();
	public void dispose();
	
	public int[] returnId();
}
