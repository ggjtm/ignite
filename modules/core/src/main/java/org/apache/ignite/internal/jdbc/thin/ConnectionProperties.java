/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.ignite.internal.jdbc.thin;

import java.sql.SQLException;
import org.apache.ignite.internal.processors.odbc.jdbc.JdbcThinFeature;
import org.apache.ignite.internal.util.HostAndPortRange;
import org.apache.ignite.transactions.TransactionConcurrency;
import org.jetbrains.annotations.Nullable;

/**
 * Provide access and manipulations with connection JDBC properties.
 */
public interface ConnectionProperties {
    /** SSL mode: DISABLE. */
    public static final String SSL_MODE_DISABLE = "disable";

    /** SSL mode: REQUIRE. */
    public static final String SSL_MODE_REQUIRE = "require";

    /**
     * @return Schema name of the connection.
     */
    public String getSchema();

    /**
     * @param schema Schema name of the connection.
     */
    public void setSchema(String schema);

    /**
     * @return The URL of the connection.
     */
    public String getUrl();

    /**
     * @param url The URL of the connection.
     * @throws SQLException On invalid URL.
     */
    public void setUrl(String url) throws SQLException;

    /**
     * @return Ignite nodes addresses.
     */
    public HostAndPortRange[] getAddresses();

    /**
     * @param addrs Ignite nodes addresses.
     */
    public void setAddresses(HostAndPortRange[] addrs);

    /**
     * @return Distributed joins flag.
     */
    public boolean isDistributedJoins();

    /**
     * @param distributedJoins Distributed joins flag.
     */
    public void setDistributedJoins(boolean distributedJoins);

    /**
     * @return Enforce join order flag.
     */
    public boolean isEnforceJoinOrder();

    /**
     * @param enforceJoinOrder Enforce join order flag.
     */
    public void setEnforceJoinOrder(boolean enforceJoinOrder);

    /**
     * @return Collocated flag.
     */
    public boolean isCollocated();

    /**
     * @param collocated Collocated flag.
     */
    public void setCollocated(boolean collocated);

    /**
     * @return Replicated only flag.
     */
    public boolean isReplicatedOnly();

    /**
     * @param replicatedOnly Replicated only flag.
     */
    public void setReplicatedOnly(boolean replicatedOnly);

    /**
     * @return Auto close server cursors flag.
     */
    public boolean isAutoCloseServerCursor();

    /**
     * @param autoCloseServerCursor Auto close server cursors flag.
     */
    public void setAutoCloseServerCursor(boolean autoCloseServerCursor);

    /**
     * @return Socket send buffer size.
     */
    public int getSocketSendBuffer();

    /**
     * @param size Socket send buffer size.
     * @throws SQLException On error.
     */
    public void setSocketSendBuffer(int size) throws SQLException;

    /**
     * @return Socket receive buffer size.
     */
    public int getSocketReceiveBuffer();

    /**
     * @param size Socket receive buffer size.
     * @throws SQLException On error.
     */
    public void setSocketReceiveBuffer(int size) throws SQLException;

    /**
     * @return TCP no delay flag.
     */
    public boolean isTcpNoDelay();

    /**
     * @param tcpNoDelay TCP no delay flag.
     */
    public void setTcpNoDelay(boolean tcpNoDelay);

    /**
     * @return Lazy query execution flag.
     */
    public boolean isLazy();

    /**
     * @param lazy Lazy query execution flag.
     */
    public void setLazy(boolean lazy);

    /**
     * @return Skip reducer on update flag.
     */
    public boolean isSkipReducerOnUpdate();

    /**
     * @param skipReducerOnUpdate Skip reducer on update flag.
     */
    public void setSkipReducerOnUpdate(boolean skipReducerOnUpdate);

    /**
     * Gets SSL connection mode.
     *
     * @return Use SSL flag.
     * @see #setSslMode(String).
     */
    public String getSslMode();

    /**
     * Use SSL connection to Ignite node. In case set to {@code "require"} SSL context must be configured.
     * {@link #setSslClientCertificateKeyStoreUrl} property and related properties must be set up
     * or JSSE properties must be set up (see {@code javax.net.ssl.keyStore} and other {@code javax.net.ssl.*}
     * properties)
     *
     * In case set to {@code "disable"} plain connection is used.
     * Available modes: {@code "disable", "require"}. Default value is {@code "disable"}
     *
     * @param mode SSL mode.
     */
    public void setSslMode(String mode);

    /**
     * Gets protocol for secure transport.
     *
     * @return SSL protocol name.
     */
    public String getSslProtocol();

    /**
     * Sets protocol for secure transport. If not specified, TLS protocol will be used.
     * Protocols implementations supplied by JSEE: SSLv3 (SSL), TLSv1 (TLS), TLSv1.1, TLSv1.2
     *
     * <p>See more at JSSE Reference Guide.
     *
     * @param sslProtocol SSL protocol name.
     */
    public void setSslProtocol(String sslProtocol);

    /**
     * Gets cipher suites.
     *
     * @return SSL cipher suites.
     */
    public String getSslCipherSuites();

    /**
     * Override default cipher suites.
     *
     * <p>See more at JSSE Reference Guide.
     *
     * @param sslCipherSuites SSL cipher suites.
     */
    public void setSslCipherSuites(String sslCipherSuites);

    /**
     * Gets algorithm that will be used to create a key manager.
     *
     * @return Key manager algorithm.
     */
    public String getSslKeyAlgorithm();

    /**
     * Sets key manager algorithm that will be used to create a key manager. Notice that in most cased default value
     * suites well, however, on Android platform this value need to be set to <tt>X509<tt/>.
     * Algorithms implementations supplied by JSEE: PKIX (X509 or SunPKIX), SunX509
     *
     * <p>See more at JSSE Reference Guide.
     *
     * @param keyAlgorithm Key algorithm name.
     */
    public void setSslKeyAlgorithm(String keyAlgorithm);

    /**
     * Gets the key store URL.
     *
     * @return Client certificate KeyStore URL.
     */
    public String getSslClientCertificateKeyStoreUrl();

    /**
     * Sets path to the key store file. This is a mandatory parameter since
     * ssl context could not be initialized without key manager.
     *
     * In case {@link #getSslMode()} is {@code required} and key store URL isn't specified by Ignite properties
     * (e.g. at JDBC URL) the JSSE property {@code javax.net.ssl.keyStore} will be used.
     *
     * @param url Client certificate KeyStore URL.
     */
    public void setSslClientCertificateKeyStoreUrl(String url);

    /**
     * Gets key store password.
     *
     * @return Client certificate KeyStore password.
     */
    public String getSslClientCertificateKeyStorePassword();

    /**
     * Sets key store password.
     *
     * In case {@link #getSslMode()} is {@code required}  and key store password isn't specified by Ignite properties
     * (e.g. at JDBC URL) the JSSE property {@code javax.net.ssl.keyStorePassword} will be used.
     *
     * @param passwd Client certificate KeyStore password.
     */
    public void setSslClientCertificateKeyStorePassword(String passwd);

    /**
     * Gets key store type used for context creation.
     *
     * @return Client certificate KeyStore type.
     */
    public String getSslClientCertificateKeyStoreType();

    /**
     * Sets key store type used in context initialization.
     *
     * In case {@link #getSslMode()} is {@code required} and key store type isn't specified by Ignite properties
     *  (e.g. at JDBC URL)the JSSE property {@code javax.net.ssl.keyStoreType} will be used.
     * In case both Ignite properties and JSSE properties are not set the default 'JKS' type is used.
     *
     * <p>See more at JSSE Reference Guide.
     *
     * @param ksType Client certificate KeyStore type.
     */
    public void setSslClientCertificateKeyStoreType(String ksType);

    /**
     * Gets the trust store URL.
     *
     * @return Trusted certificate KeyStore URL.
     */
    public String getSslTrustCertificateKeyStoreUrl();

    /**
     * Sets path to the trust store file. This is an optional parameter,
     * however one of the {@code setSslTrustCertificateKeyStoreUrl(String)}, {@link #setSslTrustAll(boolean)}
     * properties must be set.
     *
     * In case {@link #getSslMode()} is {@code required} and trust store URL isn't specified by Ignite properties
     * (e.g. at JDBC URL) the JSSE property {@code javax.net.ssl.trustStore} will be used.
     *
     * @param url Trusted certificate KeyStore URL.
     */
    public void setSslTrustCertificateKeyStoreUrl(String url);

    /**
     * Gets trust store password.
     *
     * @return Trusted certificate KeyStore password.
     */
    public String getSslTrustCertificateKeyStorePassword();

    /**
     * Sets trust store password.
     *
     * In case {@link #getSslMode()} is {@code required} and trust store password isn't specified by Ignite properties
     * (e.g. at JDBC URL) the JSSE property {@code javax.net.ssl.trustStorePassword} will be used.
     *
     * @param passwd Trusted certificate KeyStore password.
     */
    public void setSslTrustCertificateKeyStorePassword(String passwd);

    /**
     * Gets trust store type.
     *
     * @return Trusted certificate KeyStore type.
     */
    public String getSslTrustCertificateKeyStoreType();

    /**
     * Sets trust store type.
     *
     * In case {@link #getSslMode()} is {@code required} and trust store type isn't specified by Ignite properties
     * (e.g. at JDBC URL) the JSSE property {@code javax.net.ssl.trustStoreType} will be used.
     * In case both Ignite properties and JSSE properties are not set the default 'JKS' type is used.
     *
     * @param ksType Trusted certificate KeyStore type.
     */
    public void setSslTrustCertificateKeyStoreType(String ksType);

    /**
     * Gets trust any server certificate flag.
     *
     * @return Trust all certificates flag.
     */
    public boolean isSslTrustAll();

    /**
     * Sets to {@code true} to trust any server certificate (revoked, expired or self-signed SSL certificates).
     *
     * <p> Defaults is {@code false}.
     *
     * Note: Do not enable this option in production you are ever going to use
     * on a network you do not entirely trust. Especially anything going over the public internet.
     *
     * @param trustAll Trust all certificates flag.
     */
    public void setSslTrustAll(boolean trustAll);

    /**
     * Gets the class name of the custom implementation of the Factory&lt;SSLSocketFactory&gt;.
     *
     * @return Custom class name that implements Factory&lt;SSLSocketFactory&gt;.
     */
    public String getSslFactory();

    /**
     * Sets the class name of the custom implementation of the Factory&lt;SSLSocketFactory&gt;.
     * If {@link #getSslMode()} is {@code required} and factory is specified the custom factory will be used
     * instead of JSSE socket factory. So, other SSL properties will be ignored.
     *
     * @param sslFactory Custom class name that implements Factory&lt;SSLSocketFactory&gt;.
     */
    public void setSslFactory(String sslFactory);

    /**
     * @param name User name to authentication.
     */
    public void setUsername(String name);

    /**
     * @return User name to authentication.
     */
    public String getUsername();

    /**
     * @param passwd User's password.
     */
    public void setPassword(String passwd);

    /**
     * @return User's password.
     */
    public String getPassword();

    /**
     * @return {@code true} if data page scan support is enabled for this connection, {@code false} if it's disabled
     *     and {@code null} for server default.
     */
    @Nullable public Boolean isDataPageScanEnabled();

    /**
     * @param dataPageScanEnabled {@code true} if data page scan support is enabled for this connection,
     *     if {@code false} then it's disabled, if {@code null} then server should use its default settings.
     */
    public void setDataPageScanEnabled(@Nullable Boolean dataPageScanEnabled);

    /**
     * @return {@code true} if jdbc thin partition awareness is enabled for this connection,
     * {@code false} if it's disabled.
     */
    public boolean isPartitionAwareness();

    /**
     * @param partitionAwareness {@code true} if jdbc thin partition awareness is enabled
     * for this connection, if {@code false} then it's disabled.
     */
    public void setPartitionAwareness(boolean partitionAwareness);

    /**
     * Note: Batch size of 1 prevents deadlock on update where keys sequence are different in several concurrent updates.
     *
     * @return update internal bach size.
     */
    @Nullable public Integer getUpdateBatchSize();

    /**
     * Note: Set to 1 to prevent deadlock on update where keys sequence are different in several concurrent updates.
     *
     * @param updateBatchSize update internal bach size.
     * @throws SQLException On error.
     */
    public void setUpdateBatchSize(@Nullable Integer updateBatchSize) throws SQLException;

    /**
     * @return SQL cache size that is used within partition awareness optimizations.
     */
    public int getPartitionAwarenessSqlCacheSize();

    /**
     * Sets SQL cache size that is used within partition awareness optimizations.
     *
     * @param partitionAwarenessSqlCacheSize SQL cache size.
     * @throws SQLException On error.
     */
    public void setPartitionAwarenessSqlCacheSize(int partitionAwarenessSqlCacheSize) throws SQLException;

    /**
     * @return Partition distributions cache size that is used within partition awareness optimizations.
     */
    public int getPartitionAwarenessPartitionDistributionsCacheSize();

    /**
     * Sets partition distributions cache size that is used within partition awareness optimizations.
     *
     * @param partitionAwarenessPartDistributionsCacheSize Partition distributions cache size.
     * @throws SQLException On error.
     */
    public void setPartitionAwarenessPartitionDistributionsCacheSize(
        int partitionAwarenessPartDistributionsCacheSize) throws SQLException;

    /**
     * Note: zero value means there is no limits.
     *
     * @return Query timeout in seconds.
     */
    @Nullable public Integer getQueryTimeout();

    /**
     * Note: zero value means there is no limits.
     *
     * @param qryTimeout Query timeout in seconds.
     */
    public void setQueryTimeout(@Nullable Integer qryTimeout) throws SQLException;

    /**
     * Note: zero value means there is no limits.
     *
     * @return Connection timeout in milliseconds.
     */
    @Nullable public int getConnectionTimeout();

    /**
     * Note: zero value means there is no limits.
     *
     * @param connTimeout Connection timeout in milliseconds.
     */
    public void setConnectionTimeout(@Nullable Integer connTimeout) throws SQLException;

    /**
     * Gets the class name of the custom implementation of the Factory&lt;Map&lt;String, String&gt;&gt;.
     *
     * This factory should return user attributes which can be used on server node.
     *
     * @return Custom class name that implements Factory&lt;Map&lt;String, String&gt;&gt;.
     */
    public String getUserAttributesFactory();

    /**
     * Sets the class name of the custom implementation of the Factory&lt;Map&lt;String, String&gt;&gt;.
     *
     * This factory should return user attributes which can be used on server node.
     *
     * Sent attributes can be accessed on server nodes from
     * {@link org.apache.ignite.internal.processors.rest.request.GridRestRequest GridRestRequest} or
     * {@link org.apache.ignite.internal.processors.odbc.ClientListenerAbstractConnectionContext
     * ClientListenerAbstractConnectionContext} (depends on client type).
     *
     * @param sslFactory Custom class name that implements Factory&lt;Map&lt;String, String&gt;&gt;.
     */
    public void setUserAttributesFactory(String sslFactory);

    /**
     * Any JDBC features could be force disabled.
     * See {@link JdbcThinFeature}.
     * The string should contain enumeration of feature names, separated by the comma.
     *
     * @return disabled features.
     */
    public String disabledFeatures();

    /**
     * @param features Disabled features. See {@link JdbcThinFeature}.
     *      The string should contain enumeration of feature names, separated by the comma.
     */
    public void disabledFeatures(String features);

    /**
     * Get keep binary configuration flag.
     *
     * @return Keep binary configuration flag.
     */
    public boolean isKeepBinary();

    /**
     * Set to {@code true} to keep binary objects in binary form.
     *
     * <p> Defaults is {@code false}.
     **
     * @param keepBinary Whether to keep binary objects in binary form.
     */
    public void setKeepBinary(boolean keepBinary);

    /**
     * @return SQL query engine name to use by a connection.
     */
    public String getQueryEngine();

    /**
     * Sets SQL query engine for a connection.
     *
     * @param qryEngine SQL Query engine name.
     */
    public void setQueryEngine(String qryEngine);

    /**
     * @return Transaction concurrency value.
     */
    public TransactionConcurrency getTransactionConcurrency();

    /**
     * Sets transaction concurrency.
     *
     * @param transactionConcurrency Transaction concurrecny.
     */
    public void setTransactionConcurrency(String transactionConcurrency);

    /**
     * @return Transaction timeout in milliseconds.
     */
    public int getTransactionTimeout();

    /**
     * Sets transaction timeout in milliseconds.
     *
     * @param transactionTimeout Transaction timeout in millicesonds.
     */
    public void setTransactionTimeout(int transactionTimeout) throws SQLException;

    /**
     * @return Transaction label.
     */
    public String getTransactionLabel();

    /**
     * Sets transaction label.
     *
     * @param transactionLabel Transaction label.
     */
    public void setTransactionLabel(String transactionLabel);
}
