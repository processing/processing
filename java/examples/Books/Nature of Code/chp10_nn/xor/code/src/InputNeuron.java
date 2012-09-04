//Daniel Shiffman
//The Nature of Code, Fall 2006
//Neural Network

// Input Neuron Class
// Has additional functionality to receive beginning input

package nn;

public class InputNeuron extends Neuron {
    public InputNeuron() {
        super();
    }
    
    public InputNeuron(int i) {
        super(i);
    }

    public void input(float d) {
        output = d;
    }

}
