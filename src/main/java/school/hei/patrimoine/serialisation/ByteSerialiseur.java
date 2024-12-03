package school.hei.patrimoine.serialisation;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Base64;
import lombok.SneakyThrows;

/* note(no-serializable): Serializable is highly deprecated by
 * Effective Java 3rd Edition Item 86.
 * But it's also the less verbose solution. */
public final class ByteSerialiseur<T> extends Serialiseur<T> {
  @SneakyThrows
  @Override
  public String serialise(T object) {
    try (var bos = new ByteArrayOutputStream();
        ObjectOutputStream oos = new ObjectOutputStream(bos)) {
      oos.writeObject(object);
      return Base64.getEncoder().encodeToString(bos.toByteArray());
    }
  }

  @SneakyThrows
  @Override
  public T deserialise(String serialisé) {
    var bytes = Base64.getDecoder().decode(serialisé);
    try (var bis = new ByteArrayInputStream(bytes);
        var ois = new ObjectInputStream(bis)) {
      return (T) ois.readObject();
    }
  }
}
