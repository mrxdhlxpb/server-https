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

import personal.mrxdhlxpb.server.https.configuration.Configuration;

import javax.net.ssl.*;
import java.io.IOException;
import java.security.KeyStore;
import java.util.Objects;
import java.util.concurrent.Executors;

/**
 * Runs an HTTPS server. This is an implementation for the "origin server"
 * as defined in <em>RFC 9110: HTTP Semantics Section 3.6. Origin Server</em>.
 *
 * @author mrxdhlxpb
 */
public class HTTPSServer implements Runnable {

    private final Configuration configuration;

    public HTTPSServer(Configuration configuration) {
        this.configuration = Objects.requireNonNull(configuration);
    }

    @Override
    public void run() {
        try (var executor = Executors.newVirtualThreadPerTaskExecutor();
             var serverSocket = createSSLServerSocket()) {
            while (true) executor.submit(new SSLSocketTask((SSLSocket) serverSocket.accept()));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private SSLServerSocket createSSLServerSocket() throws Exception {
        final char[] password = configuration.getNetworkConfiguration().getKeyStorePassword();
        SSLContext context = SSLContext.getInstance("TLSv1.3");// RFC 8446: TLS version 1.3
        KeyStore keyStore = KeyStore.getInstance(
                configuration.getNetworkConfiguration().getKeyStoreFile(),
                password);
        KeyManagerFactory keyManagerFactory = KeyManagerFactory.getInstance("PKIX");
        keyManagerFactory.init(keyStore, password);
        KeyManager[] keyManagers = keyManagerFactory.getKeyManagers();
        context.init(keyManagers, null, null); // client authentication is not desired
        return (SSLServerSocket) context
                .getServerSocketFactory()
                .createServerSocket(
                        configuration.getNetworkConfiguration().getPort(),
                        configuration.getNetworkConfiguration().getServerSocketBacklog(),
                        configuration.getNetworkConfiguration().getServerSocketBindAddress());
    }

    private final class SSLSocketTask implements Runnable {
        private final HTTP1_1Processor http1_1Processor;
        private final SSLSocket socket;

        public SSLSocketTask(SSLSocket socket) throws IOException {
            this.http1_1Processor = new HTTP1_1Processor(configuration, socket);
            this.socket = socket;

            socket.setSoTimeout(configuration.getNetworkConfiguration().getSocketSoTimeout());
        }

        @Override
        public void run() {
            try {
                try {
                    socket.startHandshake();
                    while (http1_1Processor.process()) {}
                } finally {
                    socket.close();
                }
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }

}
