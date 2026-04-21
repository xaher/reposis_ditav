package de.gbv.reposis.ditav;

import org.mycore.common.events.MCRStartupHandler;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * Requests to /servlets/MCRDerivateContentTransformerServlet/* are allowed for the Koloniale Quellen portal.
 */
public class CORSFilterStarter implements MCRStartupHandler.AutoExecutable, Filter {

    private static final String ALLOWED_ORIGIN = "https://kolonialequellen.gbv.de";


    @Override
    public String getName() {
        return "CORSFilterStarter for servlets/MCRDerivateContentTransformerServlet";
    }

    @Override
    public int getPriority() {
        return 0;
    }

    @Override
    public void startUp(ServletContext servletContext) {
        if (servletContext == null) {
            return;
        }

        FilterRegistration.Dynamic corsFilter = servletContext.addFilter("MCRDerivateContentTransformerServlet", this);
        corsFilter.addMappingForUrlPatterns(null, false, "/servlets/MCRDerivateContentTransformerServlet/*");
    }

    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain) throws IOException, ServletException {
        if (servletRequest instanceof HttpServletRequest && servletResponse instanceof HttpServletResponse) {
            HttpServletRequest req = (HttpServletRequest) servletRequest;
            HttpServletResponse resp = (HttpServletResponse) servletResponse;

            String origin = req.getHeader("Origin");
            if (ALLOWED_ORIGIN.equals(origin)) {
                resp.setHeader("Access-Control-Allow-Origin", origin);
                resp.setHeader("Vary", "Origin");
                resp.setHeader("Access-Control-Allow-Methods", "GET, HEAD, OPTIONS");
                resp.setHeader("Access-Control-Allow-Headers", "Content-Type, Range");
                resp.setHeader("Access-Control-Expose-Headers", "Accept-Ranges, Content-Length, Content-Range");
            }

            if ("OPTIONS".equalsIgnoreCase(req.getMethod())) {
                resp.setStatus(HttpServletResponse.SC_NO_CONTENT);
                return;
            }
        }
        filterChain.doFilter(servletRequest, servletResponse);
    }

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
    }

    @Override
    public void destroy() {
    }
}
