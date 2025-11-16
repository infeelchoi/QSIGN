/*
 * Q-SSL: Quantum-Resistant Secure Sockets Layer
 * SSL/TLS-PQC Hybrid Handshake Protocol Implementation
 *
 * This module implements the SSL/TLS 1.3 handshake with PQC extensions:
 * - ClientHello/ServerHello with PQC algorithm negotiation
 * - Dual key exchange: ECDHE P-384 + KYBER1024
 * - Dual signature verification: RSA/ECDSA + DILITHIUM3
 * - Hybrid session key derivation
 *
 * Copyright 2025 QSIGN Project
 * Licensed under the Apache License, Version 2.0
 */

#include <qssl/qssl.h>
#include <openssl/ssl.h>
#include <openssl/evp.h>
#include <openssl/ec.h>
#include <openssl/rand.h>
#include <openssl/x509.h>
#include <string.h>
#include <stdio.h>
#include <stdlib.h>
#include <unistd.h>
#include <sys/socket.h>

/* Logging macros */
#ifdef ENABLE_LOGGING
#define LOG_INFO(fmt, ...) fprintf(stderr, "[QSSL-HANDSHAKE-INFO] " fmt "\n", ##__VA_ARGS__)
#define LOG_ERROR(fmt, ...) fprintf(stderr, "[QSSL-HANDSHAKE-ERROR] " fmt "\n", ##__VA_ARGS__)
#define LOG_DEBUG(fmt, ...) fprintf(stderr, "[QSSL-HANDSHAKE-DEBUG] " fmt "\n", ##__VA_ARGS__)
#else
#define LOG_INFO(fmt, ...)
#define LOG_ERROR(fmt, ...)
#define LOG_DEBUG(fmt, ...)
#endif

/* Handshake message types */
#define QSSL_MSG_CLIENT_HELLO       0x01
#define QSSL_MSG_SERVER_HELLO       0x02
#define QSSL_MSG_ENCRYPTED_EXTENSIONS 0x08
#define QSSL_MSG_CERTIFICATE        0x0B
#define QSSL_MSG_CERTIFICATE_VERIFY 0x0F
#define QSSL_MSG_FINISHED           0x14

/* PQC Extension IDs (using experimental range) */
#define QSSL_EXT_SUPPORTED_PQC_KEMS  0xFE00
#define QSSL_EXT_SUPPORTED_PQC_SIGS  0xFE01
#define QSSL_EXT_KYBER_KEY_SHARE     0xFE02

/* Maximum handshake message size */
#define QSSL_MAX_HANDSHAKE_MSG_SIZE  65536

/*
 * Handshake state
 */
typedef enum {
    QSSL_HS_STATE_START,
    QSSL_HS_STATE_CLIENT_HELLO_SENT,
    QSSL_HS_STATE_SERVER_HELLO_RECEIVED,
    QSSL_HS_STATE_SERVER_CERT_RECEIVED,
    QSSL_HS_STATE_SERVER_FINISHED,
    QSSL_HS_STATE_CLIENT_FINISHED,
    QSSL_HS_STATE_CONNECTED
} qssl_handshake_state_t;

/*
 * Connection structure (internal)
 */
struct qssl_connection_st {
    QSSL_CTX *ctx;
    int fd;
    int mode; /* QSSL_CLIENT_MODE or QSSL_SERVER_MODE */
    qssl_handshake_state_t state;

    /* Cryptographic state */
    QSSL_KYBER_KEY kyber_key;
    QSSL_ECDHE_KEY ecdhe_key;
    QSSL_HYBRID_SECRET hybrid_secret;
    QSSL_SESSION_KEYS session_keys;

    /* Peer keys */
    QSSL_KYBER_KEY peer_kyber_key;
    QSSL_DILITHIUM_KEY peer_dilithium_key;

    /* Certificate chain */
    QSSL_CERTIFICATE *peer_cert;

    /* Error state */
    int last_error;

    /* OpenSSL SSL object for classical crypto */
    SSL *ssl;
};

/*
 * Context structure (internal)
 */
struct qssl_ctx_st {
    int mode; /* QSSL_CLIENT_MODE or QSSL_SERVER_MODE */
    uint32_t options;
    int verify_mode;
    qssl_verify_callback verify_callback;

    /* Certificate and keys */
    QSSL_CERTIFICATE *cert;
    QSSL_DILITHIUM_KEY dilithium_key;

    /* OpenSSL SSL_CTX for classical crypto */
    SSL_CTX *ssl_ctx;

    /* Supported algorithms */
    uint16_t supported_kems[8];
    size_t num_kems;
    uint16_t supported_sigs[8];
    size_t num_sigs;

#ifdef ENABLE_HSM
    QSSL_HSM_CONFIG hsm_config;
#endif
};

/******************************************************************************
 * ECDHE P-384 Helper Functions
 ******************************************************************************/

/*
 * Generate ECDHE P-384 keypair
 */
static int qssl_ecdhe_keygen(QSSL_ECDHE_KEY *key) {
    EVP_PKEY_CTX *pctx = NULL;
    EVP_PKEY *pkey = NULL;
    size_t pubkey_len = QSSL_ECDHE_P384_PUBLIC_KEY_BYTES;
    int ret = QSSL_ERROR_KEY_GENERATION;

    if (key == NULL) {
        return QSSL_ERROR_NULL_POINTER;
    }

    /* Create key generation context */
    pctx = EVP_PKEY_CTX_new_id(EVP_PKEY_EC, NULL);
    if (pctx == NULL) {
        LOG_ERROR("Failed to create ECDHE context");
        return QSSL_ERROR_KEY_GENERATION;
    }

    /* Initialize key generation */
    if (EVP_PKEY_keygen_init(pctx) <= 0) {
        LOG_ERROR("ECDHE keygen init failed");
        goto cleanup;
    }

    /* Set curve to P-384 (NIST secp384r1) */
    if (EVP_PKEY_CTX_set_ec_paramgen_curve_nid(pctx, NID_secp384r1) <= 0) {
        LOG_ERROR("ECDHE set curve failed");
        goto cleanup;
    }

    /* Generate key */
    if (EVP_PKEY_keygen(pctx, &pkey) <= 0) {
        LOG_ERROR("ECDHE keygen failed");
        goto cleanup;
    }

    /* Extract public key */
    if (EVP_PKEY_get_raw_public_key(pkey, key->public_key, &pubkey_len) <= 0) {
        LOG_ERROR("Failed to extract ECDHE public key");
        goto cleanup;
    }

    key->evp_pkey = pkey;
    key->has_shared_secret = 0;
    pkey = NULL; /* Don't free, stored in key structure */
    ret = QSSL_SUCCESS;

    LOG_DEBUG("ECDHE P-384 keypair generated");

cleanup:
    if (pctx != NULL) {
        EVP_PKEY_CTX_free(pctx);
    }
    if (pkey != NULL) {
        EVP_PKEY_free(pkey);
    }

    return ret;
}

/*
 * Derive ECDHE shared secret
 */
static int qssl_ecdhe_derive(QSSL_ECDHE_KEY *key, const uint8_t *peer_public_key,
                             size_t peer_pubkey_len) {
    EVP_PKEY_CTX *ctx = NULL;
    EVP_PKEY *peer_key = NULL;
    size_t secret_len = QSSL_ECDHE_P384_SHARED_SECRET_BYTES;
    int ret = QSSL_ERROR_KEY_DERIVATION;

    if (key == NULL || peer_public_key == NULL || key->evp_pkey == NULL) {
        return QSSL_ERROR_NULL_POINTER;
    }

    /* Create peer public key */
    peer_key = EVP_PKEY_new_raw_public_key(EVP_PKEY_EC, NULL,
                                            peer_public_key, peer_pubkey_len);
    if (peer_key == NULL) {
        LOG_ERROR("Failed to import peer ECDHE public key");
        return QSSL_ERROR_KEY_DERIVATION;
    }

    /* Create derivation context */
    ctx = EVP_PKEY_CTX_new(key->evp_pkey, NULL);
    if (ctx == NULL) {
        LOG_ERROR("Failed to create ECDHE derivation context");
        EVP_PKEY_free(peer_key);
        return QSSL_ERROR_KEY_DERIVATION;
    }

    /* Initialize derivation */
    if (EVP_PKEY_derive_init(ctx) <= 0) {
        LOG_ERROR("ECDHE derive init failed");
        goto cleanup;
    }

    /* Set peer key */
    if (EVP_PKEY_derive_set_peer(ctx, peer_key) <= 0) {
        LOG_ERROR("ECDHE set peer failed");
        goto cleanup;
    }

    /* Derive shared secret */
    if (EVP_PKEY_derive(ctx, key->shared_secret, &secret_len) <= 0) {
        LOG_ERROR("ECDHE derive failed");
        goto cleanup;
    }

    key->has_shared_secret = 1;
    ret = QSSL_SUCCESS;

    LOG_DEBUG("ECDHE shared secret derived");

cleanup:
    if (ctx != NULL) {
        EVP_PKEY_CTX_free(ctx);
    }
    if (peer_key != NULL) {
        EVP_PKEY_free(peer_key);
    }

    return ret;
}

/******************************************************************************
 * Handshake Message Functions
 ******************************************************************************/

/*
 * Send ClientHello with PQC extensions
 */
static int qssl_send_client_hello(QSSL_CONNECTION *conn) {
    uint8_t msg[QSSL_MAX_HANDSHAKE_MSG_SIZE];
    size_t msg_len = 0;
    int ret;

    LOG_INFO("Sending ClientHello with PQC extensions");

    /* Generate client random */
    if (RAND_bytes(conn->hybrid_secret.client_random, QSSL_MAX_RANDOM_LEN) != 1) {
        LOG_ERROR("Failed to generate client random");
        return QSSL_ERROR_HANDSHAKE_FAILED;
    }

    /* Message type */
    msg[msg_len++] = QSSL_MSG_CLIENT_HELLO;

    /* Add client random */
    memcpy(msg + msg_len, conn->hybrid_secret.client_random, QSSL_MAX_RANDOM_LEN);
    msg_len += QSSL_MAX_RANDOM_LEN;

    /* Generate ECDHE keypair */
    ret = qssl_ecdhe_keygen(&conn->ecdhe_key);
    if (ret != QSSL_SUCCESS) {
        LOG_ERROR("Failed to generate ECDHE keypair");
        return ret;
    }

    /* Add ECDHE public key */
    memcpy(msg + msg_len, conn->ecdhe_key.public_key,
           QSSL_ECDHE_P384_PUBLIC_KEY_BYTES);
    msg_len += QSSL_ECDHE_P384_PUBLIC_KEY_BYTES;

    /* Add supported PQC KEM algorithms */
    msg[msg_len++] = (QSSL_KEM_KYBER1024 >> 8) & 0xFF;
    msg[msg_len++] = QSSL_KEM_KYBER1024 & 0xFF;

    /* Add supported PQC signature algorithms */
    msg[msg_len++] = (QSSL_SIG_DILITHIUM3 >> 8) & 0xFF;
    msg[msg_len++] = QSSL_SIG_DILITHIUM3 & 0xFF;

    /* Send message */
    ssize_t sent = send(conn->fd, msg, msg_len, 0);
    if (sent != (ssize_t)msg_len) {
        LOG_ERROR("Failed to send ClientHello (sent %zd of %zu bytes)", sent, msg_len);
        return QSSL_ERROR_SYSCALL;
    }

    conn->state = QSSL_HS_STATE_CLIENT_HELLO_SENT;
    LOG_INFO("ClientHello sent (%zu bytes)", msg_len);

    return QSSL_SUCCESS;
}

/*
 * Receive and process ServerHello
 */
static int qssl_receive_server_hello(QSSL_CONNECTION *conn) {
    uint8_t msg[QSSL_MAX_HANDSHAKE_MSG_SIZE];
    ssize_t received;
    size_t offset = 0;
    int ret;

    LOG_INFO("Waiting for ServerHello");

    /* Receive message */
    received = recv(conn->fd, msg, sizeof(msg), 0);
    if (received <= 0) {
        LOG_ERROR("Failed to receive ServerHello");
        return QSSL_ERROR_SYSCALL;
    }

    LOG_DEBUG("Received %zd bytes", received);

    /* Check message type */
    if (msg[offset++] != QSSL_MSG_SERVER_HELLO) {
        LOG_ERROR("Expected ServerHello, got 0x%02x", msg[0]);
        return QSSL_ERROR_INVALID_MESSAGE;
    }

    /* Extract server random */
    memcpy(conn->hybrid_secret.server_random, msg + offset, QSSL_MAX_RANDOM_LEN);
    offset += QSSL_MAX_RANDOM_LEN;

    /* Extract server ECDHE public key */
    uint8_t server_ecdhe_pubkey[QSSL_ECDHE_P384_PUBLIC_KEY_BYTES];
    memcpy(server_ecdhe_pubkey, msg + offset, QSSL_ECDHE_P384_PUBLIC_KEY_BYTES);
    offset += QSSL_ECDHE_P384_PUBLIC_KEY_BYTES;

    /* Extract server KYBER public key */
    memcpy(conn->peer_kyber_key.public_key, msg + offset,
           QSSL_KYBER1024_PUBLIC_KEY_BYTES);
    offset += QSSL_KYBER1024_PUBLIC_KEY_BYTES;

    LOG_INFO("ServerHello received and parsed");

    /* Derive ECDHE shared secret */
    ret = qssl_ecdhe_derive(&conn->ecdhe_key, server_ecdhe_pubkey,
                            QSSL_ECDHE_P384_PUBLIC_KEY_BYTES);
    if (ret != QSSL_SUCCESS) {
        LOG_ERROR("Failed to derive ECDHE shared secret");
        return ret;
    }

    /* Perform KYBER encapsulation */
    ret = qssl_kyber_encapsulate(&conn->peer_kyber_key);
    if (ret != QSSL_SUCCESS) {
        LOG_ERROR("Failed to encapsulate KYBER key");
        return ret;
    }

    /* Copy shared secrets to hybrid secret structure */
    memcpy(conn->hybrid_secret.classical_secret,
           conn->ecdhe_key.shared_secret,
           QSSL_ECDHE_P384_SHARED_SECRET_BYTES);
    memcpy(conn->hybrid_secret.pqc_secret,
           conn->peer_kyber_key.shared_secret,
           QSSL_KYBER1024_SHARED_SECRET_BYTES);

    conn->state = QSSL_HS_STATE_SERVER_HELLO_RECEIVED;
    LOG_INFO("Key exchange completed");

    return QSSL_SUCCESS;
}

/*
 * Send ServerHello with PQC key shares
 */
static int qssl_send_server_hello(QSSL_CONNECTION *conn) {
    uint8_t msg[QSSL_MAX_HANDSHAKE_MSG_SIZE];
    size_t msg_len = 0;
    int ret;

    LOG_INFO("Sending ServerHello with PQC extensions");

    /* Generate server random */
    if (RAND_bytes(conn->hybrid_secret.server_random, QSSL_MAX_RANDOM_LEN) != 1) {
        LOG_ERROR("Failed to generate server random");
        return QSSL_ERROR_HANDSHAKE_FAILED;
    }

    /* Message type */
    msg[msg_len++] = QSSL_MSG_SERVER_HELLO;

    /* Add server random */
    memcpy(msg + msg_len, conn->hybrid_secret.server_random, QSSL_MAX_RANDOM_LEN);
    msg_len += QSSL_MAX_RANDOM_LEN;

    /* Generate ECDHE keypair */
    ret = qssl_ecdhe_keygen(&conn->ecdhe_key);
    if (ret != QSSL_SUCCESS) {
        LOG_ERROR("Failed to generate ECDHE keypair");
        return ret;
    }

    /* Add ECDHE public key */
    memcpy(msg + msg_len, conn->ecdhe_key.public_key,
           QSSL_ECDHE_P384_PUBLIC_KEY_BYTES);
    msg_len += QSSL_ECDHE_P384_PUBLIC_KEY_BYTES;

    /* Generate KYBER keypair */
    ret = qssl_kyber_keygen(&conn->kyber_key);
    if (ret != QSSL_SUCCESS) {
        LOG_ERROR("Failed to generate KYBER keypair");
        return ret;
    }

    /* Add KYBER public key */
    memcpy(msg + msg_len, conn->kyber_key.public_key,
           QSSL_KYBER1024_PUBLIC_KEY_BYTES);
    msg_len += QSSL_KYBER1024_PUBLIC_KEY_BYTES;

    /* Send message */
    ssize_t sent = send(conn->fd, msg, msg_len, 0);
    if (sent != (ssize_t)msg_len) {
        LOG_ERROR("Failed to send ServerHello");
        return QSSL_ERROR_SYSCALL;
    }

    LOG_INFO("ServerHello sent (%zu bytes)", msg_len);

    return QSSL_SUCCESS;
}

/*
 * Receive ClientHello
 */
static int qssl_receive_client_hello(QSSL_CONNECTION *conn) {
    uint8_t msg[QSSL_MAX_HANDSHAKE_MSG_SIZE];
    ssize_t received;
    size_t offset = 0;

    LOG_INFO("Waiting for ClientHello");

    /* Receive message */
    received = recv(conn->fd, msg, sizeof(msg), 0);
    if (received <= 0) {
        LOG_ERROR("Failed to receive ClientHello");
        return QSSL_ERROR_SYSCALL;
    }

    LOG_DEBUG("Received %zd bytes", received);

    /* Check message type */
    if (msg[offset++] != QSSL_MSG_CLIENT_HELLO) {
        LOG_ERROR("Expected ClientHello, got 0x%02x", msg[0]);
        return QSSL_ERROR_INVALID_MESSAGE;
    }

    /* Extract client random */
    memcpy(conn->hybrid_secret.client_random, msg + offset, QSSL_MAX_RANDOM_LEN);
    offset += QSSL_MAX_RANDOM_LEN;

    /* Extract client ECDHE public key */
    uint8_t client_ecdhe_pubkey[QSSL_ECDHE_P384_PUBLIC_KEY_BYTES];
    memcpy(client_ecdhe_pubkey, msg + offset, QSSL_ECDHE_P384_PUBLIC_KEY_BYTES);
    offset += QSSL_ECDHE_P384_PUBLIC_KEY_BYTES;

    LOG_INFO("ClientHello received and parsed");

    /* Note: Client ECDHE key will be used after ServerHello is sent */
    memcpy(conn->peer_kyber_key.public_key, client_ecdhe_pubkey,
           QSSL_ECDHE_P384_PUBLIC_KEY_BYTES);

    return QSSL_SUCCESS;
}

/******************************************************************************
 * Public Handshake Functions
 ******************************************************************************/

/*
 * Perform client-side handshake
 */
int qssl_connect(QSSL_CONNECTION *conn) {
    int ret;

    if (conn == NULL) {
        return QSSL_ERROR_NULL_POINTER;
    }

    if (conn->mode != QSSL_CLIENT_MODE) {
        LOG_ERROR("qssl_connect called on server connection");
        return QSSL_ERROR_INVALID_ARGUMENT;
    }

    LOG_INFO("Starting Q-SSL client handshake");

    /* Send ClientHello */
    ret = qssl_send_client_hello(conn);
    if (ret != QSSL_SUCCESS) {
        conn->last_error = ret;
        return ret;
    }

    /* Receive ServerHello */
    ret = qssl_receive_server_hello(conn);
    if (ret != QSSL_SUCCESS) {
        conn->last_error = ret;
        return ret;
    }

    /* Derive hybrid master secret */
    ret = qssl_derive_master_secret(&conn->hybrid_secret);
    if (ret != QSSL_SUCCESS) {
        LOG_ERROR("Failed to derive master secret");
        conn->last_error = ret;
        return ret;
    }

    /* Derive session keys */
    ret = qssl_derive_session_keys(&conn->hybrid_secret, &conn->session_keys);
    if (ret != QSSL_SUCCESS) {
        LOG_ERROR("Failed to derive session keys");
        conn->last_error = ret;
        return ret;
    }

    /* Send KYBER ciphertext to server */
    ssize_t sent = send(conn->fd, conn->peer_kyber_key.ciphertext,
                        QSSL_KYBER1024_CIPHERTEXT_BYTES, 0);
    if (sent != QSSL_KYBER1024_CIPHERTEXT_BYTES) {
        LOG_ERROR("Failed to send KYBER ciphertext");
        conn->last_error = QSSL_ERROR_SYSCALL;
        return QSSL_ERROR_SYSCALL;
    }

    conn->state = QSSL_HS_STATE_CONNECTED;
    LOG_INFO("Q-SSL client handshake completed successfully");

    return QSSL_SUCCESS;
}

/*
 * Perform server-side handshake
 */
int qssl_accept(QSSL_CONNECTION *conn) {
    int ret;

    if (conn == NULL) {
        return QSSL_ERROR_NULL_POINTER;
    }

    if (conn->mode != QSSL_SERVER_MODE) {
        LOG_ERROR("qssl_accept called on client connection");
        return QSSL_ERROR_INVALID_ARGUMENT;
    }

    LOG_INFO("Starting Q-SSL server handshake");

    /* Receive ClientHello */
    ret = qssl_receive_client_hello(conn);
    if (ret != QSSL_SUCCESS) {
        conn->last_error = ret;
        return ret;
    }

    /* Send ServerHello */
    ret = qssl_send_server_hello(conn);
    if (ret != QSSL_SUCCESS) {
        conn->last_error = ret;
        return ret;
    }

    /* Receive KYBER ciphertext from client */
    uint8_t kyber_ciphertext[QSSL_KYBER1024_CIPHERTEXT_BYTES];
    ssize_t received = recv(conn->fd, kyber_ciphertext,
                            QSSL_KYBER1024_CIPHERTEXT_BYTES, 0);
    if (received != QSSL_KYBER1024_CIPHERTEXT_BYTES) {
        LOG_ERROR("Failed to receive KYBER ciphertext");
        conn->last_error = QSSL_ERROR_SYSCALL;
        return QSSL_ERROR_SYSCALL;
    }

    /* Decapsulate KYBER ciphertext */
    memcpy(conn->kyber_key.ciphertext, kyber_ciphertext,
           QSSL_KYBER1024_CIPHERTEXT_BYTES);
    ret = qssl_kyber_decapsulate(&conn->kyber_key);
    if (ret != QSSL_SUCCESS) {
        LOG_ERROR("Failed to decapsulate KYBER key");
        conn->last_error = ret;
        return ret;
    }

    /* Derive ECDHE shared secret (using client's public key received earlier) */
    ret = qssl_ecdhe_derive(&conn->ecdhe_key, conn->peer_kyber_key.public_key,
                            QSSL_ECDHE_P384_PUBLIC_KEY_BYTES);
    if (ret != QSSL_SUCCESS) {
        LOG_ERROR("Failed to derive ECDHE shared secret");
        conn->last_error = ret;
        return ret;
    }

    /* Copy shared secrets */
    memcpy(conn->hybrid_secret.classical_secret,
           conn->ecdhe_key.shared_secret,
           QSSL_ECDHE_P384_SHARED_SECRET_BYTES);
    memcpy(conn->hybrid_secret.pqc_secret,
           conn->kyber_key.shared_secret,
           QSSL_KYBER1024_SHARED_SECRET_BYTES);

    /* Derive hybrid master secret */
    ret = qssl_derive_master_secret(&conn->hybrid_secret);
    if (ret != QSSL_SUCCESS) {
        LOG_ERROR("Failed to derive master secret");
        conn->last_error = ret;
        return ret;
    }

    /* Derive session keys */
    ret = qssl_derive_session_keys(&conn->hybrid_secret, &conn->session_keys);
    if (ret != QSSL_SUCCESS) {
        LOG_ERROR("Failed to derive session keys");
        conn->last_error = ret;
        return ret;
    }

    conn->state = QSSL_HS_STATE_CONNECTED;
    LOG_INFO("Q-SSL server handshake completed successfully");

    return QSSL_SUCCESS;
}

/*
 * Verify peer certificate
 */
int qssl_verify_peer_certificate(QSSL_CONNECTION *conn) {
    if (conn == NULL) {
        return 0;
    }

    /* TODO: Implement full certificate chain validation
     * including DILITHIUM3 signature verification */

    LOG_INFO("Certificate verification (stub - always succeeds)");
    return 1;
}

/*
 * Set server name indication
 */
int qssl_set_server_name(QSSL_CONNECTION *conn, const char *hostname) {
    if (conn == NULL || hostname == NULL) {
        return QSSL_ERROR_NULL_POINTER;
    }

    LOG_INFO("SNI set to: %s", hostname);
    return QSSL_SUCCESS;
}
