package main

import (
	"crypto/tls"
	"crypto/x509"
	"encoding/json"
	"errors"
	"fmt"
	"log"
	"net"
	"net/http"
	"strings"
)

// ErrorResponse is the uniform JSON error shape (same idea as the Kotlin sample’s ErrorResponse).
type ErrorResponse struct {
	Status    int    `json:"status"`
	Message   string `json:"message"`
	Exception string `json:"exception,omitempty"`
	Path      string `json:"path,omitempty"`
	Code      string `json:"code,omitempty"`
}

func writeError(w http.ResponseWriter, r *http.Request, status int, msg string, err error) {
	er := ErrorResponse{
		Status:  status,
		Message: msg,
		Path:    r.URL.Path,
	}
	if err != nil {
		er.Exception = err.Error()
		if code, _, _ := classifyOutboundErr(err); code != "" {
			er.Code = code
		}
	}
	w.Header().Set("Content-Type", "application/json")
	w.WriteHeader(status)
	if encErr := json.NewEncoder(w).Encode(er); encErr != nil {
		log.Printf("writeError encode: %v", encErr)
	}
}

// classifyOutboundErr maps TLS and dial failures to stable codes (Go analogue of Java SSLHandshakeException / PKIX paths).
func classifyOutboundErr(err error) (code string, status int, clientMsg string) {
	if err == nil {
		return "", 0, ""
	}
	var dns *net.DNSError
	if errors.As(err, &dns) {
		return "DNS", http.StatusBadGateway, "Could not resolve upstream host"
	}
	var opErr *net.OpError
	if errors.As(err, &opErr) && opErr.Op == "dial" {
		return "DIAL", http.StatusBadGateway, "Could not connect to upstream"
	}
	var certErr *tls.CertificateVerificationError
	if errors.As(err, &certErr) {
		return "TLS_VERIFY", http.StatusBadGateway, "Upstream TLS certificate verification failed"
	}
	var unk x509.UnknownAuthorityError
	if errors.As(err, &unk) {
		return "TLS_VERIFY", http.StatusBadGateway, "Upstream TLS certificate is not trusted (unknown authority)"
	}
	var hostErr x509.HostnameError
	if errors.As(err, &hostErr) {
		return "TLS_HOST", http.StatusBadGateway, "Upstream TLS certificate does not match host"
	}
	var recErr tls.RecordHeaderError
	if errors.As(err, &recErr) {
		return "TLS_RECORD", http.StatusBadGateway, "Invalid TLS response from upstream (possible protocol mismatch)"
	}
	msg := strings.ToLower(err.Error())
	if strings.Contains(msg, "handshake") || strings.Contains(msg, "tls: ") || strings.HasPrefix(msg, "tls:") {
		return "TLS_HANDSHAKE", http.StatusBadGateway, "TLS handshake with upstream failed"
	}
	return "", 0, ""
}

func writeOutboundError(w http.ResponseWriter, r *http.Request, err error) {
	code, st, clientMsg := classifyOutboundErr(err)
	if code != "" {
		writeError(w, r, st, clientMsg, err)
		return
	}
	writeError(w, r, http.StatusInternalServerError, err.Error(), err)
}

func writeJSON(w http.ResponseWriter, status int, v any) {
	w.Header().Set("Content-Type", "application/json")
	w.WriteHeader(status)
	if err := json.NewEncoder(w).Encode(v); err != nil {
		log.Printf("writeJSON: %v", err)
	}
}

func withRecover(next http.Handler) http.Handler {
	return http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
		defer func() {
			if rec := recover(); rec != nil {
				log.Printf("panic: %v", rec)
				writeError(w, r, http.StatusInternalServerError, "internal server error", fmt.Errorf("%v", rec))
			}
		}()
		next.ServeHTTP(w, r)
	})
}

func withCORS(next http.Handler) http.Handler {
	return http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
		h := w.Header()
		h.Set("Access-Control-Allow-Origin", "*")
		h.Set("Access-Control-Allow-Methods", "GET, POST, PATCH, PUT, DELETE, OPTIONS")
		h.Set("Access-Control-Allow-Headers", "Content-Type, Accept, X-Environment, Idempotency-Key, X-API-Key, x-correlation-id")
		if r.Method == http.MethodOptions {
			w.WriteHeader(http.StatusNoContent)
			return
		}
		next.ServeHTTP(w, r)
	})
}
