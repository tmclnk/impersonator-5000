package com.example.idp.web;

import lombok.Getter;
import lombok.Setter;

/**
 * Form-backing bean for the login page.
 */
@Getter
@Setter
public class LoginCommand {
    private String username;
    private String password;
    private String loginState;
    private String loginId;
    private int delaySeconds;
}
