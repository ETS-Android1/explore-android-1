package com.mentalab;

public class MentalabConstants {

  interface QueueAttribute {
    enum OrientationAttribute {
      ACC_X,
      ACC_Y,
      Acc_Z,
      Mag_X,
      Mag_Y,
      Mag_Z,
      Gyro_X,
      Gyro_Y,
      Gyro_Z;
    }

    enum ExgChannel {
      CHANNEL_0,
      CHANNEL_1,
      CHANNEL_2,
      CHANNEL_3,
      CHANNEL_4,
      CHANNEL_5,
      CHANNEL_6,
      CHANNEL_7;
    }

    enum DeviceInfoAttribute {
      FIRMWARE_VERSION,
      SAMPLING_RATE,
      ADS_MASK;
    }
  }

  public enum SamplingRate {
    // enum fields
    SR_250(0xA1),
    SR_500(0xA2),
    SR_1000(0xA3);

    // constructor
    private SamplingRate(final int samplingRate) {
      this.samplingRate = samplingRate;
    }

    // internal state
    private int samplingRate;

    public int getValue() {
      return samplingRate;
    }
  }

  public enum Command {
    // enum fields
    CMD_SAMPLING_RATE_SET(0xA1){
      @Override
      public CommandTranslator createInstance(Command command, int extraArguments) {
        return new SamplingRateTranslator(command.getValue(), extraArguments);
      }
    },
    CMD_CHANNEL_SET(0xA2){
      @Override
      public CommandTranslator createInstance(Command command, int extraArguments) {
        return null;
      }
    },
    CMD_MEMORY_FORMAT(0xA3){
      @Override
      public CommandTranslator createInstance(Command command, int extraArguments) {
        return null;
      }
    },
    CMD_REC_TIME_SET(0xB1){
      @Override
      public CommandTranslator createInstance(Command command, int extraArguments) {
        return null;
      }
    },
    CMD_MODULE_DISABLE(0xA4){
      @Override
      public CommandTranslator createInstance(Command command, int extraArguments) {
        return null;
      }
    },
    CMD_MODULE_ENABLE(0xA5){
      @Override
      public CommandTranslator createInstance(Command command, int extraArguments) {
        return null;
      }
    },
    CMD_ZM_DISABLE(0xA6){
      @Override
      public CommandTranslator createInstance(Command command, int extraArguments) {
        return null;
      }
    },
    CMD_ZM_ENABLE(0xA7){
      @Override
      public CommandTranslator createInstance(Command command, int extraArguments) {
        return null;
      }
    },
    CMD_SOFT_RESET(0xA8){
      @Override
      public CommandTranslator createInstance(Command command, int extraArguments) {
        return null;
      }
    };

    // constructor
    private Command(final int opCode) {
      this.opCode = opCode;
    }

    // internal state
    private int opCode;

    public int getValue() {
      return opCode;
    }

    public abstract CommandTranslator createInstance(Command command, int extraArguments);
  }

  /** Topics available for Publisher Subscriber manager */
  enum Topic {
    ExG,
    Orn,
    Marker;
  }
}
