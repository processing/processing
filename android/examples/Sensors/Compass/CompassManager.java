import java.lang.reflect.*;
import java.util.List;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;


public class CompassManager {
  private Sensor sensor;
  private SensorManager sensorManager;

  Method compassEventMethod;
  Method directionEventMethod;

  private Boolean supported;
  private boolean running = false;

  Context context;


  public CompassManager(Context parent) {
    this.context = parent;

    try {
      compassEventMethod =
        parent.getClass().getMethod("compassEvent", new Class[] { Float.TYPE, Float.TYPE, Float.TYPE });
    } catch (Exception e) {
      // no such method, or an error.. which is fine, just ignore
    }
    try {
      directionEventMethod =
        parent.getClass().getMethod("directionEvent", new Class[] { Float.TYPE });
    } catch (Exception e) {
      // no such method, or an error.. which is fine, just ignore
    }
//    System.out.println("directionEventMethod is " + directionEventMethod);

    resume();
  }


  public void resume() {
    if (isSupported()) {
      startListening();
    }
  }


  public void pause() {
    if (isListening()) {
      stopListening();
    }
  }


  /**
   * Returns true if the manager is listening to orientation changes
   */
  public boolean isListening() {
    return running;
  }


  /**
   * Unregisters listeners
   */
  public void stopListening() {
    running = false;
    try {
      if (sensorManager != null && sensorEventListener != null) {
        sensorManager.unregisterListener(sensorEventListener);
      }
    }
    catch (Exception e) {
    }
  }


  /**
   * Returns true if at least one Accelerometer sensor is available
   */
  public boolean isSupported() {
    if (supported == null) {
      sensorManager = (SensorManager) context.getSystemService(Context.SENSOR_SERVICE);
      List<Sensor> sensors = sensorManager.getSensorList(Sensor.TYPE_ORIENTATION);
      supported = new Boolean(sensors.size() > 0);
    }
    return supported;
  }


  public void startListening() {
    sensorManager = (SensorManager) context.getSystemService(Context.SENSOR_SERVICE);
    List<Sensor> sensors = sensorManager.getSensorList(Sensor.TYPE_ORIENTATION);
    if (sensors.size() > 0) {
      sensor = sensors.get(0);
      running = sensorManager.registerListener(sensorEventListener, sensor, SensorManager.SENSOR_DELAY_GAME);
    }
  }


  /**
   * The listener that listen to events from the accelerometer listener
   */
  private SensorEventListener sensorEventListener = new SensorEventListener() {

    public void onAccuracyChanged(Sensor sensor, int accuracy) {
      // ignored for now
    }

    public void onSensorChanged(SensorEvent event) {
      float x = event.values[0];
      float y = event.values[1];
      float z = event.values[2];

      if (compassEventMethod != null) {
        try {
          compassEventMethod.invoke(context, new Object[] { x, y, z });
        } catch (Exception e) {
          e.printStackTrace();
          compassEventMethod = null;
        }
      }

      if (directionEventMethod != null) {
        try {
          directionEventMethod.invoke(context, new Object[] { (float) (-x * Math.PI / 180) });
        } catch (Exception e) {
          e.printStackTrace();
          directionEventMethod = null;
        }
      }
    }
  };
}

