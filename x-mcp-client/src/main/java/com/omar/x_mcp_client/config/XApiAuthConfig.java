package com.omar.x_mcp_client.config;

import io.modelcontextprotocol.client.transport.HttpClientStreamableHttpTransport;
import io.modelcontextprotocol.client.transport.customizer.McpSyncHttpClientRequestCustomizer;
import org.springframework.ai.mcp.customizer.McpClientCustomizer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;

/**
 * Adds a Bearer token to requests made on the {@code x-api} MCP connection only,
 * leaving the no-auth {@code x-docs} connection untouched.
 *
 * <p>Matching is done on the Spring AI connection <em>name</em> (as declared under
 * {@code spring.ai.mcp.client.streamable-http.connections} in {@code application.yaml}),
 * not on the request URI &mdash; the {@code name} is what {@link McpClientCustomizer} hands
 * us, and it's a more reliable signal than trying to inspect the outgoing request.
 *
 * <h2>Known caveat &mdash; read before relying on this beyond a local demo</h2>
 * X documents {@code https://api.x.com/mcp} as requiring full OAuth 2.0 + PKCE,
 * brokered through X's own {@code xurl} CLI bridge &mdash; not a static bearer header
 * presented directly to {@code api.x.com}. This class takes the simpler "just add a
 * header" approach anyway, since it mirrors how an app-only bearer token is normally
 * used against the plain X REST API and keeps this demo to one Spring config class
 * instead of an external bridge process. If {@code api.x.com/mcp} responds with 401,
 * that's the OAuth requirement &mdash; the fix is to run {@code xurl} locally and point the
 * {@code x-api} connection's URL at the bridge's loopback endpoint instead of
 * {@code api.x.com} directly. See the README's "Demo 2 auth" section for details.
 *
 * <p>The token is read only from the {@code X_BEARER_TOKEN} environment variable &mdash;
 * never hardcode a real token in {@code application.yaml} or commit it to source
 * control.
 */
@Configuration
public class XApiAuthConfig {

    @Bean
    McpClientCustomizer<HttpClientStreamableHttpTransport.Builder> xApiBearerAuth(
            @Value("${X_BEARER_TOKEN:}") String token) {

        McpSyncHttpClientRequestCustomizer addBearer =
                (request, method, uri, endpoint, context) -> {
                    if (StringUtils.hasText(token)
                            && "api.x.com".equals(uri.getHost())) {
                        request.header(
                                "Authorization",
                                "Bearer " + token
                        );
                    }
                };

        return (name, builder) ->
                builder.httpRequestCustomizer(addBearer);
    }
}
