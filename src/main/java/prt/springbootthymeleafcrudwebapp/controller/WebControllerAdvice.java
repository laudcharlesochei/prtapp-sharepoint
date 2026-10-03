package prt.springbootthymeleafcrudwebapp.controller;

import java.security.Principal;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ModelAttribute;
import prt.springbootthymeleafcrudwebapp.config.SharePointProperties;
import prt.springbootthymeleafcrudwebapp.sharepoint.SharePointException;

/** Adds the signed-in user to every page and turns SharePoint errors into a friendly page. */
@ControllerAdvice(assignableTypes = {EmployeeController.class})
public class WebControllerAdvice {

    private static final Logger log = LoggerFactory.getLogger(WebControllerAdvice.class);
    private final SharePointProperties props;

    public WebControllerAdvice(SharePointProperties props) {
        this.props = props;
    }

    @ModelAttribute
    public void addCommonAttributes(Model model, Principal principal) {
        model.addAttribute("currentUser", principal == null ? null : principal.getName());
        model.addAttribute("delegated", props.isDelegated());
        model.addAttribute("siteUrl", props.getSiteUrl());
    }

    @ExceptionHandler(SharePointException.class)
    public String handleSharePointError(SharePointException ex, Model model) {
        log.warn("SharePoint call failed: {}", ex.getMessage());
        model.addAttribute("status", ex.getStatus());
        model.addAttribute("message", ex.getMessage());
        model.addAttribute("hint", ex.getHint());
        return "sharepoint_error";
    }
}
