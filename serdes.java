import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.kafka.common.serialization.Serializer;

public class LookupInstrumentSerializer implements Serializer<LookupInstrument> {

    @Override
    public byte[] serialize(String topic, LookupInstrument data) {
        byte[] retVal = null;
        ObjectMapper objectMapper = new ObjectMapper();
        try {
            retVal = objectMapper.writeValueAsString(data).getBytes();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return retVal;
    }
}
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.kafka.common.serialization.Deserializer;

public class LookupInstrumentDeserializer implements Deserializer<LookupInstrument> {

    @Override
    public LookupInstrument deserialize(String topic, byte[] data) {
        ObjectMapper mapper = new ObjectMapper();
        LookupInstrument instrument = null;
        try {
            instrument = mapper.readValue(data, LookupInstrument.class);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return instrument;
    }
}
public class LookupInstrumentSerde implements Serde<LookupInstrument> {

    private final Serializer<LookupInstrument> serializer;
    private final Deserializer<LookupInstrument> deserializer;

    public LookupInstrumentSerde() {
        this.serializer = new LookupInstrumentSerializer();
        this.deserializer = new LookupInstrumentDeserializer();
    }

    @Override
    public Serializer<LookupInstrument> serializer() {
        return serializer;
    }

    @Override
    public Deserializer<LookupInstrument> deserializer() {
        return deserializer;
    }

    @Override
    public void configure(Map<String, ?> configs, boolean isKey) {
        serializer.configure(configs, isKey);
        deserializer.configure(configs, isKey);
    }

    @Override
    public void close() {
        serializer.close();
        deserializer.close();
    }
}


import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.kafka.common.serialization.Deserializer;

public class LookupInstrumentDeserializer implements Deserializer<LookupInstrument> {

    private Class<LookupInstrument> tClass;

    public LookupInstrumentDeserializer(Class<LookupInstrument> tClass) {
        this.tClass = tClass;
    }

    @Override
    public LookupInstrument deserialize(String topic, byte[] data) {
        ObjectMapper mapper = new ObjectMapper();
        LookupInstrument instrument = null;
        try {
            instrument = mapper.readValue(data, tClass);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return instrument;
    }
}
