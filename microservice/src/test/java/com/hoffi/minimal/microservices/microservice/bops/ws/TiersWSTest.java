package com.hoffi.minimal.microservices.microservice.bops.ws;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@AutoConfigureMockMvc
@SpringBootTest
public class TiersWSTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    public void tiersWS_returnsFailevery() throws Exception {
        //        String fooResourceUrl = "http://localhost:9090/sources";
        //        RestTemplate restTemplate = new RestTemplate();
        //
        //        ResponseEntity<String> responseEntity = restTemplate.getForEntity(String.format("%s%s", fooResourceUrl, "/sourcerate"),
        //                String.class);
        //        assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
        //        assertEquals("X  source rate now ms: 5000", responseEntity.getBody());

        MvcResult result = mockMvc.perform(get("/tiers/failevery").contentType(MediaType.APPLICATION_JSON)).andExpect(status().isOk())
                .andExpect(content().string("X  will fail on every 5th call")).andReturn();

        String content = result.getResponse().getContentAsString();

    }

}
