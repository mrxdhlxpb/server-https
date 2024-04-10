/*
 *  Copyright (C) 2024 mrxdhlxpb
 *
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package personal.mrxdhlxpb.server.https;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * This subclass of {@code BufferedInputStream} provides additional capability to record bytes read,
 * which are stored in a {@link ByteArrayOutputStream}.
 * <p> Whether to enable the recording capability can be decided at the stage of object creation.
 * If enabled, you may reset the recording with {@code resetRecording()} or obtain the recorded
 * byte array with {@code getRecordingByteArray()}. If not enabled, invocation of the two methods
 * throws {@link NullPointerException}.
 *
 * @author mrxdhlxpb
 */
public class RecordingBufferedInputStream extends BufferedInputStream {

    /**
     * whether the recording capability has been enabled
     */
    protected final boolean recordingEnabled;

    protected final ByteArrayOutputStream recording;

    private RecordingBufferedInputStream(InputStream underlyingInputStream,
                                         boolean recordingEnabled,
                                         ByteArrayOutputStream recording) {
        super(underlyingInputStream);
        this.recordingEnabled = recordingEnabled;
        this.recording = recording;
    }

    /**
     * Constructs a {@code RecordingBufferedInputStream} without recording capability.
     * @param in the underlying input stream
     */
    public RecordingBufferedInputStream(InputStream in) {
        this(in, false, null);
    }

    /**
     * Constructs a {@code RecordingBufferedInputStream} with recording capability enabled.
     * The initial buffer size of {@code recording} will be 32 bytes.
     * @param in the underlying input stream
     * @param placeholder placeholder
     */
    public RecordingBufferedInputStream(InputStream in, Void placeholder) {
        this(in, true, new ByteArrayOutputStream(32));
    }

    /**
     * Constructs a {@code RecordingBufferedInputStream} with recording capability enabled.
     * The initial buffer size of {@code recording} will be specified by {@code size}.
     * @param in the underlying input stream
     * @param size the initial buffer size of {@code recording} in bytes
     */
    public RecordingBufferedInputStream(InputStream in, int size) {
        this(in, true, new ByteArrayOutputStream(size));
    }

    /**
     * Constructs a {@code RecordingBufferedInputStream}. Whether to enable recording capability
     * is determined by {@code recordingEnabled} namely. If {@code recordingEnabled} is
     * {@code true}, the initial buffer size of {@code recording} will be 32 bytes.
     *
     * @param in the underlying input stream
     * @param recordingEnabled true if you want to enable the recording capability
     */
    public RecordingBufferedInputStream(InputStream in, boolean recordingEnabled) {
        this(in, recordingEnabled, recordingEnabled ? new ByteArrayOutputStream(32) : null);
    }

    /**
     * Constructs a {@code RecordingBufferedInputStream}. Whether to enable recording capability
     * is determined by {@code recordingEnabled} namely. If {@code recordingEnabled} is {@code true},
     * the initial buffer size of {@code recording} will be specified by {@code size}. If
     * {@code recordingEnabled} is {@code false}, the {@code size} will not be used.
     *
     * @param in the underlying input stream
     * @param recordingEnabled true if you want to enable the recording capability
     * @param size the initial buffer size of {@code recording} in bytes,
     *             if {@code recordingEnabled} is {@code true}
     * @throws IllegalArgumentException if {@code size} is negative
     */
    public RecordingBufferedInputStream(InputStream in, boolean recordingEnabled, int size) {
        this(in, recordingEnabled, recordingEnabled ? new ByteArrayOutputStream(size) : null);
    }

    /**
     * Discards all currently recorded bytes. This method calls {@code recording.reset()}.
     * @throws NullPointerException if the recording capability has not been enabled
     */
    public void resetRecording() {
        recording.reset();
    }

    /**
     * This method returns {@code recording.toByteArray()}.
     * @return all currently recorded bytes, as a byte array
     * @throws NullPointerException if the recording capability has not been enabled
     */
    public byte[] getRecordingByteArray() {
        return recording.toByteArray();
    }

    /**
     * @return whether the recording capability has been enabled
     */
    public boolean isRecordingEnabled() { return this.recordingEnabled; }

    @Override
    public int read() throws IOException {
        int result = super.read();
        if (recordingEnabled && result != -1) recording.write((byte) result);
        return result;
    }

    @Override
    public int read(byte[] b, int off, int len) throws IOException {
        int result = super.read(b, off, len);
        if (recordingEnabled && result != -1) recording.write(b, off, result);
        return result;
    }
}
