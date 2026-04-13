package main

import (
	"crypto/x509"
	"errors"
	"net/http"
	"strings"
	"testing"
)

func TestClassifyOutboundErr_TLS(t *testing.T) {
	code, st, msg := classifyOutboundErr(x509.UnknownAuthorityError{})
	if code != "TLS_VERIFY" || st != http.StatusBadGateway || !strings.Contains(msg, "trust") {
		t.Fatalf("got code=%q st=%d msg=%q", code, st, msg)
	}
	code, st, _ = classifyOutboundErr(errors.New("remote error: tls: handshake failure"))
	if code != "TLS_HANDSHAKE" || st != http.StatusBadGateway {
		t.Fatalf("handshake: got code=%q st=%d", code, st)
	}
}
