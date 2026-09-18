package com.br.guirldev.financial_control_api;

import org.springframework.boot.SpringApplication;

public class TestFinancialControlApiApplication {

	public static void main(String[] args) {
		SpringApplication.from(FinancialControlApiApplication::main).with(TestcontainersConfiguration.class).run(args);
	}

}
