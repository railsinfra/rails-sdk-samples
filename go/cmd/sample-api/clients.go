package main

import (
	"crypto/tls"
	"log"
	"net/http"
	"os"
	"strings"

	"github.com/stainless-sdks/rails-go"
	"github.com/stainless-sdks/rails-go/option"
)

func insecureTLS() bool {
	return strings.EqualFold(os.Getenv("RAILS_INSECURE_SSL"), "true")
}

func newInsecureTransport() *http.Transport {
	return &http.Transport{
		TLSClientConfig: &tls.Config{InsecureSkipVerify: true}, //nolint:gosec // dev/staging only; gated by RAILS_INSECURE_SSL
	}
}

func newProxyHTTPClient() *http.Client {
	if insecureTLS() {
		log.Println("[rails-go-sample] outbound HTTP client: trust-all TLS ON (RAILS_INSECURE_SSL=true) — dev/staging only")
		return &http.Client{Transport: newInsecureTransport()}
	}
	log.Println("[rails-go-sample] outbound HTTP client: default TLS (set RAILS_INSECURE_SSL=true if handshake/PKIX fails against a private CA)")
	return &http.Client{}
}

func newRailsClient() rails.Client {
	var opts []option.RequestOption
	if insecureTLS() {
		opts = append(opts, option.WithHTTPClient(&http.Client{Transport: newInsecureTransport()}))
	}
	return rails.NewClient(opts...)
}
