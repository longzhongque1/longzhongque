package com.example.aicodeproductbackend;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication
@MapperScan("com.example.aicodeproductbackend.mapper")
public class AiCodeProductBackendApplication {

	public static void main(String[] args) {
		SpringApplication.run(AiCodeProductBackendApplication.class, args);
	}

}
