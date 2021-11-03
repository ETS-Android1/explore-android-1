package com.mentalab;

import android.util.Log;
import com.mentalab.MentalabConstants.Command;
import com.mentalab.exception.InvalidCommandException;
import com.mentalab.exception.InvalidDataException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MentalabCodec {

  private static final String TAG = "Explore";
  private static final int NTHREADPOOL = 100;
  private static final ExecutorService executor = Executors.newFixedThreadPool(NTHREADPOOL);

  public static Map<String, Queue<Float>> decodedDataMap = null;

  /**
   * Decodes a device raw data stream
   *
   * <p>Incoming bytes from Bluetooth are converted to an immutable Map of Double Ended Queue of
   * Float numbers. ExG channels are saved as single precision floating point numbers (Float) in the
   * unit of mVolt. Launches one worker thread on first invocation. Currently the it provides the
   * following data queues from Explore device: Channel_1, Channel_2...Channel_N where N is the maximum
   * available numbers of channel of the device. Acc_X, Acc_Y, Acc_Z in the units of mg/LSB. Gyro_X,
   * Gyro_Y and Gyro_Z in mdps/LSB. MAG_X, Mag_Y, Mag_Z in mgauss/LSB. To get a specific instance of
   * the queue:
   * <pre>{@code
   * Map<String, Queue<Float>> map = MentalabCodec.decode(stream);
   * Queue<Float> accXMap = map.get("Acc_X").poll();
   * Queue<Float> channel2 = map.get("Channel2").poll();
   * }<pre>
   *
   * @throws InvalidDataException throws when invalid data is received
   * @stream InputStream of device bytes
   * @return Immutable Map of Queues of Numbers
   */
  public static Map<String, Queue<Float>> decode(InputStream stream) throws InvalidDataException {

    executor.execute(new ConnectedThread(stream));
    Log.d(TAG, "Started execution of decoder!!");
    //    ConnectedThread thread = new ConnectedThread(stream);
    //    thread.start();
    return decodedDataMap;
  }

  /**
   * Encodes a command
   *
   * @throws InvalidCommandException when the command is not recognized
   * @return byte[] encoded commands that can be sent to the device
   */
  static byte[] encodeCommand(Command command, int extraArguments) throws InvalidCommandException {

    CommandTranslator translator = command.createInstance(command, extraArguments);
    byte[] translatedBytes = translator.translateCommand(extraArguments);
    return translatedBytes; // Some example while stub
  }

  private static void parsePayloadData(int pId, double timeStamp, byte[] byteBuffer)
      throws InvalidDataException {

    for (Packet.PacketId packetId : Packet.PacketId.values()) {
      if (packetId.getNumVal() == pId) {
        Log.d(TAG, "Converting data for Explore");
        Packet packet = packetId.createInstance(timeStamp);
        if (packet != null) {
          packet.convertData(byteBuffer);
          Log.d(TAG, "Data decoded is " + packet.toString());
          pushDataInQueue(packet);
        }
      }
    }
  }
  // TODO refactor packet class to expose uniform methods
  private static void pushDataInQueue(Packet packet) {

    if (packet instanceof DataPacket) {
      DataPacket dataPacket = (DataPacket) packet;
      int channelCount = dataPacket.getDataCount();

      for (int index = 0; index < channelCount; index++) {
        synchronized (decodedDataMap) {
          ArrayList<Float> convertedSamples = ((DataPacket) packet).getData();
          String channelKey = "Channel_" + String.valueOf(index + 1);
          if (decodedDataMap.get(channelKey) == null) {
            decodedDataMap.put(channelKey, new ConcurrentLinkedDeque<>());
          }

          ConcurrentLinkedDeque<Float> floats =
              (ConcurrentLinkedDeque) decodedDataMap.get(channelKey);
          floats.offerFirst(((DataPacket) packet).convertedSamples.get(index));
        }
      }
      // Lsl Packet Subscriber implementation
      PubSubManager.getInstance().publish("ExG", packet);

    } else if (packet instanceof InfoPacket) {

      int channelCount = packet.getDataCount();

      for (int index = 0; index < channelCount; index++) {
        synchronized (decodedDataMap) {
          String channelKey = ((InfoPacket) packet).attributes.get(index);
          if (decodedDataMap.get(channelKey) == null) {
            decodedDataMap.put(channelKey, new ConcurrentLinkedDeque<>());
          }
          ConcurrentLinkedDeque<Float> floats =
              (ConcurrentLinkedDeque) decodedDataMap.get(channelKey);
          floats.offerFirst(((InfoPacket) packet).convertedSamples.get(index));
        }
      }
      if (packet instanceof Orientation) {
        PubSubManager.getInstance().publish("Orn", packet);
      }

      if (packet instanceof MarkerPacket) {
        PubSubManager.getInstance().publish("Marker", packet);
      }

      if (packet instanceof CommandStatusPacket || packet instanceof AckPacket || packet instanceof CommandReceivedPacket) {
        PubSubManager.getInstance().publish("Marker", packet);
      }
    }
  }

  // TODO Decouple executor class from Codec class
  public static void pushToLsl() {
    executor.execute(new LslPacketSubscriber());
  }

  private static class ConnectedThread extends Thread {
    private final InputStream mmInStream;

    public ConnectedThread(InputStream inputStream) {
      mmInStream = inputStream;
      initializeMapInstance();
    }

    public void run() {

      int pId = 0;
      while (true) {
        try {
          byte[] buffer = new byte[1024];
          // reading PID
          mmInStream.read(buffer, 0, 1);
          pId = ByteBuffer.wrap(buffer).order(java.nio.ByteOrder.LITTLE_ENDIAN).getInt();
          Log.d(TAG, "pid .." + pId);
          buffer = new byte[1024];

          // reading count
          mmInStream.read(buffer, 0, 1);
          int count = ByteBuffer.wrap(buffer).order(java.nio.ByteOrder.LITTLE_ENDIAN).getInt();
          buffer = new byte[1024];

          // reading payload
          mmInStream.read(buffer, 0, 2);
          int payload = ByteBuffer.wrap(buffer).order(java.nio.ByteOrder.LITTLE_ENDIAN).getInt();
          buffer = new byte[1024];

          // reading timestamp
          mmInStream.read(buffer, 0, 4);
          double timeStamp =
              ByteBuffer.wrap(buffer).order(java.nio.ByteOrder.LITTLE_ENDIAN).getInt();

          Log.d(TAG, "pid .." + pId + " payload is : " + payload);

          // reading payload data
          buffer = new byte[payload - 4];
          int read = mmInStream.read(buffer, 0, buffer.length);
          Log.d(TAG, "reading count is ...." + read);
          // parsing payload data

          parsePayloadData(pId, timeStamp, Arrays.copyOfRange(buffer, 0, buffer.length - 4));

        } catch (IOException | InvalidDataException exception) {
          exception.printStackTrace();
          break;
        }
      }
    }

    void initializeMapInstance() {

      if (decodedDataMap == null) {
        decodedDataMap = new HashMap<>();
      }
    }
  }

  synchronized static ExecutorService getExecutorService(){
    return executor;
  }
}
