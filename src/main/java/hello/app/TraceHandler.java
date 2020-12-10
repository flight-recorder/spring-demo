package hello.app;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.stereotype.Component;
import org.springframework.web.servlet.handler.HandlerInterceptorAdapter;

import jdk.jfr.Event;
import jdk.jfr.Label;
import jdk.jfr.Name;
import jdk.jfr.StackTrace;

@Component
public class TraceHandler extends HandlerInterceptorAdapter {

    @Name("HttpRequest")
    @StackTrace(false)
    static class HttpRequestEvent extends Event {
        @Label("Request Path")
        public String requestURI;
    }

    private HttpRequestEvent event;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        event = new HttpRequestEvent();
        event.begin();
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler,
            Exception ex) {
        event.requestURI = request.getRequestURI();
        event.end();
        event.commit();
    }
}