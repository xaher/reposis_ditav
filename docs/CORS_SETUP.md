# CORS Setup Guide

This document explains how CORS is configured in this project for public file access.

## Goal

Allow cross-origin requests from:

- https://test.gbv.de

Only for public resources served by:

- /servlets/MCRDerivateContentTransformerServlet/*

## Where CORS Is Configured

1. Filter logic (origin check and headers):
   - src/main/java/de/gbv/reposis/ditav/CORSFilterStarter.java
2. Filter registration and URL mapping:
   - src/main/resources/META-INF/web-fragment.xml

Both are required.

## Current Behavior

The filter allows only one origin:

- https://test.gbv.de

For matching requests it sets:

- Access-Control-Allow-Origin
- Vary: Origin
- Access-Control-Allow-Methods: GET, HEAD, OPTIONS
- Access-Control-Allow-Headers: Content-Type, Range
- Access-Control-Expose-Headers: Accept-Ranges, Content-Length, Content-Range

For OPTIONS preflight requests it returns status 204.

No Access-Control-Allow-Credentials header is set, which matches public, unauthenticated file access.

## Change Allowed Origin

Edit this constant in:

- src/main/java/de/gbv/reposis/ditav/CORSFilterStarter.java

```java
private static final String ALLOWED_ORIGIN = "https://test.gbv.de";
```

## Add More Allowed Origins

If more than one portal origin should be allowed, replace the single-string check with a set/list check.

Example:

```java
private static final Set<String> ALLOWED_ORIGINS = Set.of(
    "https://test.gbv.de",
    "https://another.example.org"
);

if (ALLOWED_ORIGINS.contains(origin)) {
    // set CORS headers
}
```

## Change URL Scope

To apply CORS to other endpoints, add additional filter mappings in:

- src/main/resources/META-INF/web-fragment.xml

Current mapping:

```xml
<filter-mapping>
    <filter-name>CORSFilterStarter</filter-name>
    <url-pattern>/servlets/MCRDerivateContentTransformerServlet/*</url-pattern>
</filter-mapping>
```

## Build and Deploy

Use the configured VS Code task:

- Build and Deploy reposis_ditav

Or run manually:

```bash
mvn clean install
```

Then copy the JAR to the mir container lib directory and restart the container.

## Verify CORS

Allowed origin preflight should return CORS headers:

```bash
curl -s -D - -o /dev/null -X OPTIONS \
  'http://localhost:8291/servlets/MCRDerivateContentTransformerServlet/test' \
  -H 'Origin: https://test.gbv.de' \
  -H 'Access-Control-Request-Method: GET'
```

Expected:

- HTTP status 204
- Access-Control-Allow-Origin: https://test.gbv.de

Disallowed origin should not get Access-Control-Allow-Origin:

```bash
curl -s -D - -o /dev/null -X OPTIONS \
  'http://localhost:8291/servlets/MCRDerivateContentTransformerServlet/test' \
  -H 'Origin: https://example.org' \
  -H 'Access-Control-Request-Method: GET'
```

## Troubleshooting

If headers are missing:

1. Confirm the filter is registered in web-fragment.xml.
2. Confirm the updated JAR is copied into /mcr/home/lib in the mir container.
3. Restart the mir container.
4. Re-run the curl checks.
5. Ensure the request path matches the mapped URL pattern.
