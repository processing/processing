//Daniel Shiffman
//The Nature of Code, Fall 2006
//Neural Network

//Generic Neuron Class
//Can be a bias neuron (true or false)

package nn;

import java.util.ArrayList;

public class Neuron {

    protected float output;
    protected ArrayList connections; 
    protected boolean bias = false;

    // A regular Neuron
    public Neuron() {
        output = 0;
        // Using an arraylist to store list of connections to other neurons
        connections = new ArrayList();  
        bias = false;
    }

    // Constructor for a bias neuron
    public Neuron(int i) {
        output = i;
        connections = new ArrayList();
        bias = true;
    }

    // Function to calculate output of this neuron
    // Output is sum of all inputs*weight of connections
    public void calcOutput() {
        if (bias) {
            // do nothing
        } else {
            float sum = 0;
            float bias = 0;
            //System.out.println("Looking through " + connections.size() + " connections");
            for (int i = 0; i < connections.size(); i++) {
                Connection c = (Connection) connections.get(i);
                Neuron from = c.getFrom();
                Neuron to = c.getTo();
                // Is this connection moving forward to us
                // Ignore connections that we send our output to
                if (to == this) {
                    // This isn't really necessary
                    // But I am treating the bias individually in case I need to at some point
                    if (from.bias) {
                        bias = from.getOutput()*c.getWeight();
                    } else {
                        sum += from.getOutput()*c.getWeight();
                    }
                }
            }
            // Output is result of sigmoid function
            output = f(bias+sum);
        }
    }

    void addConnection(Connection c) {
        connections.add(c);
    }

    float getOutput() {
        return output;
    }

    // Sigmoid function
    public static float f(float x) {
        return 1.0f / (1.0f + (float) Math.exp(-x));
    }

    public ArrayList getConnections() {
        return connections;
    }


}
