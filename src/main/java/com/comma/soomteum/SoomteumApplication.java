package com.comma.soomteum;

import com.comma.soomteum.config.FeignConfig;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

@SpringBootApplication
@EnableFeignClients(defaultConfiguration = FeignConfig.class)
public class SoomteumApplication {

	public static void main(String[] args) {
		SpringApplication.run(SoomteumApplication.class, args);
	}

}
