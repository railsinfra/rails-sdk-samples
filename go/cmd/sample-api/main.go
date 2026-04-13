package main

import (
	"log"
	"net/http"
	"os"
	"strconv"
	"strings"
)

func normalizeBaseURL(raw string) string {
	t := strings.TrimSpace(strings.TrimSuffix(raw, "/"))
	if t == "" {
		return "https://api.railsinfra.com/"
	}
	low := strings.ToLower(t)
	if !strings.HasPrefix(low, "http://") && !strings.HasPrefix(low, "https://") {
		t = "https://" + t
	}
	return strings.TrimSuffix(t, "/") + "/"
}

func main() {
	port := 8083
	if p := os.Getenv("PORT"); p != "" {
		if v, err := strconv.Atoi(p); err == nil {
			port = v
		}
	}

	baseURL := normalizeBaseURL(os.Getenv("RAILS_BASE_URL"))
	apiKey := os.Getenv("RAILS_API_KEY")

	s := &srv{
		baseURL: baseURL,
		apiKey:  apiKey,
		proxy:   newProxyHTTPClient(),
		rail:    newRailsClient(),
	}

	mux := http.NewServeMux()
	mux.HandleFunc("GET /{$}", s.serveSwaggerUI)
	mux.HandleFunc("GET /openapi.json", s.serveOpenAPI)
	mux.HandleFunc("GET /health", s.health)

	registerDual(mux, "GET", "/api/v1/accounts", s.forwardListAccounts)
	registerDual(mux, "POST", "/api/v1/accounts", s.forwardCreateAccount)
	registerDual(mux, "GET", "/api/v1/accounts/{id}", s.accountGet)
	registerDual(mux, "DELETE", "/api/v1/accounts/{id}", s.accountClose)
	registerDual(mux, "PATCH", "/api/v1/accounts/{id}", s.accountPatchStatus)
	registerDual(mux, "PATCH", "/api/v1/accounts/{id}/status", s.accountPatchStatus)
	registerDual(mux, "POST", "/api/v1/accounts/{id}/deposit", s.forwardDeposit)
	registerDual(mux, "POST", "/api/v1/accounts/{id}/transfer", s.forwardTransfer)
	registerDual(mux, "POST", "/api/v1/accounts/{id}/withdraw", s.forwardWithdraw)

	registerDual(mux, "GET", "/api/v1/transactions/{id}", s.transactionGet)
	registerDual(mux, "GET", "/api/v1/accounts/{accountId}/transactions", s.transactionsByAccount)

	registerDual(mux, "GET", "/api/v1/raw/get", s.rawGet)
	registerDual(mux, "POST", "/api/v1/raw/post", s.rawPost)

	addr := ":" + strconv.Itoa(port)
	log.Printf("rails Go sample API on http://localhost%s (swagger at /)", addr)
	if err := http.ListenAndServe(addr, withRecover(withCORS(mux))); err != nil {
		log.Fatal(err)
	}
}
