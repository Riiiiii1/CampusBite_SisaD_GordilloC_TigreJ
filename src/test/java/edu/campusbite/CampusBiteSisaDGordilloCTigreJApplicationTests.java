package edu.campusbite;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;

@SpringBootTest(properties = "campusbite.cocina.habilitada=false")
class CampusBiteSisaDGordilloCTigreJApplicationTests {

    @Test
    void contextLoads() {
    }

}
