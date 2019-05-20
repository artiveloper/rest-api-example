package dev.artiveloper.restapiexample.config;

import dev.artiveloper.restapiexample.accounts.Account;
import dev.artiveloper.restapiexample.accounts.AccountRole;
import dev.artiveloper.restapiexample.accounts.AccountService;
import dev.artiveloper.restapiexample.common.BaseControllerTest;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Arrays;
import java.util.HashSet;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public class AuthServerConfigurationTest extends BaseControllerTest {

    @Autowired
    AccountService accountService;

    @Test
    public void 인증토큰_발급_테스트() throws Exception {
        String api = "/oauth/token";
        String clientId = "myApp";
        String clientSecret = "pass";
        String username = "artiveloper@gmail.com";
        String password = "password";

        Account account = Account.builder()
                .email(username)
                .password(password)
                .roles(new HashSet<>(Arrays.asList(AccountRole.ADMIN, AccountRole.USER)))
                .build();

        this.accountService.save(account);

        this.mockMvc.perform(post(api)
                    .with(httpBasic(clientId, clientSecret))
                .param("username", username)
                .param("password", password)
                .param("grant_type", "password"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("access_token").exists());
    }

}