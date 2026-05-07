package sample;

import com.fasterxml.jackson.databind.json.JsonMapper;
import com.railsinfra.client.RailsClient;
import com.railsinfra.client.okhttp.RailsOkHttpClient;
import com.railsinfra.core.ObjectMappers;
import com.railsinfra.errors.RailsServiceException;
import com.railsinfra.models.accounts.AccountCloseParams;
import com.railsinfra.models.accounts.AccountRetrieveParams;
import com.railsinfra.models.accounts.AccountUpdateStatusParams;
import com.railsinfra.models.transactions.TransactionListByAccountParams;
import com.railsinfra.models.transactions.TransactionRetrieveParams;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.URLDecoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.Executors;
import java.util.regex.Pattern;
import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

/**
 * Local demo server: Swagger at {@code /}, forwards some routes with {@link HttpClient}, others via
 * {@link RailsOkHttpClient}. Trust-all TLS when {@code RAILS_INSECURE_SSL=true} or
 * {@code -Drails.insecure.ssl=true} (use {@code ./gradlew run -PrailsInsecureSsl=true} if the Gradle
 * daemon ignores your shell env). Dev/staging only.
 */
public final class Main {

    private static final JsonMapper JSON = ObjectMappers.jsonMapper();

    private Main() {}

    private record StatusBody(String status) {}

    public static void main(String[] args) throws Exception {
        String baseUrl = normalizeBaseUrl(Objects.requireNonNullElse(System.getenv("RAILS_BASE_URL"), ""));
        String apiKey = Objects.requireNonNullElse(System.getenv("RAILS_API_KEY"), "");
        int port = Optional.ofNullable(System.getenv("PORT")).map(Main::parsePort).orElse(8081);

        RailsClient client = buildRailsClient(baseUrl, apiKey);

        HttpServer server = HttpServer.create(new InetSocketAddress(port), 0);
        server.setExecutor(Executors.newVirtualThreadPerTaskExecutor());
        server.createContext(
                "/",
                ex -> {
                    try {
                        handle(ex, baseUrl, apiKey, client);
                    } catch (RailsServiceException e) {
                        respondRailsServiceException(ex, e);
                    } catch (Exception e) {
                        respondThrowable(ex, e);
                    }
                });
        server.start();
        System.err.println("[rails-sdk-sample] Responding at http://0.0.0.0:" + port);
    }

    private static void handle(HttpExchange ex, String baseUrl, String apiKey, RailsClient client) throws Exception {
        addCors(ex);
        String method = ex.getRequestMethod();
        if ("OPTIONS".equalsIgnoreCase(method)) {
            ex.sendResponseHeaders(204, -1);
            ex.close();
            return;
        }

        String rawPath = ex.getRequestURI().getPath();
        String path = stripTrailingSlash(URLDecoder.decode(rawPath, StandardCharsets.UTF_8));

        switch (method) {
            case "GET" -> {
                if ("/".equals(path) || path.isEmpty()) {
                    respondText(ex, 200, readResource("/swagger-ui.html"), "text/html; charset=utf-8");
                    return;
                }
                if ("/openapi.json".equals(path)) {
                    respondText(ex, 200, readResource("/openapi.json"), "application/json; charset=utf-8");
                    return;
                }
                if ("/health".equals(path)) {
                    respondBytes(ex, 200, "{\"status\":\"ok\"}".getBytes(StandardCharsets.UTF_8), "application/json");
                    return;
                }
                if (isListAccountsPath(path)) {
                    forwardListAccounts(ex, baseUrl, apiKey);
                    return;
                }
                Optional<String> acctId = accountResourceId(path);
                if (acctId.isPresent()) {
                    String xEnv = resolveXEnvironment(ex);
                    var p =
                            AccountRetrieveParams.builder()
                                    .putAdditionalHeader("X-Environment", xEnv)
                                    .build();
                    respondJson(ex, 200, JSON.writeValueAsBytes(client.accounts().retrieve(acctId.get(), p)));
                    return;
                }
                Optional<String> txId = transactionIdOnly(path);
                if (txId.isPresent()) {
                    String xEnv = resolveXEnvironment(ex);
                    var p =
                            TransactionRetrieveParams.builder()
                                    .putAdditionalHeader("X-Environment", xEnv)
                                    .build();
                    respondJson(ex, 200, JSON.writeValueAsBytes(client.transactions().retrieve(txId.get(), p)));
                    return;
                }
                Optional<String> forTxList = accountIdForTransactions(path);
                if (forTxList.isPresent()) {
                    String xEnv = resolveXEnvironment(ex);
                    var b =
                            TransactionListByAccountParams.builder()
                                    .accountId(forTxList.get())
                                    .putAdditionalHeader("X-Environment", xEnv);
                    parseLimit(ex.getRequestURI().getQuery()).ifPresent(b::limit);
                    var list = client.transactions().listByAccount(b.build());
                    respondJson(ex, 200, JSON.writeValueAsBytes(list));
                    return;
                }
                if (isRawGet(path)) {
                    rawGet(ex, baseUrl, apiKey);
                    return;
                }
            }
            case "POST" -> {
                if (isListAccountsPath(path)) {
                    forwardCreateAccount(ex, baseUrl, apiKey);
                    return;
                }
                Optional<String> dep = accountSubPath(path, "deposit");
                if (dep.isPresent()) {
                    forwardDeposit(ex, baseUrl, apiKey, dep.get());
                    return;
                }
                Optional<String> xfer = accountSubPath(path, "transfer");
                if (xfer.isPresent()) {
                    forwardTransfer(ex, baseUrl, apiKey, xfer.get());
                    return;
                }
                Optional<String> wdr = accountSubPath(path, "withdraw");
                if (wdr.isPresent()) {
                    forwardWithdraw(ex, baseUrl, apiKey, wdr.get());
                    return;
                }
                if (isRawPost(path)) {
                    rawPost(ex, baseUrl, apiKey);
                    return;
                }
            }
            case "PATCH" -> {
                Optional<String> pid = accountResourceId(path);
                if (pid.isPresent()) {
                    byte[] body = ex.getRequestBody().readAllBytes();
                    StatusBody sb = JSON.readValue(body, StatusBody.class);
                    if (sb == null || sb.status() == null || sb.status().isBlank()) {
                        respondError(ex, 400, "missing status");
                        return;
                    }
                    String xEnv = resolveXEnvironment(ex);
                    var params =
                            AccountUpdateStatusParams.builder()
                                    .id(pid.get())
                                    .status(AccountUpdateStatusParams.Status.of(sb.status()))
                                    .putAdditionalHeader("X-Environment", xEnv)
                                    .build();
                    respondJson(ex, 200, JSON.writeValueAsBytes(client.accounts().updateStatus(params)));
                    return;
                }
            }
            case "DELETE" -> {
                Optional<String> cid = accountResourceId(path);
                if (cid.isPresent()) {
                    String xEnv = resolveXEnvironment(ex);
                    var p =
                            AccountCloseParams.builder()
                                    .putAdditionalHeader("X-Environment", xEnv)
                                    .build();
                    respondJson(ex, 200, JSON.writeValueAsBytes(client.accounts().close(cid.get(), p)));
                    return;
                }
            }
            default -> { /* fall through to 404 */ }
        }

        respondError(ex, 404, "no route for " + method + " " + path);
    }

    private static boolean isRawGet(String path) {
        return "/api/raw/get".equals(path) || "/api/v1/raw/get".equals(path);
    }

    private static boolean isRawPost(String path) {
        return "/api/raw/post".equals(path) || "/api/v1/raw/post".equals(path);
    }

    private static boolean isListAccountsPath(String path) {
        return "/api/accounts".equals(path) || "/api/v1/accounts".equals(path);
    }

    /** Matches {@code /api/v1/accounts/{id}}, optional {@code /status}, and non-v1 {@code /api/accounts/...}. */
    private static final Pattern ACCOUNT_RESOURCE =
            Pattern.compile("^/(?:api/v1/accounts|api/accounts)/([^/]+)(?:/status)?$");

    private static Optional<String> accountResourceId(String path) {
        var m = ACCOUNT_RESOURCE.matcher(path);
        return m.matches() ? Optional.of(m.group(1)) : Optional.empty();
    }

    private static Optional<String> accountSubPath(String path, String sub) {
        String v1 = "/api/v1/accounts/";
        String leg = "/api/accounts/";
        for (String pre : List.of(v1, leg)) {
            String suf = "/" + sub;
            if (path.startsWith(pre) && path.endsWith(suf)) {
                String id = path.substring(pre.length(), path.length() - suf.length());
                if (!id.contains("/")) {
                    return Optional.of(id);
                }
            }
        }
        return Optional.empty();
    }

    private static Optional<String> transactionIdOnly(String path) {
        for (String pre : List.of("/api/v1/transactions/", "/api/transactions/")) {
            if (path.startsWith(pre)) {
                String id = path.substring(pre.length());
                if (!id.isEmpty() && !id.contains("/")) {
                    return Optional.of(id);
                }
            }
        }
        return Optional.empty();
    }

    private static Optional<String> accountIdForTransactions(String path) {
        String suffix = "/transactions";
        for (String pre : List.of("/api/v1/accounts/", "/api/accounts/")) {
            if (path.startsWith(pre) && path.endsWith(suffix)) {
                String id = path.substring(pre.length(), path.length() - suffix.length());
                if (!id.contains("/")) {
                    return Optional.of(id);
                }
            }
        }
        return Optional.empty();
    }

    private static Optional<Long> parseLimit(String query) {
        if (query == null || query.isBlank()) {
            return Optional.empty();
        }
        for (String pair : query.split("&")) {
            String[] kv = pair.split("=", 2);
            if (kv.length == 2 && "limit".equals(kv[0])) {
                try {
                    return Optional.of(Long.parseLong(kv[1]));
                } catch (NumberFormatException ignored) {
                    return Optional.empty();
                }
            }
        }
        return Optional.empty();
    }

    private static void forwardCreateAccount(HttpExchange ex, String baseUrl, String apiKey) throws Exception {
        byte[] body = ex.getRequestBody().readAllBytes();
        var req =
                HttpRequest.newBuilder()
                        .uri(URI.create(trimSlash(baseUrl) + "/api/v1/accounts"))
                        .header("Content-Type", "application/json")
                        .header("X-API-Key", apiKey)
                        .header("X-Environment", "sandbox")
                        .POST(HttpRequest.BodyPublishers.ofByteArray(body))
                        .build();
        var res = proxyHttpClient().send(req, HttpResponse.BodyHandlers.ofByteArray());
        respondBytes(ex, proxiedStatus(res.statusCode()), res.body(), "application/json");
    }

    private static void forwardListAccounts(HttpExchange ex, String baseUrl, String apiKey) throws Exception {
        String q = ex.getRequestURI().getQuery();
        String url = trimSlash(baseUrl) + "/api/v1/accounts" + (q != null && !q.isBlank() ? "?" + q : "");
        var req =
                HttpRequest.newBuilder()
                        .uri(URI.create(url))
                        .header("X-API-Key", apiKey)
                        .header("X-Environment", "sandbox")
                        .GET()
                        .build();
        var res = proxyHttpClient().send(req, HttpResponse.BodyHandlers.ofByteArray());
        respondBytes(ex, proxiedStatus(res.statusCode()), res.body(), "application/json");
    }

    private static void forwardDeposit(HttpExchange ex, String baseUrl, String apiKey, String id) throws Exception {
        forwardIdempotentPost(ex, baseUrl, apiKey, id, "deposit", "dep");
    }

    private static void forwardTransfer(HttpExchange ex, String baseUrl, String apiKey, String id) throws Exception {
        forwardIdempotentPost(ex, baseUrl, apiKey, id, "transfer", "xfr");
    }

    private static void forwardWithdraw(HttpExchange ex, String baseUrl, String apiKey, String id) throws Exception {
        forwardIdempotentPost(ex, baseUrl, apiKey, id, "withdraw", "wdr");
    }

    private static void forwardIdempotentPost(
            HttpExchange ex, String baseUrl, String apiKey, String id, String action, String idemPrefix)
            throws Exception {
        byte[] body = ex.getRequestBody().readAllBytes();
        if (body.length == 0) {
            respondError(ex, 400, "missing body");
            return;
        }
        String idem = firstNonBlank(header(ex, "Idempotency-Key"), genIdempotencyKey(idemPrefix));
        var b =
                HttpRequest.newBuilder()
                        .uri(URI.create(trimSlash(baseUrl) + "/api/v1/accounts/" + id + "/" + action))
                        .header("Content-Type", "application/json")
                        .header("X-API-Key", apiKey)
                        .header("Idempotency-Key", idem)
                        .header("X-Environment", "sandbox")
                        .POST(HttpRequest.BodyPublishers.ofByteArray(body));
        var res = proxyHttpClient().send(b.build(), HttpResponse.BodyHandlers.ofByteArray());
        respondBytes(ex, proxiedStatus(res.statusCode()), res.body(), "application/json");
    }

    private static void rawGet(HttpExchange ex, String baseUrl, String apiKey) throws Exception {
        String pathParam = queryParam(ex.getRequestURI().getQuery(), "path");
        String path = pathParam != null ? pathParam : "api/v1/accounts";
        var req =
                HttpRequest.newBuilder()
                        .uri(URI.create(trimSlash(baseUrl) + "/" + path.replaceFirst("^/+", "")))
                        .header("X-API-Key", apiKey)
                        .GET()
                        .build();
        var res = proxyHttpClient().send(req, HttpResponse.BodyHandlers.ofByteArray());
        respondBytes(ex, proxiedStatus(res.statusCode()), res.body(), "application/json");
    }

    private static void rawPost(HttpExchange ex, String baseUrl, String apiKey) throws Exception {
        String pathParam = queryParam(ex.getRequestURI().getQuery(), "path");
        String path = pathParam != null ? pathParam : "api/v1/accounts";
        byte[] body = ex.getRequestBody().readAllBytes();
        var req =
                HttpRequest.newBuilder()
                        .uri(URI.create(trimSlash(baseUrl) + "/" + path.replaceFirst("^/+", "")))
                        .header("Content-Type", "application/json")
                        .header("X-API-Key", apiKey)
                        .POST(HttpRequest.BodyPublishers.ofByteArray(body))
                        .build();
        var res = proxyHttpClient().send(req, HttpResponse.BodyHandlers.ofByteArray());
        respondBytes(ex, proxiedStatus(res.statusCode()), res.body(), "application/json");
    }

    private static String queryParam(String query, String key) {
        if (query == null) {
            return null;
        }
        for (String pair : query.split("&")) {
            String[] kv = pair.split("=", 2);
            if (kv.length == 2 && key.equals(kv[0])) {
                return URLDecoder.decode(kv[1], StandardCharsets.UTF_8);
            }
        }
        return null;
    }

    private static String genIdempotencyKey(String prefix) {
        return prefix + "-" + System.currentTimeMillis() + "-" + java.util.UUID.randomUUID().toString().replace("-", "");
    }

    private static int proxiedStatus(int code) {
        return switch (code) {
            case 200, 201, 204, 400, 401, 403, 404, 409, 422, 500, 502, 503 -> code;
            default -> code >= 100 && code <= 599 ? code : 500;
        };
    }

    private static void respondRailsServiceException(HttpExchange ex, RailsServiceException e) {
        try {
            addCors(ex);
            String path = stripTrailingSlash(URLDecoder.decode(ex.getRequestURI().getPath(), StandardCharsets.UTF_8));
            ErrorResponse err =
                    new ErrorResponse(e.statusCode(), e.getMessage(), e.getClass().getName(), path);
            byte[] json = JSON.writeValueAsBytes(err);
            respondBytes(ex, e.statusCode(), json, "application/json");
        } catch (IOException ioe) {
            ioe.printStackTrace();
            ex.close();
        }
    }

    private static void respondThrowable(HttpExchange ex, Throwable e) {
        try {
            addCors(ex);
            e.printStackTrace();
            String path = stripTrailingSlash(URLDecoder.decode(ex.getRequestURI().getPath(), StandardCharsets.UTF_8));
            ErrorResponse err =
                    new ErrorResponse(500, e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName(),
                            e.getClass().getName(), path);
            byte[] json = JSON.writeValueAsBytes(err);
            respondBytes(ex, 500, json, "application/json");
        } catch (IOException ioe) {
            ioe.printStackTrace();
            ex.close();
        }
    }

    private static void respondError(HttpExchange ex, int status, String message) throws IOException {
        addCors(ex);
        String path = stripTrailingSlash(URLDecoder.decode(ex.getRequestURI().getPath(), StandardCharsets.UTF_8));
        ErrorResponse err = new ErrorResponse(status, message, null, path);
        respondBytes(ex, status, JSON.writeValueAsBytes(err), "application/json");
    }

    private static void respondJson(HttpExchange ex, int status, byte[] json) throws IOException {
        addCors(ex);
        respondBytes(ex, status, json, "application/json");
    }

    private static void respondText(HttpExchange ex, int status, String body, String contentType) throws IOException {
        addCors(ex);
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        ex.getResponseHeaders().set("Content-Type", contentType);
        ex.sendResponseHeaders(status, bytes.length);
        ex.getResponseBody().write(bytes);
        ex.close();
    }

    private static void respondBytes(HttpExchange ex, int status, byte[] body, String contentType) throws IOException {
        ex.getResponseHeaders().set("Content-Type", contentType);
        if (body == null) {
            body = new byte[0];
        }
        ex.sendResponseHeaders(status, body.length);
        ex.getResponseBody().write(body);
        ex.close();
    }

    private static void addCors(HttpExchange ex) {
        var h = ex.getResponseHeaders();
        h.set("Access-Control-Allow-Origin", "*");
        h.set("Access-Control-Allow-Methods", "GET, POST, PATCH, PUT, DELETE, OPTIONS");
        h.set(
                "Access-Control-Allow-Headers",
                "Content-Type, Accept, X-Environment, Idempotency-Key, X-API-Key, x-correlation-id");
        h.set("Access-Control-Allow-Credentials", "false");
    }

    private static String header(HttpExchange ex, String name) {
        return ex.getRequestHeaders().getFirst(name);
    }

    private static String stripTrailingSlash(String path) {
        if (path.length() > 1 && path.endsWith("/")) {
            return path.substring(0, path.length() - 1);
        }
        return path.isEmpty() ? "/" : path;
    }

    /** This sample always uses the sandbox environment for Rails account API calls. */
    private static String resolveXEnvironment(HttpExchange ignored) {
        return "sandbox";
    }

    private static String trimSlash(String baseUrl) {
        String t = baseUrl.trim();
        while (t.endsWith("/")) {
            t = t.substring(0, t.length() - 1);
        }
        return t;
    }

    static String normalizeBaseUrl(String raw) {
        String t = raw.trim();
        while (t.endsWith("/")) {
            t = t.substring(0, t.length() - 1);
        }
        if (t.isEmpty()) {
            return "https://api.railsinfra.com/";
        }
        String withScheme =
                t.startsWith("http://") || t.startsWith("https://") ? t : "https://" + t;
        return withScheme + "/";
    }

    private static String firstNonBlank(String a, String b) {
        if (a != null && !a.isBlank()) {
            return a;
        }
        return b;
    }

    private static int parsePort(String s) {
        try {
            return Integer.parseInt(s.trim());
        } catch (NumberFormatException e) {
            return 8081;
        }
    }

    private static String readResource(String path) throws IOException {
        InputStream in = Main.class.getResourceAsStream(path);
        if (in == null) {
            throw new IOException("missing resource " + path);
        }
        try (in) {
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    private static boolean isInsecureTls() {
        String env = System.getenv("RAILS_INSECURE_SSL");
        if (env != null && "true".equalsIgnoreCase(env.trim())) {
            return true;
        }
        String prop = System.getProperty("rails.insecure.ssl");
        return prop != null && "true".equalsIgnoreCase(prop.trim());
    }

    private static HttpClient buildProxyHttpClient() {
        boolean insecure = isInsecureTls();
        System.err.println(
                "[rails-sdk-sample] Proxy HttpClient trust-all TLS: "
                        + (insecure ? "ON" : "OFF")
                        + " (set RAILS_INSECURE_SSL=true or run with -PrailsInsecureSsl=true if PKIX still fails)");
        if (!insecure) {
            return HttpClient.newHttpClient();
        }
        TrustManager[] trustAll =
                new TrustManager[] {
                    new X509TrustManager() {
                        @Override
                        public void checkClientTrusted(X509Certificate[] chain, String authType) {}

                        @Override
                        public void checkServerTrusted(X509Certificate[] chain, String authType) {}

                        @Override
                        public X509Certificate[] getAcceptedIssuers() {
                            return new X509Certificate[0];
                        }
                    }
                };
        try {
            SSLContext ctx = SSLContext.getInstance("TLS");
            ctx.init(null, trustAll, new SecureRandom());
            return HttpClient.newBuilder().sslContext(ctx).build();
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    private static final class ProxyHolder {
        static final HttpClient INSTANCE = buildProxyHttpClient();
    }

    private static HttpClient proxyHttpClient() {
        return ProxyHolder.INSTANCE;
    }

    private static RailsClient buildRailsClient(String baseUrl, String apiKey) {
        var b = RailsOkHttpClient.builder().baseUrl(baseUrl).apiKey(apiKey);
        if (isInsecureTls()) {
            TrustManager[] trustAll =
                    new TrustManager[] {
                        new X509TrustManager() {
                            @Override
                            public void checkClientTrusted(X509Certificate[] chain, String authType) {}

                            @Override
                            public void checkServerTrusted(X509Certificate[] chain, String authType) {}

                            @Override
                            public X509Certificate[] getAcceptedIssuers() {
                                return new X509Certificate[0];
                            }
                        }
                    };
            try {
                SSLContext ctx = SSLContext.getInstance("TLS");
                ctx.init(null, trustAll, new SecureRandom());
                X509TrustManager tm = (X509TrustManager) trustAll[0];
                HostnameVerifier allHosts = (hostname, session) -> true;
                b.sslSocketFactory(ctx.getSocketFactory());
                b.trustManager(tm);
                b.hostnameVerifier(allHosts);
            } catch (Exception e) {
                throw new IllegalStateException(e);
            }
        }
        return b.build();
    }
}
