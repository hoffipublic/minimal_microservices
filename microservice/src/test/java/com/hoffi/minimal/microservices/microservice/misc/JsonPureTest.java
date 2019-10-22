package com.hoffi.minimal.microservices.microservice.misc;

import static org.junit.jupiter.api.Assertions.assertEquals;
import java.io.IOException;
import com.hoffi.minimal.microservices.microservice.bops.outbound.TH;
import com.hoffi.minimal.microservices.microservice.common.dto.MessageDTO;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.json.AutoConfigureJsonTesters;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.json.JacksonTester;
import org.springframework.boot.test.json.JsonContent;

// @AutoConfigureJson
@AutoConfigureJsonTesters
@SpringBootTest(classes = com.hoffi.minimal.microservices.microservice.bootconfigs.DummyConfig.class, webEnvironment = WebEnvironment.NONE)
public class JsonPureTest {
    @Autowired
    private JacksonTester<MessageDTO> json;

    @Test
    public void messageDTOtoJson() throws IOException {
        MessageDTO referenceMessageDTO = TH.referenceMessageDTO();
        referenceMessageDTO.seq = "1";
        referenceMessageDTO.message = TH.REF_MESSAGE_TIMER;

        // every receive for test below increases MessageDTO seq and BOP.bpId

        // Option 1: receive message tests with MessageQueueMatcher
        JsonContent<MessageDTO> referenceJsonDTO = this.json.write(referenceMessageDTO);
        String jsonString = referenceJsonDTO.getJson();
        System.out.println(jsonString);
        assertEquals("{\n  \"seq\" : \"1\",\n  \"message\" : \"fromSource\",\n  \"modifications\" : \"\"\n}", jsonString);
    }
}
