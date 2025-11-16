/*
 * Q-SSL: 양자 내성 보안 소켓 계층
 * Copyright 2025 QSIGN Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 *
 * 하이브리드 암호화(고전 + PQC)를 사용하여 양자 내성
 * SSL/TLS 구현을 제공하는 Q-SSL 라이브러리의 메인 API 헤더입니다.
 */

#ifndef QSSL_H
#define QSSL_H

#include <stdint.h>
#include <stddef.h>
#include <sys/types.h>

#ifdef __cplusplus
extern "C" {
#endif

/* Version information */
#define QSSL_VERSION_MAJOR 1
#define QSSL_VERSION_MINOR 0
#define QSSL_VERSION_PATCH 0
#define QSSL_VERSION_STRING "1.0.0"

/* Operating modes */
#define QSSL_CLIENT_MODE 0
#define QSSL_SERVER_MODE 1

/* Protocol versions */
#define QSSL_VERSION_SSLv3  0x0300
#define QSSL_VERSION_TLSv1  0x0301
#define QSSL_VERSION_TLSv1_1 0x0302
#define QSSL_VERSION_TLSv1_2 0x0303
#define QSSL_VERSION_TLSv1_3 0x0304

/* Algorithm identifiers */
/* Post-Quantum KEMs (NIST standardized) */
#define QSSL_KEM_KYBER512     0x0001
#define QSSL_KEM_KYBER768     0x0002
#define QSSL_KEM_KYBER1024    0x0003  /* Default - highest security */

/* Post-Quantum Signatures (NIST standardized) */
#define QSSL_SIG_DILITHIUM2   0x0101
#define QSSL_SIG_DILITHIUM3   0x0102  /* Default - highest security */
#define QSSL_SIG_DILITHIUM5   0x0103

/* Classical KEMs */
#define QSSL_KEM_ECDHE_P256   0x0201
#define QSSL_KEM_ECDHE_P384   0x0202  /* Default for hybrid */
#define QSSL_KEM_ECDHE_P521   0x0203

/* Classical Signatures */
#define QSSL_SIG_RSA_2048     0x0301
#define QSSL_SIG_RSA_4096     0x0302
#define QSSL_SIG_ECDSA_P256   0x0303
#define QSSL_SIG_ECDSA_P384   0x0304

/* Symmetric ciphers */
#define QSSL_CIPHER_AES_128_GCM      0x0401
#define QSSL_CIPHER_AES_256_GCM      0x0402  /* Default */
#define QSSL_CIPHER_CHACHA20_POLY1305 0x0403

/* Context options */
#define QSSL_OP_NO_SSLv2              0x00000001
#define QSSL_OP_NO_SSLv3              0x00000002
#define QSSL_OP_NO_TLSv1              0x00000004
#define QSSL_OP_NO_TLSv1_1            0x00000008
#define QSSL_OP_NO_TLSv1_2            0x00000010
#define QSSL_OP_HYBRID_MODE           0x00000100  /* Enable PQC hybrid */
#define QSSL_OP_PQC_ONLY              0x00000200  /* PQC only (experimental) */
#define QSSL_OP_CLASSICAL_ONLY        0x00000400  /* Classical only */

/* Verification modes */
#define QSSL_VERIFY_NONE               0x00
#define QSSL_VERIFY_PEER               0x01
#define QSSL_VERIFY_FAIL_IF_NO_PEER_CERT 0x02
#define QSSL_VERIFY_CLIENT_ONCE        0x04

/* File types */
#define QSSL_FILETYPE_PEM     1
#define QSSL_FILETYPE_ASN1    2

/* Error codes */
#define QSSL_SUCCESS                    0
#define QSSL_ERROR_NONE                 0
#define QSSL_ERROR_GENERIC             -1
#define QSSL_ERROR_NULL_POINTER        -2
#define QSSL_ERROR_INVALID_ARGUMENT    -3
#define QSSL_ERROR_OUT_OF_MEMORY       -4
#define QSSL_ERROR_SYSCALL             -5
#define QSSL_ERROR_WANT_READ           -6
#define QSSL_ERROR_WANT_WRITE          -7
#define QSSL_ERROR_ZERO_RETURN         -8

/* Crypto errors */
#define QSSL_ERROR_CRYPTO_INIT         -100
#define QSSL_ERROR_KEY_GENERATION      -101
#define QSSL_ERROR_ENCAPSULATION       -102
#define QSSL_ERROR_DECAPSULATION       -103
#define QSSL_ERROR_SIGNATURE           -104
#define QSSL_ERROR_VERIFICATION        -105
#define QSSL_ERROR_KEY_DERIVATION      -106
#define QSSL_ERROR_ENCRYPTION          -107
#define QSSL_ERROR_DECRYPTION          -108

/* Protocol errors */
#define QSSL_ERROR_HANDSHAKE_FAILED    -200
#define QSSL_ERROR_PROTOCOL_VERSION    -201
#define QSSL_ERROR_CERT_VERIFY_FAILED  -202
#define QSSL_ERROR_PEER_CLOSED         -203
#define QSSL_ERROR_INVALID_MESSAGE     -204
#define QSSL_ERROR_UNSUPPORTED_ALGO    -205

/* HSM errors */
#define QSSL_ERROR_HSM_NOT_AVAILABLE   -300
#define QSSL_ERROR_HSM_INIT_FAILED     -301
#define QSSL_ERROR_HSM_LOGIN_FAILED    -302
#define QSSL_ERROR_HSM_KEY_NOT_FOUND   -303
#define QSSL_ERROR_HSM_OPERATION_FAILED -304

/* Maximum sizes */
#define QSSL_MAX_CERT_CHAIN_LEN   10
#define QSSL_MAX_SESSION_ID_LEN   32
#define QSSL_MAX_RANDOM_LEN       32
#define QSSL_MAX_MASTER_SECRET    48

/* KYBER1024 constants (ML-KEM-1024) */
#define QSSL_KYBER1024_PUBLIC_KEY_BYTES   1568
#define QSSL_KYBER1024_SECRET_KEY_BYTES   3168
#define QSSL_KYBER1024_CIPHERTEXT_BYTES   1568
#define QSSL_KYBER1024_SHARED_SECRET_BYTES 32

/* DILITHIUM3 constants (ML-DSA-65) */
#define QSSL_DILITHIUM3_PUBLIC_KEY_BYTES  1952
#define QSSL_DILITHIUM3_SECRET_KEY_BYTES  4000
#define QSSL_DILITHIUM3_SIGNATURE_BYTES   3293

/* ECDHE P-384 constants */
#define QSSL_ECDHE_P384_PUBLIC_KEY_BYTES  97
#define QSSL_ECDHE_P384_SHARED_SECRET_BYTES 48

/*
 * Forward declarations
 */
typedef struct qssl_ctx_st QSSL_CTX;
typedef struct qssl_connection_st QSSL_CONNECTION;
typedef struct qssl_x509_st QSSL_X509;

/*
 * KYBER1024 key structure
 * Used for post-quantum key encapsulation
 */
typedef struct {
    uint8_t public_key[QSSL_KYBER1024_PUBLIC_KEY_BYTES];
    uint8_t secret_key[QSSL_KYBER1024_SECRET_KEY_BYTES];
    uint8_t ciphertext[QSSL_KYBER1024_CIPHERTEXT_BYTES];
    uint8_t shared_secret[QSSL_KYBER1024_SHARED_SECRET_BYTES];
    int has_secret_key;    /* 1 if secret key is present */
    int has_shared_secret; /* 1 if shared secret is derived */
} QSSL_KYBER_KEY;

/*
 * DILITHIUM3 key structure
 * Used for post-quantum digital signatures
 */
typedef struct {
    uint8_t public_key[QSSL_DILITHIUM3_PUBLIC_KEY_BYTES];
    uint8_t secret_key[QSSL_DILITHIUM3_SECRET_KEY_BYTES];
    int has_secret_key;    /* 1 if secret key is present */
} QSSL_DILITHIUM_KEY;

/*
 * ECDHE P-384 key structure
 * Used for classical key exchange
 */
typedef struct {
    uint8_t public_key[QSSL_ECDHE_P384_PUBLIC_KEY_BYTES];
    uint8_t shared_secret[QSSL_ECDHE_P384_SHARED_SECRET_BYTES];
    void *evp_pkey;        /* OpenSSL EVP_PKEY pointer */
    int has_shared_secret; /* 1 if shared secret is derived */
} QSSL_ECDHE_KEY;

/*
 * Hybrid master secret
 * Combines classical and PQC shared secrets
 */
typedef struct {
    uint8_t classical_secret[QSSL_ECDHE_P384_SHARED_SECRET_BYTES];
    uint8_t pqc_secret[QSSL_KYBER1024_SHARED_SECRET_BYTES];
    uint8_t master_secret[QSSL_MAX_MASTER_SECRET];
    uint8_t client_random[QSSL_MAX_RANDOM_LEN];
    uint8_t server_random[QSSL_MAX_RANDOM_LEN];
} QSSL_HYBRID_SECRET;

/*
 * Session keys derived from master secret
 */
typedef struct {
    uint8_t client_write_key[32];
    uint8_t server_write_key[32];
    uint8_t client_write_iv[12];
    uint8_t server_write_iv[12];
} QSSL_SESSION_KEYS;

/*
 * Certificate structure
 */
typedef struct {
    uint8_t *data;
    size_t length;
    int format; /* QSSL_FILETYPE_PEM or QSSL_FILETYPE_ASN1 */
    void *x509; /* OpenSSL X509 pointer */
    QSSL_DILITHIUM_KEY *dilithium_key;
    int verified;
} QSSL_CERTIFICATE;

/*
 * HSM configuration for PKCS#11
 */
typedef struct {
    char *pkcs11_module_path;  /* Path to PKCS#11 library */
    char *token_label;         /* HSM token label */
    char *pin;                 /* HSM PIN (stored securely) */
    void *pkcs11_handle;       /* dlopen handle */
    void *function_list;       /* CK_FUNCTION_LIST pointer */
    unsigned long session;     /* CK_SESSION_HANDLE */
    int initialized;           /* 1 if HSM is initialized */
} QSSL_HSM_CONFIG;

/*
 * Callback function types
 */
typedef int (*qssl_verify_callback)(int preverify_ok, QSSL_X509 *x509_ctx);
typedef int (*qssl_psk_callback)(QSSL_CONNECTION *conn, const char *hint,
                                  char *identity, unsigned int max_identity_len,
                                  unsigned char *psk, unsigned int max_psk_len);

/******************************************************************************
 * Context Management Functions
 ******************************************************************************/

/*
 * Create a new Q-SSL context
 * mode: QSSL_CLIENT_MODE or QSSL_SERVER_MODE
 * Returns: New context or NULL on error
 */
QSSL_CTX *qssl_ctx_new(int mode);

/*
 * Free a Q-SSL context
 */
void qssl_ctx_free(QSSL_CTX *ctx);

/*
 * Set options on context
 * options: Bitmask of QSSL_OP_* flags
 * Returns: QSSL_SUCCESS or error code
 */
int qssl_ctx_set_options(QSSL_CTX *ctx, uint32_t options);

/*
 * Get current options
 * Returns: Current option flags
 */
uint32_t qssl_ctx_get_options(QSSL_CTX *ctx);

/*
 * Set verification mode
 * mode: QSSL_VERIFY_* flags
 * callback: Optional verification callback
 * Returns: QSSL_SUCCESS or error code
 */
int qssl_ctx_set_verify_mode(QSSL_CTX *ctx, int mode, qssl_verify_callback callback);

/*
 * Load certificate from file
 * file: Path to certificate file
 * type: QSSL_FILETYPE_PEM or QSSL_FILETYPE_ASN1
 * Returns: QSSL_SUCCESS or error code
 */
int qssl_ctx_use_certificate_file(QSSL_CTX *ctx, const char *file, int type);

/*
 * Load private key from file
 * file: Path to private key file
 * type: QSSL_FILETYPE_PEM or QSSL_FILETYPE_ASN1
 * Returns: QSSL_SUCCESS or error code
 */
int qssl_ctx_use_private_key_file(QSSL_CTX *ctx, const char *file, int type);

/*
 * Load private key from HSM using PKCS#11 URI
 * uri: PKCS#11 URI (e.g., "pkcs11:token=luna;object=mykey")
 * Returns: QSSL_SUCCESS or error code
 */
int qssl_ctx_use_hsm_key(QSSL_CTX *ctx, const char *uri);

/*
 * Load CA certificates for verification
 * file: Path to CA bundle file (can be NULL)
 * path: Path to CA directory (can be NULL)
 * Returns: QSSL_SUCCESS or error code
 */
int qssl_ctx_load_verify_locations(QSSL_CTX *ctx, const char *file, const char *path);

/*
 * Set supported PQC algorithms
 * kems: Array of KEM algorithm IDs
 * num_kems: Number of KEMs in array
 * sigs: Array of signature algorithm IDs
 * num_sigs: Number of signatures in array
 * Returns: QSSL_SUCCESS or error code
 */
int qssl_ctx_set_pqc_algorithms(QSSL_CTX *ctx,
                                 const uint16_t *kems, size_t num_kems,
                                 const uint16_t *sigs, size_t num_sigs);

/******************************************************************************
 * Connection Management Functions
 ******************************************************************************/

/*
 * Create a new Q-SSL connection
 * ctx: Q-SSL context
 * Returns: New connection or NULL on error
 */
QSSL_CONNECTION *qssl_new(QSSL_CTX *ctx);

/*
 * Free a Q-SSL connection
 */
void qssl_free(QSSL_CONNECTION *conn);

/*
 * Associate file descriptor with connection
 * fd: Socket file descriptor
 * Returns: QSSL_SUCCESS or error code
 */
int qssl_set_fd(QSSL_CONNECTION *conn, int fd);

/*
 * Get file descriptor
 * Returns: File descriptor or -1
 */
int qssl_get_fd(QSSL_CONNECTION *conn);

/******************************************************************************
 * Handshake Functions
 ******************************************************************************/

/*
 * Perform client-side handshake
 * Returns: QSSL_SUCCESS or error code
 */
int qssl_connect(QSSL_CONNECTION *conn);

/*
 * Perform server-side handshake
 * Returns: QSSL_SUCCESS or error code
 */
int qssl_accept(QSSL_CONNECTION *conn);

/*
 * Set server name indication (SNI) for client
 * hostname: Server hostname
 * Returns: QSSL_SUCCESS or error code
 */
int qssl_set_server_name(QSSL_CONNECTION *conn, const char *hostname);

/*
 * Verify peer certificate
 * Returns: 1 if verified, 0 if not verified or error
 */
int qssl_verify_peer_certificate(QSSL_CONNECTION *conn);

/******************************************************************************
 * I/O Functions
 ******************************************************************************/

/*
 * Read encrypted data from connection
 * buf: Buffer to store data
 * num: Maximum bytes to read
 * Returns: Number of bytes read, or error code
 */
int qssl_read(QSSL_CONNECTION *conn, void *buf, int num);

/*
 * Write encrypted data to connection
 * buf: Data to write
 * num: Number of bytes to write
 * Returns: Number of bytes written, or error code
 */
int qssl_write(QSSL_CONNECTION *conn, const void *buf, int num);

/*
 * Get number of bytes pending
 * Returns: Number of bytes available to read
 */
int qssl_pending(QSSL_CONNECTION *conn);

/*
 * Shutdown connection
 * Returns: QSSL_SUCCESS or error code
 */
int qssl_shutdown(QSSL_CONNECTION *conn);

/******************************************************************************
 * Cryptographic Functions
 ******************************************************************************/

/*
 * Generate KYBER1024 keypair
 * key: KYBER key structure to populate
 * Returns: QSSL_SUCCESS or error code
 */
int qssl_kyber_keygen(QSSL_KYBER_KEY *key);

/*
 * KYBER1024 encapsulation (client side)
 * key: KYBER key structure with public key
 * Returns: QSSL_SUCCESS or error code
 */
int qssl_kyber_encapsulate(QSSL_KYBER_KEY *key);

/*
 * KYBER1024 decapsulation (server side)
 * key: KYBER key structure with secret key and ciphertext
 * Returns: QSSL_SUCCESS or error code
 */
int qssl_kyber_decapsulate(QSSL_KYBER_KEY *key);

/*
 * Generate DILITHIUM3 keypair
 * key: DILITHIUM key structure to populate
 * Returns: QSSL_SUCCESS or error code
 */
int qssl_dilithium_keygen(QSSL_DILITHIUM_KEY *key);

/*
 * DILITHIUM3 sign message
 * key: DILITHIUM key structure with secret key
 * msg: Message to sign
 * msg_len: Length of message
 * sig: Buffer for signature
 * sig_len: Output signature length
 * Returns: QSSL_SUCCESS or error code
 */
int qssl_dilithium_sign(const QSSL_DILITHIUM_KEY *key,
                        const uint8_t *msg, size_t msg_len,
                        uint8_t *sig, size_t *sig_len);

/*
 * DILITHIUM3 verify signature
 * key: DILITHIUM key structure with public key
 * msg: Message that was signed
 * msg_len: Length of message
 * sig: Signature to verify
 * sig_len: Length of signature
 * Returns: 1 if valid, 0 if invalid, negative on error
 */
int qssl_dilithium_verify(const QSSL_DILITHIUM_KEY *key,
                          const uint8_t *msg, size_t msg_len,
                          const uint8_t *sig, size_t sig_len);

/*
 * Derive hybrid master secret
 * secret: Hybrid secret structure to populate
 * Returns: QSSL_SUCCESS or error code
 */
int qssl_derive_master_secret(QSSL_HYBRID_SECRET *secret);

/*
 * Derive session keys from master secret
 * secret: Hybrid secret structure
 * keys: Session keys structure to populate
 * Returns: QSSL_SUCCESS or error code
 */
int qssl_derive_session_keys(const QSSL_HYBRID_SECRET *secret,
                              QSSL_SESSION_KEYS *keys);

/******************************************************************************
 * HSM Functions (Luna HSM via PKCS#11)
 ******************************************************************************/

/*
 * Initialize HSM connection
 * module_path: Path to PKCS#11 library
 * Returns: QSSL_SUCCESS or error code
 */
int qssl_hsm_init(const char *module_path);

/*
 * Login to HSM
 * token_label: HSM token label
 * pin: User PIN
 * Returns: QSSL_SUCCESS or error code
 */
int qssl_hsm_login(const char *token_label, const char *pin);

/*
 * Generate ephemeral key in HSM
 * conn: Q-SSL connection
 * algorithm: QSSL_KEM_KYBER1024 or QSSL_SIG_DILITHIUM3
 * Returns: QSSL_SUCCESS or error code
 */
int qssl_generate_ephemeral_key_hsm(QSSL_CONNECTION *conn, uint16_t algorithm);

/*
 * Perform KYBER decapsulation in HSM
 * conn: Q-SSL connection
 * ciphertext: KYBER ciphertext
 * ct_len: Length of ciphertext
 * shared_secret: Output buffer for shared secret
 * Returns: QSSL_SUCCESS or error code
 */
int qssl_hsm_kyber_decapsulate(QSSL_CONNECTION *conn,
                                const uint8_t *ciphertext, size_t ct_len,
                                uint8_t *shared_secret);

/*
 * Cleanup HSM connection
 */
void qssl_hsm_cleanup(void);

/******************************************************************************
 * Utility Functions
 ******************************************************************************/

/*
 * Get error string for error code
 * error: Error code
 * Returns: Human-readable error string
 */
const char *qssl_get_error_string(int error);

/*
 * Get last error for connection
 * conn: Q-SSL connection
 * Returns: Last error code
 */
int qssl_get_error(QSSL_CONNECTION *conn);

/*
 * Get library version string
 * Returns: Version string (e.g., "1.0.0")
 */
const char *qssl_version(void);

/*
 * Get negotiated cipher suite
 * conn: Q-SSL connection
 * Returns: Cipher suite string or NULL
 */
const char *qssl_get_cipher(QSSL_CONNECTION *conn);

/*
 * Get negotiated protocol version
 * conn: Q-SSL connection
 * Returns: Protocol version (e.g., QSSL_VERSION_TLSv1_3)
 */
int qssl_get_version(QSSL_CONNECTION *conn);

/*
 * Get peer certificate
 * conn: Q-SSL connection
 * Returns: Certificate or NULL
 */
QSSL_CERTIFICATE *qssl_get_peer_certificate(QSSL_CONNECTION *conn);

/*
 * Free certificate
 */
void qssl_certificate_free(QSSL_CERTIFICATE *cert);

/*
 * Secure memory zeroing (constant-time)
 * ptr: Memory to zero
 * len: Length of memory
 */
void qssl_secure_zero(void *ptr, size_t len);

#ifdef __cplusplus
}
#endif

#endif /* QSSL_H */
