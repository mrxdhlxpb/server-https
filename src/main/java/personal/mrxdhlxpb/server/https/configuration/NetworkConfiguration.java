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
package personal.mrxdhlxpb.server.https.configuration;

import java.io.File;
import java.net.InetAddress;
import java.util.Set;

/**
 * @author mrxdhlxpb
 */
public interface NetworkConfiguration {
    /**
     * @return the port number that the server socket is bound to, or 0 to
     *         use a port number that is automatically allocated
     * @see java.net.ServerSocket#ServerSocket(int, int, InetAddress)
     */
    int getPort();

    /**
     * @return requested maximum length of the queue of incoming connections
     * @see java.net.ServerSocket#ServerSocket(int, int, InetAddress)
     */
    int getServerSocketBacklog();

    /**
     * @return the local InetAddress the server will bind to
     * @see java.net.ServerSocket#ServerSocket(int, int, InetAddress)
     */
    InetAddress getServerSocketBindAddress();

    /**
     * @return the keystore file
     * @see java.security.KeyStore#getInstance(File, char[])
     */
    File getKeyStoreFile();

    /**
     * @return the keystore password, which may be {@code null}
     * @see java.security.KeyStore#getInstance(File, char[])
     */
    char[] getKeyStorePassword();

    int getSocketSoTimeout();

    String getServerName();

    Set<String> getServerAliases();

}
