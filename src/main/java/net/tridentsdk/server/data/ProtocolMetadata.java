/*
 * Trident - A Multithreaded Server Alternative
 * Copyright 2014 The TridentSDK Team
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package net.tridentsdk.server.data;

import com.google.common.collect.Iterators;
import com.google.common.collect.Lists;
import io.netty.buffer.ByteBuf;
import net.tridentsdk.server.netty.Codec;
import net.tridentsdk.util.Vector;

import javax.annotation.concurrent.GuardedBy;
import javax.annotation.concurrent.ThreadSafe;
import java.util.List;

@ThreadSafe
public class ProtocolMetadata implements Writable {
    @GuardedBy("metadata")
    private final List<MetadataValue> metadata = Lists.newLinkedList(() -> Iterators.forArray(new MetadataValue[22]));

    public void setMeta(int index, MetadataValue value) {
        synchronized (metadata) {
            metadata.add(index, value);
        }
    }

    public void setMeta(int index, MetadataType type, Object value) {
        synchronized (metadata) {
            metadata.add(index, new MetadataValue(index, value, type));
        }
    }

    public MetadataValue get(int index) {
        synchronized (metadata) {
            return metadata.get(index);
        }
    }

    public void remove(int index) {
        synchronized (metadata) {
            metadata.remove(index);
        }
    }

    @Override
    public void write(ByteBuf buf) {
        List<MetadataValue> localMeta;
        synchronized (metadata) {
            localMeta = metadata;
        }

        for(MetadataValue value : localMeta) {
            if (value == null) {
                continue;
            }

            buf.writeByte((value.type().id() << 5 | value.index & 0x1F) & 0xFF);

            switch(value.type) {
                case BYTE:
                    buf.writeByte((byte) value.value);
                    break;

                case SHORT:
                    buf.writeShort((short) value.value);
                    break;

                case INT:
                    buf.writeInt((int) value.value);
                    break;

                case FLOAT:
                    buf.writeFloat((float) value.value);
                    break;

                case STRING:
                    Codec.writeString(buf, (String) value.value);
                    break;

                case SLOT:
                    ((Slot) value.value).write(buf);
                    break;

                case XYZ:
                    Vector vector = (Vector) value.value;

                    buf.writeInt((int) vector.getX());
                    buf.writeInt((int) vector.getY());
                    buf.writeInt((int) vector.getZ());
                    break;

                case PYR:
                    Vector v = (Vector) value.value;

                    buf.writeFloat((float) v.getX());
                    buf.writeFloat((float) v.getY());
                    buf.writeFloat((float) v.getZ());

                    break;
            }
        }

        buf.writeByte(0x7F); // terminate array
    }

    public static class MetadataValue {
        private final int index;
        private final Object value;
        private final MetadataType type;

        public MetadataValue(int index, Object value, MetadataType type) {
            this.index = index;
            this.value = value;
            this.type = type;
        }

        public int index() {
            return index;
        }

        public Object value() {
            return value;
        }

        public MetadataType type() {
            return type;
        }
    }
}
