package com.quickcheck.engine;

import burp.api.montoya.http.message.requests.HttpRequest;

import java.util.Arrays;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class KeyNormalizer {

    private static final Pattern UUID_PATTERN = Pattern.compile(
        "(?<=/|^)[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}(?=/|$)");
    private static final Pattern MONGO_ID_PATTERN = Pattern.compile(
        "(?<=/|^)[0-9a-fA-F]{24}(?=/|$)");
    private static final Pattern LONG_TOKEN_PATTERN = Pattern.compile(
        "(?<=/|^)[A-Za-z0-9+/=_\\-]{20,}(?=/|$)");
    private static final Pattern NUMERIC_PATTERN = Pattern.compile(
        "(?<=/|^)\\d{1,18}(?=/|$)");

    public String normalize(HttpRequest request) {
        String host   = request.httpService().host();
        String method = request.method().toUpperCase();

        // Extract path+query from URL (request.path() may omit query string)
        String url = request.url();
        int schemeEnd = url.indexOf("://");
        int pathStart = schemeEnd >= 0 ? url.indexOf('/', schemeEnd + 3) : 0;
        String pathAndQuery = pathStart >= 0 ? url.substring(pathStart) : "/";

        int q = pathAndQuery.indexOf('?');
        String pathOnly = q >= 0 ? pathAndQuery.substring(0, q) : pathAndQuery;
        String rawQuery = q >= 0 ? pathAndQuery.substring(q + 1) : "";

        String normalizedPath  = normalizePath(pathOnly);
        String normalizedQuery = normalizeQuery(rawQuery);

        return method + " " + host + normalizedPath
            + (normalizedQuery.isEmpty() ? "" : "?" + normalizedQuery);
    }

    public String normalizeForDisplay(HttpRequest request) {
        String url = request.url();
        int schemeEnd = url.indexOf("://");
        int pathStart = schemeEnd >= 0 ? url.indexOf('/', schemeEnd + 3) : 0;
        String pathAndQuery = pathStart >= 0 ? url.substring(pathStart) : "/";

        int q = pathAndQuery.indexOf('?');
        String pathOnly = q >= 0 ? pathAndQuery.substring(0, q) : pathAndQuery;
        String rawQuery = q >= 0 ? pathAndQuery.substring(q + 1) : "";

        String np = normalizePath(pathOnly);
        String nq = normalizeQuery(rawQuery);
        return np + (nq.isEmpty() ? "" : "?" + nq);
    }

    public String normalizePath(String path) {
        path = UUID_PATTERN.matcher(path).replaceAll("{id}");
        path = MONGO_ID_PATTERN.matcher(path).replaceAll("{id}");
        path = LONG_TOKEN_PATTERN.matcher(path).replaceAll("{id}");
        path = NUMERIC_PATTERN.matcher(path).replaceAll("{id}");
        if (path.length() > 1 && path.endsWith("/"))
            path = path.substring(0, path.length() - 1);
        return path;
    }

    public String normalizeQuery(String query) {
        if (query == null || query.isBlank()) return "";
        return Arrays.stream(query.split("&"))
            .map(p -> {
                int e = p.indexOf('=');
                return e > 0 ? p.substring(0, e) + "={" + p.substring(0, e) + "}" : p;
            })
            .collect(Collectors.joining("&"));
    }
}
