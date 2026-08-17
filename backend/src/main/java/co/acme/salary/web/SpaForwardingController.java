package co.acme.salary.web;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * Hands deep links back to the Angular router.
 *
 * <p>The SPA owns paths like {@code /employees/4211}, but a browser asked to open that URL
 * directly requests it from the server, which has no such resource. Forwarding to the bundle's
 * entry point lets the client-side router take it from there. Only routes the SPA actually owns
 * are listed, so a genuinely wrong URL still 404s instead of silently rendering the app.
 */
@Controller
public class SpaForwardingController {

    @GetMapping({"/employees", "/employees/**", "/insights", "/insights/**"})
    public String forwardToSpa() {
        return "forward:/index.html";
    }
}
