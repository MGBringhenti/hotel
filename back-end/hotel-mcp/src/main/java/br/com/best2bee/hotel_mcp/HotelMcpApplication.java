package br.com.best2bee.hotel_mcp;

import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.ai.tool.method.MethodToolCallbackProvider;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
public class HotelMcpApplication {

	public static void main(String[] args) {
		SpringApplication.run(HotelMcpApplication.class, args);
	}

	@Bean
    ToolCallbackProvider hotelToolCallbacks(HotelTools tools) {
        return MethodToolCallbackProvider.builder()
                .toolObjects(tools)
                .build();
    }

}
