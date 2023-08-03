import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.kafka.common.serialization.Serializer;

public class JsonSerializer<T> implements Serializer<T> {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public byte[] serialize(String topic, T data) {
        if (data == null) {
            return null;
        }

        try {
            return objectMapper.writeValueAsBytes(data);
        } catch (Exception e) {
            throw new RuntimeException("Error serializing JSON message", e);
        }
    }
}
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.kafka.common.serialization.Deserializer;

public class JsonDeserializer<T> implements Deserializer<T> {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private Class<T> tClass;

    public JsonDeserializer(Class<T> tClass) {
        this.tClass = tClass;
    }

    @Override
    public T deserialize(String topic, byte[] data) {
        if (data == null) {
            return null;
        }

        try {
            return objectMapper.readValue(data, tClass);
        } catch (Exception e) {
            throw new RuntimeException("Error deserializing JSON message", e);
        }
    }
}
import org.apache.kafka.common.serialization.Serde;
import org.apache.kafka.common.serialization.Serdes;

public class JsonSerdeFactory {

    public static <T> Serde<T> createJsonSerde(Class<T> classOfT) {
        return Serdes.serdeFrom(new JsonSerializer<>(), new JsonDeserializer<>(classOfT));
    }

    public static Serde<LookupInstrument> getLookupInstrumentSerde() {
        return createJsonSerde(LookupInstrument.class);
    }

    public static Serde<Transaction> getTransactionSerde() {
        return createJsonSerde(Transaction.class);
    }
}
