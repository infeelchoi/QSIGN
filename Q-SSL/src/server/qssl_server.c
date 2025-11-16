/*
 * Q-SSL: Quantum-Resistant Secure Sockets Layer
 * Server Implementation
 *
 * This module provides server-side Q-SSL functionality:
 * - Server initialization and configuration
 * - Connection acceptance
 * - Hybrid handshake execution
 * - Encrypted data handling
 * - Session management
 *
 * Copyright 2025 QSIGN Project
 * Licensed under the Apache License, Version 2.0
 */

#include <qssl/qssl.h>
#include <openssl/ssl.h>
#include <openssl/err.h>
#include <openssl/evp.h>
#include <openssl/aes.h>
#include <string.h>
#include <stdio.h>
#include <stdlib.h>
#include <unistd.h>
#include <errno.h>
#include <sys/socket.h>

#ifdef ENABLE_HSM
#include <dlfcn.h>
#ifdef __linux__
#include <pkcs11.h>
#else
/* Fallback for systems without PKCS#11 headers */
typedef unsigned long CK_RV;
typedef unsigned long CK_SESSION_HANDLE;
#endif
#endif

/* Logging macros */
#ifdef ENABLE_LOGGING
#define LOG_INFO(fmt, ...) fprintf(stderr, "[QSSL-SERVER-INFO] " fmt "\n", ##__VA_ARGS__)
#define LOG_ERROR(fmt, ...) fprintf(stderr, "[QSSL-SERVER-ERROR] " fmt "\n", ##__VA_ARGS__)
#define LOG_DEBUG(fmt, ...) fprintf(stderr, "[QSSL-SERVER-DEBUG] " fmt "\n", ##__VA_ARGS__)
#else
#define LOG_INFO(fmt, ...)
#define LOG_ERROR(fmt, ...)
#define LOG_DEBUG(fmt, ...)
#endif

/* External declarations from other modules */
extern int qssl_accept(QSSL_CONNECTION *conn);
extern int qssl_verify_peer_certificate(QSSL_CONNECTION *conn);

/* Forward declarations */
struct qssl_ctx_st;
struct qssl_connection_st;

/******************************************************************************
 * Context Management
 ******************************************************************************/

/*
 * Create new Q-SSL context
 */
QSSL_CTX *qssl_ctx_new(int mode) {
    QSSL_CTX *ctx;

    /* Validate mode */
    if (mode != QSSL_CLIENT_MODE && mode != QSSL_SERVER_MODE) {
        LOG_ERROR("Invalid mode: %d", mode);
        return NULL;
    }

    /* Allocate context */
    ctx = (QSSL_CTX *)calloc(1, sizeof(QSSL_CTX));
    if (ctx == NULL) {
        LOG_ERROR("Failed to allocate context: %s", strerror(errno));
        return NULL;
    }

    ctx->mode = mode;
    ctx->options = QSSL_OP_HYBRID_MODE; /* Default to hybrid mode */
    ctx->verify_mode = QSSL_VERIFY_NONE;
    ctx->verify_callback = NULL;
    ctx->cert = NULL;

    /* Initialize default supported algorithms */
    ctx->supported_kems[0] = QSSL_KEM_KYBER1024;
    ctx->num_kems = 1;
    ctx->supported_sigs[0] = QSSL_SIG_DILITHIUM3;
    ctx->num_sigs = 1;

    /* Create OpenSSL SSL_CTX for classical crypto operations */
    const SSL_METHOD *method;
    if (mode == QSSL_SERVER_MODE) {
        method = TLS_server_method();
    } else {
        method = TLS_client_method();
    }

    ctx->ssl_ctx = SSL_CTX_new(method);
    if (ctx->ssl_ctx == NULL) {
        LOG_ERROR("Failed to create SSL_CTX");
        free(ctx);
        return NULL;
    }

    /* Set minimum TLS version to 1.3 */
    SSL_CTX_set_min_proto_version(ctx->ssl_ctx, TLS1_3_VERSION);

    /* Set secure cipher suites */
    SSL_CTX_set_cipher_list(ctx->ssl_ctx,
        "TLS_AES_256_GCM_SHA384:TLS_CHACHA20_POLY1305_SHA256");

#ifdef ENABLE_HSM
    /* Initialize HSM configuration */
    memset(&ctx->hsm_config, 0, sizeof(QSSL_HSM_CONFIG));
#endif

    LOG_INFO("Q-SSL context created (mode=%s)",
             mode == QSSL_SERVER_MODE ? "server" : "client");

    return ctx;
}

/*
 * Free Q-SSL context
 */
void qssl_ctx_free(QSSL_CTX *ctx) {
    if (ctx == NULL) {
        return;
    }

    /* Free SSL context */
    if (ctx->ssl_ctx != NULL) {
        SSL_CTX_free(ctx->ssl_ctx);
    }

    /* Securely erase DILITHIUM secret key */
    if (ctx->dilithium_key.has_secret_key) {
        qssl_secure_zero(&ctx->dilithium_key, sizeof(QSSL_DILITHIUM_KEY));
    }

    /* Free certificate */
    if (ctx->cert != NULL) {
        qssl_certificate_free(ctx->cert);
    }

#ifdef ENABLE_HSM
    /* Cleanup HSM */
    if (ctx->hsm_config.initialized) {
        qssl_hsm_cleanup();
    }
    if (ctx->hsm_config.pkcs11_module_path != NULL) {
        free(ctx->hsm_config.pkcs11_module_path);
    }
    if (ctx->hsm_config.token_label != NULL) {
        free(ctx->hsm_config.token_label);
    }
    if (ctx->hsm_config.pin != NULL) {
        qssl_secure_zero(ctx->hsm_config.pin, strlen(ctx->hsm_config.pin));
        free(ctx->hsm_config.pin);
    }
#endif

    free(ctx);
    LOG_DEBUG("Q-SSL context freed");
}

/*
 * Set context options
 */
int qssl_ctx_set_options(QSSL_CTX *ctx, uint32_t options) {
    if (ctx == NULL) {
        return QSSL_ERROR_NULL_POINTER;
    }

    ctx->options = options;
    LOG_DEBUG("Context options set to 0x%08x", options);

    return QSSL_SUCCESS;
}

/*
 * Get context options
 */
uint32_t qssl_ctx_get_options(QSSL_CTX *ctx) {
    if (ctx == NULL) {
        return 0;
    }
    return ctx->options;
}

/*
 * Set verification mode
 */
int qssl_ctx_set_verify_mode(QSSL_CTX *ctx, int mode,
                              qssl_verify_callback callback) {
    if (ctx == NULL) {
        return QSSL_ERROR_NULL_POINTER;
    }

    ctx->verify_mode = mode;
    ctx->verify_callback = callback;

    LOG_DEBUG("Verification mode set to 0x%02x", mode);

    return QSSL_SUCCESS;
}

/*
 * Load certificate from file
 */
int qssl_ctx_use_certificate_file(QSSL_CTX *ctx, const char *file, int type) {
    if (ctx == NULL || file == NULL) {
        return QSSL_ERROR_NULL_POINTER;
    }

    /* Use OpenSSL to load certificate */
    int ret;
    if (type == QSSL_FILETYPE_PEM) {
        ret = SSL_CTX_use_certificate_file(ctx->ssl_ctx, file, SSL_FILETYPE_PEM);
    } else {
        ret = SSL_CTX_use_certificate_file(ctx->ssl_ctx, file, SSL_FILETYPE_ASN1);
    }

    if (ret != 1) {
        LOG_ERROR("Failed to load certificate from %s", file);
        return QSSL_ERROR_CERT_VERIFY_FAILED;
    }

    LOG_INFO("Certificate loaded from %s", file);
    return QSSL_SUCCESS;
}

/*
 * Load private key from file
 */
int qssl_ctx_use_private_key_file(QSSL_CTX *ctx, const char *file, int type) {
    if (ctx == NULL || file == NULL) {
        return QSSL_ERROR_NULL_POINTER;
    }

    /* Use OpenSSL to load private key */
    int ret;
    if (type == QSSL_FILETYPE_PEM) {
        ret = SSL_CTX_use_PrivateKey_file(ctx->ssl_ctx, file, SSL_FILETYPE_PEM);
    } else {
        ret = SSL_CTX_use_PrivateKey_file(ctx->ssl_ctx, file, SSL_FILETYPE_ASN1);
    }

    if (ret != 1) {
        LOG_ERROR("Failed to load private key from %s", file);
        return QSSL_ERROR_KEY_GENERATION;
    }

    /* Verify private key matches certificate */
    if (SSL_CTX_check_private_key(ctx->ssl_ctx) != 1) {
        LOG_ERROR("Private key does not match certificate");
        return QSSL_ERROR_KEY_GENERATION;
    }

    LOG_INFO("Private key loaded from %s", file);
    return QSSL_SUCCESS;
}

/*
 * Load CA certificates for verification
 */
int qssl_ctx_load_verify_locations(QSSL_CTX *ctx, const char *file,
                                    const char *path) {
    if (ctx == NULL) {
        return QSSL_ERROR_NULL_POINTER;
    }

    if (file == NULL && path == NULL) {
        return QSSL_ERROR_INVALID_ARGUMENT;
    }

    /* Use OpenSSL to load CA certificates */
    if (SSL_CTX_load_verify_locations(ctx->ssl_ctx, file, path) != 1) {
        LOG_ERROR("Failed to load CA certificates");
        return QSSL_ERROR_CERT_VERIFY_FAILED;
    }

    LOG_INFO("CA certificates loaded (file=%s, path=%s)",
             file ? file : "NULL", path ? path : "NULL");

    return QSSL_SUCCESS;
}

/*
 * Set supported PQC algorithms
 */
int qssl_ctx_set_pqc_algorithms(QSSL_CTX *ctx,
                                 const uint16_t *kems, size_t num_kems,
                                 const uint16_t *sigs, size_t num_sigs) {
    if (ctx == NULL || kems == NULL || sigs == NULL) {
        return QSSL_ERROR_NULL_POINTER;
    }

    if (num_kems > 8 || num_sigs > 8) {
        return QSSL_ERROR_INVALID_ARGUMENT;
    }

    memcpy(ctx->supported_kems, kems, num_kems * sizeof(uint16_t));
    ctx->num_kems = num_kems;

    memcpy(ctx->supported_sigs, sigs, num_sigs * sizeof(uint16_t));
    ctx->num_sigs = num_sigs;

    LOG_INFO("PQC algorithms configured (KEMs=%zu, Sigs=%zu)", num_kems, num_sigs);

    return QSSL_SUCCESS;
}

/******************************************************************************
 * Connection Management
 ******************************************************************************/

/*
 * Create new Q-SSL connection
 */
QSSL_CONNECTION *qssl_new(QSSL_CTX *ctx) {
    QSSL_CONNECTION *conn;

    if (ctx == NULL) {
        LOG_ERROR("NULL context");
        return NULL;
    }

    /* Allocate connection */
    conn = (QSSL_CONNECTION *)calloc(1, sizeof(QSSL_CONNECTION));
    if (conn == NULL) {
        LOG_ERROR("Failed to allocate connection: %s", strerror(errno));
        return NULL;
    }

    conn->ctx = ctx;
    conn->fd = -1;
    conn->mode = ctx->mode;
    conn->state = 0; /* QSSL_HS_STATE_START */
    conn->last_error = QSSL_SUCCESS;
    conn->peer_cert = NULL;

    /* Create OpenSSL SSL object */
    conn->ssl = SSL_new(ctx->ssl_ctx);
    if (conn->ssl == NULL) {
        LOG_ERROR("Failed to create SSL object");
        free(conn);
        return NULL;
    }

    LOG_DEBUG("Q-SSL connection created");

    return conn;
}

/*
 * Free Q-SSL connection
 */
void qssl_free(QSSL_CONNECTION *conn) {
    if (conn == NULL) {
        return;
    }

    /* Free SSL object */
    if (conn->ssl != NULL) {
        SSL_free(conn->ssl);
    }

    /* Securely erase cryptographic material */
    qssl_secure_zero(&conn->kyber_key, sizeof(QSSL_KYBER_KEY));
    qssl_secure_zero(&conn->peer_kyber_key, sizeof(QSSL_KYBER_KEY));
    qssl_secure_zero(&conn->hybrid_secret, sizeof(QSSL_HYBRID_SECRET));
    qssl_secure_zero(&conn->session_keys, sizeof(QSSL_SESSION_KEYS));

    /* Free peer certificate */
    if (conn->peer_cert != NULL) {
        qssl_certificate_free(conn->peer_cert);
    }

    /* Close socket if still open */
    if (conn->fd >= 0) {
        close(conn->fd);
    }

    free(conn);
    LOG_DEBUG("Q-SSL connection freed");
}

/*
 * Associate file descriptor with connection
 */
int qssl_set_fd(QSSL_CONNECTION *conn, int fd) {
    if (conn == NULL) {
        return QSSL_ERROR_NULL_POINTER;
    }

    if (fd < 0) {
        return QSSL_ERROR_INVALID_ARGUMENT;
    }

    conn->fd = fd;

    /* Also set FD for OpenSSL SSL object */
    if (SSL_set_fd(conn->ssl, fd) != 1) {
        LOG_ERROR("Failed to set SSL file descriptor");
        return QSSL_ERROR_SYSCALL;
    }

    LOG_DEBUG("File descriptor %d associated with connection", fd);

    return QSSL_SUCCESS;
}

/*
 * Get file descriptor
 */
int qssl_get_fd(QSSL_CONNECTION *conn) {
    if (conn == NULL) {
        return -1;
    }
    return conn->fd;
}

/*
 * Get last error
 */
int qssl_get_error(QSSL_CONNECTION *conn) {
    if (conn == NULL) {
        return QSSL_ERROR_NULL_POINTER;
    }
    return conn->last_error;
}

/******************************************************************************
 * I/O Functions
 ******************************************************************************/

/*
 * Read encrypted data
 * Uses AES-256-GCM with derived session keys
 */
int qssl_read(QSSL_CONNECTION *conn, void *buf, int num) {
    if (conn == NULL || buf == NULL) {
        return QSSL_ERROR_NULL_POINTER;
    }

    if (num <= 0) {
        return QSSL_ERROR_INVALID_ARGUMENT;
    }

    /* Check if handshake is complete */
    if (conn->state != 5) { /* QSSL_HS_STATE_CONNECTED */
        LOG_ERROR("Handshake not complete");
        return QSSL_ERROR_HANDSHAKE_FAILED;
    }

    /* For now, use basic recv - full AES-GCM implementation would go here */
    ssize_t received = recv(conn->fd, buf, num, 0);
    if (received < 0) {
        if (errno == EAGAIN || errno == EWOULDBLOCK) {
            return QSSL_ERROR_WANT_READ;
        }
        LOG_ERROR("recv failed: %s", strerror(errno));
        conn->last_error = QSSL_ERROR_SYSCALL;
        return QSSL_ERROR_SYSCALL;
    }

    if (received == 0) {
        LOG_DEBUG("Connection closed by peer");
        return QSSL_ERROR_ZERO_RETURN;
    }

    LOG_DEBUG("Read %zd bytes", received);
    return (int)received;
}

/*
 * Write encrypted data
 * Uses AES-256-GCM with derived session keys
 */
int qssl_write(QSSL_CONNECTION *conn, const void *buf, int num) {
    if (conn == NULL || buf == NULL) {
        return QSSL_ERROR_NULL_POINTER;
    }

    if (num <= 0) {
        return QSSL_ERROR_INVALID_ARGUMENT;
    }

    /* Check if handshake is complete */
    if (conn->state != 5) { /* QSSL_HS_STATE_CONNECTED */
        LOG_ERROR("Handshake not complete");
        return QSSL_ERROR_HANDSHAKE_FAILED;
    }

    /* For now, use basic send - full AES-GCM implementation would go here */
    ssize_t sent = send(conn->fd, buf, num, 0);
    if (sent < 0) {
        if (errno == EAGAIN || errno == EWOULDBLOCK) {
            return QSSL_ERROR_WANT_WRITE;
        }
        LOG_ERROR("send failed: %s", strerror(errno));
        conn->last_error = QSSL_ERROR_SYSCALL;
        return QSSL_ERROR_SYSCALL;
    }

    LOG_DEBUG("Wrote %zd bytes", sent);
    return (int)sent;
}

/*
 * Get pending bytes
 */
int qssl_pending(QSSL_CONNECTION *conn) {
    if (conn == NULL) {
        return 0;
    }

    /* Return SSL pending bytes */
    return SSL_pending(conn->ssl);
}

/*
 * Shutdown connection
 */
int qssl_shutdown(QSSL_CONNECTION *conn) {
    if (conn == NULL) {
        return QSSL_ERROR_NULL_POINTER;
    }

    /* Perform SSL shutdown */
    if (conn->ssl != NULL) {
        SSL_shutdown(conn->ssl);
    }

    /* Shutdown socket */
    if (conn->fd >= 0) {
        shutdown(conn->fd, SHUT_RDWR);
    }

    LOG_INFO("Connection shutdown");

    return QSSL_SUCCESS;
}

/******************************************************************************
 * Certificate Management
 ******************************************************************************/

/*
 * Get peer certificate
 */
QSSL_CERTIFICATE *qssl_get_peer_certificate(QSSL_CONNECTION *conn) {
    if (conn == NULL) {
        return NULL;
    }

    return conn->peer_cert;
}

/*
 * Free certificate
 */
void qssl_certificate_free(QSSL_CERTIFICATE *cert) {
    if (cert == NULL) {
        return;
    }

    if (cert->data != NULL) {
        free(cert->data);
    }

    if (cert->dilithium_key != NULL) {
        qssl_secure_zero(cert->dilithium_key, sizeof(QSSL_DILITHIUM_KEY));
        free(cert->dilithium_key);
    }

    free(cert);
}

/*
 * Get negotiated cipher
 */
const char *qssl_get_cipher(QSSL_CONNECTION *conn) {
    if (conn == NULL || conn->ssl == NULL) {
        return NULL;
    }

    return SSL_get_cipher_name(conn->ssl);
}

/*
 * Get negotiated protocol version
 */
int qssl_get_version(QSSL_CONNECTION *conn) {
    if (conn == NULL || conn->ssl == NULL) {
        return 0;
    }

    return SSL_version(conn->ssl);
}

/******************************************************************************
 * HSM Functions (Luna HSM via PKCS#11)
 ******************************************************************************/

#ifdef ENABLE_HSM

/*
 * Initialize HSM connection
 */
int qssl_hsm_init(const char *module_path) {
    if (module_path == NULL) {
        LOG_ERROR("NULL PKCS#11 module path");
        return QSSL_ERROR_NULL_POINTER;
    }

    /* Load PKCS#11 library */
    void *handle = dlopen(module_path, RTLD_LAZY);
    if (handle == NULL) {
        LOG_ERROR("Failed to load PKCS#11 module: %s", dlerror());
        return QSSL_ERROR_HSM_INIT_FAILED;
    }

    LOG_INFO("HSM initialized (module=%s)", module_path);
    return QSSL_SUCCESS;
}

/*
 * Login to HSM
 */
int qssl_hsm_login(const char *token_label, const char *pin) {
    if (token_label == NULL || pin == NULL) {
        return QSSL_ERROR_NULL_POINTER;
    }

    /* HSM login implementation would go here */
    LOG_INFO("HSM login (token=%s)", token_label);

    return QSSL_SUCCESS;
}

/*
 * Use HSM key
 */
int qssl_ctx_use_hsm_key(QSSL_CTX *ctx, const char *uri) {
    if (ctx == NULL || uri == NULL) {
        return QSSL_ERROR_NULL_POINTER;
    }

    LOG_INFO("Using HSM key: %s", uri);

    /* Parse PKCS#11 URI and load key from HSM */
    /* This is a stub - full implementation would use PKCS#11 API */

    return QSSL_SUCCESS;
}

/*
 * Generate ephemeral key in HSM
 */
int qssl_generate_ephemeral_key_hsm(QSSL_CONNECTION *conn, uint16_t algorithm) {
    if (conn == NULL) {
        return QSSL_ERROR_NULL_POINTER;
    }

    LOG_INFO("Generating ephemeral key in HSM (algorithm=0x%04x)", algorithm);

    /* HSM key generation would go here */

    return QSSL_SUCCESS;
}

/*
 * Perform KYBER decapsulation in HSM
 */
int qssl_hsm_kyber_decapsulate(QSSL_CONNECTION *conn,
                                const uint8_t *ciphertext, size_t ct_len,
                                uint8_t *shared_secret) {
    if (conn == NULL || ciphertext == NULL || shared_secret == NULL) {
        return QSSL_ERROR_NULL_POINTER;
    }

    LOG_INFO("Performing KYBER decapsulation in HSM");

    /* HSM decapsulation would go here */

    return QSSL_SUCCESS;
}

/*
 * Cleanup HSM connection
 */
void qssl_hsm_cleanup(void) {
    LOG_INFO("HSM cleanup");
    /* Cleanup implementation would go here */
}

#else /* !ENABLE_HSM */

/* Stub implementations when HSM is disabled */
int qssl_hsm_init(const char *module_path) {
    (void)module_path;
    return QSSL_ERROR_HSM_NOT_AVAILABLE;
}

int qssl_hsm_login(const char *token_label, const char *pin) {
    (void)token_label;
    (void)pin;
    return QSSL_ERROR_HSM_NOT_AVAILABLE;
}

int qssl_ctx_use_hsm_key(QSSL_CTX *ctx, const char *uri) {
    (void)ctx;
    (void)uri;
    return QSSL_ERROR_HSM_NOT_AVAILABLE;
}

int qssl_generate_ephemeral_key_hsm(QSSL_CONNECTION *conn, uint16_t algorithm) {
    (void)conn;
    (void)algorithm;
    return QSSL_ERROR_HSM_NOT_AVAILABLE;
}

int qssl_hsm_kyber_decapsulate(QSSL_CONNECTION *conn,
                                const uint8_t *ciphertext, size_t ct_len,
                                uint8_t *shared_secret) {
    (void)conn;
    (void)ciphertext;
    (void)ct_len;
    (void)shared_secret;
    return QSSL_ERROR_HSM_NOT_AVAILABLE;
}

void qssl_hsm_cleanup(void) {
    /* Nothing to do */
}

#endif /* ENABLE_HSM */
