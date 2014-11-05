package app;

import benchmark.Payload;
import benchmark.RandomWordsPayload;

import javax.servlet.ServletException;
import javax.servlet.http.*;
import java.io.IOException;

/**
 * Main HttpServlet used for tests and benchmarks.
 */
@SuppressWarnings("serial")
public class BenchmarkServlet extends HttpServlet {

    private final Payload payload = new RandomWordsPayload();

    @Override
    protected void service(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        HttpSession session = request.getSession(false);
        Cookie[] cookies = request.getCookies();
        response.setContentType("text/html");

        if (session == null) {
            session = request.getSession(true);
            if (cookies != null && cookies.length > 0) {
                response.setStatus(HttpServletResponse.SC_NOT_FOUND);
                response.getWriter().println("[FAIL]");
            } else {
                response.setStatus(HttpServletResponse.SC_OK);
                response.getWriter().println("[NEW] " + session.getId());
            }

            session.setAttribute("sessionData", payload.getPayload());
        } else {
            Object data = session.getAttribute("sessionData");
            if (data != null) {
                response.setStatus(HttpServletResponse.SC_OK);
                response.getWriter().println("[OLD] " + session.getId());
            } else {
                response.setStatus(HttpServletResponse.SC_NOT_FOUND);
                response.getWriter().println("[FAIL]");
                session.setAttribute("sessionData", payload.getPayload());
            }
        }


    }

}
