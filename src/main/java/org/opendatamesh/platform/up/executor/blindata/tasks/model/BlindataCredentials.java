package org.opendatamesh.platform.up.executor.blindata.tasks.model;

/** Credentials of the technical Blindata identity of one environment. Never logged. */
public sealed interface BlindataCredentials {

    /** {@code X-BD-User} + {@code X-BD-ApiKey}. */
    record ApiKey(String user, String key) implements BlindataCredentials {

        @Override
        public String toString() {
            return "ApiKey[user=" + user + "]";
        }
    }

    /** OAuth2 client credentials (shared secret or certificate), same parameters as the Blindata observer. */
    record OAuth2ClientCredentials(String url, String grantType, String scope, String clientId, String clientSecret,
                                   String clientCertificate, String clientCertificatePrivateKey)
            implements BlindataCredentials {

        @Override
        public String toString() {
            return "OAuth2ClientCredentials[url=" + url + ", clientId=" + clientId + "]";
        }
    }
}
