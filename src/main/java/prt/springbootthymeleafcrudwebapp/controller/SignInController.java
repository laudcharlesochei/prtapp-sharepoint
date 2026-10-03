package prt.springbootthymeleafcrudwebapp.controller;

import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.security.web.WebAttributes;
import prt.springbootthymeleafcrudwebapp.config.SharePointProperties;

/** Sign-in page shown before redirecting to Microsoft (delegated mode). */
@Controller
public class SignInController {

    private final SharePointProperties props;

    public SignInController(SharePointProperties props) {
        this.props = props;
    }

    @GetMapping("/signin")
    public String signIn(@RequestParam(value = "error", required = false) String error,
                         @RequestParam(value = "logout", required = false) String logout,
                         HttpSession session, Model model) {
        if (!props.isDelegated()) {
            return "redirect:/index";
        }
        if (error != null) {
            Object ex = session.getAttribute(WebAttributes.AUTHENTICATION_EXCEPTION);
            model.addAttribute("errorMessage", ex instanceof Exception
                    ? ((Exception) ex).getMessage() : "Sign-in failed.");
        }
        model.addAttribute("loggedOut", logout != null);
        return "signin";
    }
}
