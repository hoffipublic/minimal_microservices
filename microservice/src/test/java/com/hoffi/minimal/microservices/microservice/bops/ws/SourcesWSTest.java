package com.hoffi.minimal.microservices.microservice.bops.ws;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import annotations.TrivialTest;
import annotations.WebTest;

@ActiveProfiles("source")
@AutoConfigureMockMvc
@SpringBootTest
public class SourcesWSTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private SourcesWS sourcesWS;

    @WebTest
    @TrivialTest
    public void contexLoads() throws Exception {
        assertTrue(sourcesWS != null);
    }

    @WebTest
    public void sourcesWS_returnsSourceRate() throws Exception {
        // String fooResourceUrl = "http://localhost:8083/sources";
        // RestTemplate restTemplate = new RestTemplate();

        // ResponseEntity<String> responseEntity = restTemplate
        // .getForEntity(String.format("%s%s", fooResourceUrl, "/sourcerate"), String.class);
        // assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
        // assertEquals("X source rate now ms: 5000", responseEntity.getBody());

        MvcResult result =
                mockMvc.perform(get("/sources/sourcerate").contentType(MediaType.TEXT_PLAIN))
                        .andExpect(status().isOk())
                        .andExpect(content().string("X  source rate now ms: 5000")).andReturn();

        String content = result.getResponse().getContentAsString();
    }

}
