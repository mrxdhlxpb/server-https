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

import java.io.IOException;
import java.io.InputStream;

/**
 * @author mrxdhlxpb
 */
public class LimitedRecordingBufferedInputStream extends RecordingBufferedInputStream {

    private boolean limitEnabled = false;

    private long limit;

    public LimitedRecordingBufferedInputStream(InputStream in) {
        super(in);
    }

    public LimitedRecordingBufferedInputStream(InputStream in, Void placeholder) {
        super(in, placeholder);
    }

    public LimitedRecordingBufferedInputStream(InputStream in, int size) {
        super(in, size);
    }

    public LimitedRecordingBufferedInputStream(InputStream in, boolean recordingEnabled) {
        super(in, recordingEnabled);
    }

    public LimitedRecordingBufferedInputStream(InputStream in, boolean recordingEnabled, int size) {
        super(in, recordingEnabled, size);
    }

    public void enableLimit(long limit) {
        if (limit < 0)
            throw new IllegalArgumentException("negative limit");
        if (limitEnabled)
            throw new IllegalStateException("limit already enabled");
        this.limitEnabled = true;
        this.limit = limit;
    }

    public void disableLimit() {
        if (!limitEnabled)
            throw new IllegalStateException("limit not enabled");
        this.limitEnabled = false;
    }

    @Override
    public int read() throws IOException {
        if (limitEnabled) {
            if (1 > limit)
                throw new TryToReadOutOfLimitException();
            limit--;
        }
        return super.read();
    }

    @Override
    public int read(byte[] b, int off, int len) throws IOException {
        if (limitEnabled) {
            if (len > limit)
                throw new TryToReadOutOfLimitException();
            limit -= len;
        }
        return super.read(b, off, len);
    }

    public static final class TryToReadOutOfLimitException extends IOException {}
}
