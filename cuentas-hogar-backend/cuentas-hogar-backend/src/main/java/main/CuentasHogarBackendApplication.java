package main;

import com.sun.tools.javac.Main;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ApplicationContext;

@SpringBootApplication
public class CuentasHogarBackendApplication {

	public static void main(String[] args) {

		ApplicationContext context = SpringApplication.run(Main.class, args);
	}

}
