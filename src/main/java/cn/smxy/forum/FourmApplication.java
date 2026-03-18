package cn.smxy.forum;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.transaction.annotation.EnableTransactionManagement;

@SpringBootApplication
@EnableScheduling
@EnableTransactionManagement
@MapperScan("cn.smxy.forum.mapper")
@ComponentScan("cn.smxy")
public class FourmApplication {

	public static void main(String[] args) {
		SpringApplication.run(FourmApplication.class, args);
	}

}
