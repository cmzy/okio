package Okio;

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import Okio.Buffer;
import Okio.BufferedSink;
import Okio.ByteString;
import Okio.RealBufferedSink;
import Okio.Segment;
import static Okio.TestUtil.repeat;
import static java.util.Arrays.asList;
import static org.junit.Assert.assertEquals;

@RunWith(Parameterized.class)
public class BufferedSinkTest {
  private interface Factory {
    BufferedSink create(Buffer data);
  }

  @Parameterized.Parameters(name = "{0}")
  public static List<Object[]> parameters() {
    return Arrays.asList(new Object[] {
        new Factory() {
          @Override public BufferedSink create(Buffer data) {
            return data;
          }

          @Override public String toString() {
            return "Buffer";
          }
        }
    }, new Object[] {
        new Factory() {
          @Override public BufferedSink create(Buffer data) {
            return new RealBufferedSink(data);
          }

          @Override public String toString() {
            return "RealBufferedSink";
          }
        }
    });
  }

  @Parameterized.Parameter
  public Factory factory;

  private Buffer data;
  private BufferedSink sink;

  @Before public void setUp() {
    data = new Buffer();
    sink = factory.create(data);
  }

  @Test public void writeNothing() throws IOException {
    sink.writeUtf8("");
    sink.flush();
    assertEquals(0, data.size());
  }

  @Test public void writeBytes() throws Exception {
    sink.writeByte(0xab);
    sink.writeByte(0xcd);
    sink.flush();
    assertEquals("Buffer[size=2 data=abcd]", data.toString());
  }

  @Test public void writeLastByteInSegment() throws Exception {
    sink.writeUtf8(repeat('a', Segment.SIZE - 1));
    sink.writeByte(0x20);
    sink.writeByte(0x21);
    sink.flush();
    assertEquals(asList(Segment.SIZE, 1), data.segmentSizes());
    assertEquals(repeat('a', Segment.SIZE - 1), data.readUtf8(Segment.SIZE - 1));
    assertEquals("Buffer[size=2 data=2021]", data.toString());
  }

  @Test public void writeShort() throws Exception {
    sink.writeShort(0xabcd);
    sink.writeShort(0x4321);
    sink.flush();
    assertEquals("Buffer[size=4 data=abcd4321]", data.toString());
  }

  @Test public void writeShortLe() throws Exception {
    sink.writeShortLe(0xabcd);
    sink.writeShortLe(0x4321);
    sink.flush();
    assertEquals("Buffer[size=4 data=cdab2143]", data.toString());
  }

  @Test public void writeInt() throws Exception {
    sink.writeInt(0xabcdef01);
    sink.writeInt(0x87654321);
    sink.flush();
    assertEquals("Buffer[size=8 data=abcdef0187654321]", data.toString());
  }

  @Test public void writeLastIntegerInSegment() throws Exception {
    sink.writeUtf8(repeat('a', Segment.SIZE - 4));
    sink.writeInt(0xabcdef01);
    sink.writeInt(0x87654321);
    sink.flush();
    assertEquals(asList(Segment.SIZE, 4), data.segmentSizes());
    assertEquals(repeat('a', Segment.SIZE - 4), data.readUtf8(Segment.SIZE - 4));
    assertEquals("Buffer[size=8 data=abcdef0187654321]", data.toString());
  }

  @Test public void writeIntegerDoesNotQuiteFitInSegment() throws Exception {
    sink.writeUtf8(repeat('a', Segment.SIZE - 3));
    sink.writeInt(0xabcdef01);
    sink.writeInt(0x87654321);
    sink.flush();
    assertEquals(asList(Segment.SIZE - 3, 8), data.segmentSizes());
    assertEquals(repeat('a', Segment.SIZE - 3), data.readUtf8(Segment.SIZE - 3));
    assertEquals("Buffer[size=8 data=abcdef0187654321]", data.toString());
  }

  @Test public void writeIntLe() throws Exception {
    sink.writeIntLe(0xabcdef01);
    sink.writeIntLe(0x87654321);
    sink.flush();
    assertEquals("Buffer[size=8 data=01efcdab21436587]", data.toString());
  }

  @Test public void writeLong() throws Exception {
    sink.writeLong(0xabcdef0187654321L);
    sink.writeLong(0xcafebabeb0b15c00L);
    sink.flush();
    assertEquals("Buffer[size=16 data=abcdef0187654321cafebabeb0b15c00]", data.toString());
  }

  @Test public void writeLongLe() throws Exception {
    sink.writeLongLe(0xabcdef0187654321L);
    sink.writeLongLe(0xcafebabeb0b15c00L);
    sink.flush();
    assertEquals("Buffer[size=16 data=2143658701efcdab005cb1b0bebafeca]", data.toString());
  }

  @Test public void writeSpecificCharset() throws Exception {
    sink.writeString("təˈranəˌsôr", Charset.forName("utf-32"));
    sink.flush();
    assertEquals(ByteString.decodeHex("0000007400000259000002c800000072000000610000006e00000259"
        + "000002cc00000073000000f400000072"), data.readByteString());
  }

  @Test public void writeAll() throws Exception {
    Buffer source = new Buffer().writeUtf8("abcdef");

    assertEquals(6, sink.writeAll(source));
    assertEquals(0, source.size());
    sink.flush();
    assertEquals("abcdef", data.readUtf8());
  }

  @Test public void writeAllExhausted() throws Exception {
    Buffer source = new Buffer();
    assertEquals(0, sink.writeAll(source));
    assertEquals(0, source.size());
  }

  @Test public void closeEmitsBufferedBytes() throws IOException {
    sink.writeByte('a');
    sink.close();
    assertEquals('a', data.readByte());
  }
}
