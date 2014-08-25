package galsasson.mode.tweak;

import java.net.InetAddress;
import java.util.ArrayList;

import com.illposed.osc.*;

public class OSCSender {
	
	public static void sendFloat(int index, float val, int port) throws Exception
	{
		OSCPortOut sender = new OSCPortOut(InetAddress.getByName("localhost"), port);
		ArrayList<Object> args = new ArrayList<Object>();
		args.add(Integer.valueOf(index));
		args.add(Float.valueOf(val));
		OSCMessage msg = new OSCMessage("/tm_change_float", args);
		 try {
			sender.send(msg);
		 } catch (Exception e) {
			 System.out.println("TweakMode: error sending new value of float " + index);
		 }
	}
	
	public static void sendInt(int index, int val, int port) throws Exception
	{
		OSCPortOut sender = new OSCPortOut(InetAddress.getByName("localhost"), port);
		ArrayList<Object> args = new ArrayList<Object>();
		args.add(Integer.valueOf(index));
		args.add(Integer.valueOf(val));
		OSCMessage msg = new OSCMessage("/tm_change_int", args);
		 try {
			sender.send(msg);
		 } catch (Exception e) {
			 System.out.println("TweakMode: error sending new value of int " + index);
			 System.out.println(e.toString());
		 }
	}

	public static void sendLong(int index, long val, int port) throws Exception
	{
		OSCPortOut sender = new OSCPortOut(InetAddress.getByName("localhost"), port);
		ArrayList<Object> args = new ArrayList<Object>();
		args.add(Integer.valueOf(index));
		args.add(Long.valueOf(val));
		OSCMessage msg = new OSCMessage("/tm_change_long", args);
		 try {
			sender.send(msg);
		 } catch (Exception e) {
			 System.out.println("TweakMode: error sending new value of long " + index);
			 System.out.println(e.toString());
		 }
	}
}
