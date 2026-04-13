package main

import (
	"encoding/json"
	"io"
	"net/http"
	"strconv"
	"strings"

	"github.com/rails/sdk-samples/go/apidocs"
	"github.com/stainless-sdks/rails-go"
	"github.com/stainless-sdks/rails-go/option"
	"github.com/stainless-sdks/rails-go/packages/param"
)

type srv struct {
	baseURL   string
	apiKey    string
	proxy     *http.Client
	rail      rails.Client
}

func registerDual(mux *http.ServeMux, method, patternV1 string, h http.HandlerFunc) {
	mux.HandleFunc(method+" "+patternV1, h)
	alt := strings.Replace(patternV1, "/api/v1/", "/api/", 1)
	if alt != patternV1 {
		mux.HandleFunc(method+" "+alt, h)
	}
}

func (s *srv) serveSwaggerUI(w http.ResponseWriter, _ *http.Request) {
	b, err := apidocs.Files.ReadFile("swagger-ui.html")
	if err != nil {
		http.Error(w, "swagger ui missing", http.StatusInternalServerError)
		return
	}
	w.Header().Set("Content-Type", "text/html; charset=utf-8")
	_, _ = w.Write(b)
}

func (s *srv) serveOpenAPI(w http.ResponseWriter, _ *http.Request) {
	b, err := apidocs.Files.ReadFile("openapi.json")
	if err != nil {
		http.Error(w, "openapi missing", http.StatusInternalServerError)
		return
	}
	w.Header().Set("Content-Type", "application/json")
	_, _ = w.Write(b)
}

func (s *srv) health(w http.ResponseWriter, _ *http.Request) {
	writeJSON(w, http.StatusOK, map[string]string{"status": "ok"})
}

func envOpts(r *http.Request) []option.RequestOption {
	x := r.Header.Get("X-Environment")
	if x != "sandbox" && x != "production" {
		x = "sandbox"
	}
	return []option.RequestOption{option.WithHeader("X-Environment", x)}
}

func (s *srv) requireAPIKey(w http.ResponseWriter, r *http.Request) bool {
	if strings.TrimSpace(s.apiKey) != "" {
		return true
	}
	writeError(w, r, http.StatusBadRequest, "RAILS_API_KEY is not set", nil)
	return false
}

func (s *srv) forward(w http.ResponseWriter, r *http.Request, method, upstreamPath string, body io.Reader, headerMode string) {
	if !s.requireAPIKey(w, r) {
		return
	}
	base := strings.TrimSuffix(strings.TrimSpace(s.baseURL), "/")
	url := base + "/" + strings.TrimPrefix(upstreamPath, "/")
	if r.URL.RawQuery != "" {
		url += "?" + r.URL.RawQuery
	}
	req, err := http.NewRequestWithContext(r.Context(), method, url, body)
	if err != nil {
		writeError(w, r, http.StatusBadRequest, "bad upstream request", err)
		return
	}

	req.Header.Set("X-API-Key", s.apiKey)
	switch headerMode {
	case "envAlways":
		x := r.Header.Get("X-Environment")
		if x == "" {
			x = "sandbox"
		}
		req.Header.Set("X-Environment", x)
	case "envOptional":
		if x := r.Header.Get("X-Environment"); x == "sandbox" || x == "production" {
			req.Header.Set("X-Environment", x)
		}
	default:
		break
	}
	if ct := r.Header.Get("Content-Type"); ct != "" {
		req.Header.Set("Content-Type", ct)
	}
	if ik := r.Header.Get("Idempotency-Key"); ik != "" {
		req.Header.Set("Idempotency-Key", ik)
	}

	resp, err := s.proxy.Do(req)
	if err != nil {
		writeOutboundError(w, r, err)
		return
	}
	defer resp.Body.Close()
	for k, vv := range resp.Header {
		if strings.EqualFold(k, "content-length") {
			continue
		}
		for _, v := range vv {
			w.Header().Add(k, v)
		}
	}
	w.WriteHeader(resp.StatusCode)
	_, _ = io.Copy(w, resp.Body)
}

func (s *srv) forwardListAccounts(w http.ResponseWriter, r *http.Request) {
	s.forward(w, r, http.MethodGet, "api/v1/accounts", nil, "envAlways")
}

func (s *srv) forwardCreateAccount(w http.ResponseWriter, r *http.Request) {
	s.forward(w, r, http.MethodPost, "api/v1/accounts", r.Body, "envAlways")
}

func (s *srv) forwardDeposit(w http.ResponseWriter, r *http.Request) {
	id := r.PathValue("id")
	if id == "" {
		writeError(w, r, http.StatusBadRequest, "missing id", nil)
		return
	}
	s.forward(w, r, http.MethodPost, "api/v1/accounts/"+id+"/deposit", r.Body, "envOptional")
}

func (s *srv) forwardTransfer(w http.ResponseWriter, r *http.Request) {
	id := r.PathValue("id")
	if id == "" {
		writeError(w, r, http.StatusBadRequest, "missing id", nil)
		return
	}
	s.forward(w, r, http.MethodPost, "api/v1/accounts/"+id+"/transfer", r.Body, "envOptional")
}

func (s *srv) forwardWithdraw(w http.ResponseWriter, r *http.Request) {
	id := r.PathValue("id")
	if id == "" {
		writeError(w, r, http.StatusBadRequest, "missing id", nil)
		return
	}
	s.forward(w, r, http.MethodPost, "api/v1/accounts/"+id+"/withdraw", r.Body, "envOptional")
}

func (s *srv) rawGet(w http.ResponseWriter, r *http.Request) {
	p := r.URL.Query().Get("path")
	if p == "" {
		p = "api/v1/accounts"
	}
	s.forward(w, r, http.MethodGet, strings.TrimPrefix(p, "/"), nil, "envOptional")
}

func (s *srv) rawPost(w http.ResponseWriter, r *http.Request) {
	p := r.URL.Query().Get("path")
	if p == "" {
		p = "api/v1/accounts"
	}
	s.forward(w, r, http.MethodPost, strings.TrimPrefix(p, "/"), r.Body, "envOptional")
}

func (s *srv) accountGet(w http.ResponseWriter, r *http.Request) {
	id := r.PathValue("id")
	if id == "" {
		writeError(w, r, http.StatusBadRequest, "missing id", nil)
		return
	}
	res, err := s.rail.Accounts.Get(r.Context(), id, envOpts(r)...)
	if err != nil {
		writeOutboundError(w, r, err)
		return
	}
	writeJSON(w, http.StatusOK, res)
}

func (s *srv) accountClose(w http.ResponseWriter, r *http.Request) {
	id := r.PathValue("id")
	if id == "" {
		writeError(w, r, http.StatusBadRequest, "missing id", nil)
		return
	}
	res, err := s.rail.Accounts.Close(r.Context(), id, envOpts(r)...)
	if err != nil {
		writeOutboundError(w, r, err)
		return
	}
	writeJSON(w, http.StatusOK, res)
}

type statusBody struct {
	Status string `json:"status"`
}

func (s *srv) accountPatchStatus(w http.ResponseWriter, r *http.Request) {
	id := r.PathValue("id")
	if id == "" {
		writeError(w, r, http.StatusBadRequest, "missing id", nil)
		return
	}
	var body statusBody
	if err := json.NewDecoder(r.Body).Decode(&body); err != nil {
		writeError(w, r, http.StatusBadRequest, "invalid JSON body", err)
		return
	}
	if body.Status == "" {
		writeError(w, r, http.StatusBadRequest, "missing status", nil)
		return
	}
	st := rails.AccountUpdateStatusParamsStatus(body.Status)
	switch st {
	case rails.AccountUpdateStatusParamsStatusActive,
		rails.AccountUpdateStatusParamsStatusSuspended,
		rails.AccountUpdateStatusParamsStatusClosed:
	default:
		writeError(w, r, http.StatusBadRequest, "invalid status", nil)
		return
	}
	res, err := s.rail.Accounts.UpdateStatus(r.Context(), id, rails.AccountUpdateStatusParams{Status: st}, envOpts(r)...)
	if err != nil {
		writeOutboundError(w, r, err)
		return
	}
	writeJSON(w, http.StatusOK, res)
}

func (s *srv) transactionGet(w http.ResponseWriter, r *http.Request) {
	id := r.PathValue("id")
	if id == "" {
		writeError(w, r, http.StatusBadRequest, "missing id", nil)
		return
	}
	res, err := s.rail.Transactions.Get(r.Context(), id, envOpts(r)...)
	if err != nil {
		writeOutboundError(w, r, err)
		return
	}
	writeJSON(w, http.StatusOK, res)
}

func (s *srv) transactionsByAccount(w http.ResponseWriter, r *http.Request) {
	accountID := r.PathValue("accountId")
	if accountID == "" {
		writeError(w, r, http.StatusBadRequest, "missing accountId", nil)
		return
	}
	q := rails.TransactionListByAccountParams{}
	if v := r.URL.Query().Get("limit"); v != "" {
		n, err := strconv.ParseInt(v, 10, 64)
		if err != nil {
			writeError(w, r, http.StatusBadRequest, "invalid limit", err)
			return
		}
		q.Limit = param.NewOpt(n)
	}
	res, err := s.rail.Transactions.ListByAccount(r.Context(), accountID, q, envOpts(r)...)
	if err != nil {
		writeOutboundError(w, r, err)
		return
	}
	writeJSON(w, http.StatusOK, res)
}
