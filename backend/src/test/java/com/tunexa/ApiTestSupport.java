package com.tunexa;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public abstract class ApiTestSupport {

    protected static final String CUSTOMER_PHONE = "+21655555555";
    protected static final String SELLER_PHONE = "+21622222222";
    protected static final String ADMIN_PHONE = "+21620000000";

    @Autowired
    protected MockMvc mvc;

    @Autowired
    protected ObjectMapper json;

    protected ResultActions call(MockHttpServletRequestBuilder req, String token, Object body) throws Exception {
        if (token != null) {
            req.header("Authorization", "Bearer " + token);
        }
        if (body != null) {
            req.contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(body));
        }
        return mvc.perform(req);
    }

    protected JsonNode read(ResultActions result) throws Exception {
        return json.readTree(result.andReturn().getResponse().getContentAsString());
    }

    protected ResultActions getJson(String url, String token) throws Exception {
        return call(get(url), token, null);
    }

    protected ResultActions postJson(String url, String token, Object body) throws Exception {
        return call(post(url), token, body);
    }

    protected ResultActions putJson(String url, String token, Object body) throws Exception {
        return call(put(url), token, body);
    }

    protected String login(String phone, String password) throws Exception {
        return read(postJson("/api/auth/login", null, java.util.Map.of("phone", phone, "password", password)))
                .get("token").asText();
    }
}
