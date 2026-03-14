package com.kombee.orderly.config;

import com.kombee.orderly.entity.Product;
import com.kombee.orderly.entity.User;
import com.kombee.orderly.repository.ProductRepository;
import com.kombee.orderly.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
@Profile("!test")
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final ProductRepository productRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {
        if (userRepository.count() > 0) {
            return;
        }
        User user = User.builder()
                .username("demo")
                .email("demo@kombee.com")
                .passwordHash(passwordEncoder.encode("demo123"))
                .displayName("Demo User")
                .role(User.Role.USER)
                .build();
        userRepository.save(user);

        for (int i = 1; i <= 5; i++) {
            Product p = Product.builder()
                    .sku("SKU-" + i)
                    .name("Product " + i)
                    .description("Description for product " + i)
                    .price(BigDecimal.valueOf(10.99 * i))
                    .stockQuantity(100)
                    .build();
            productRepository.save(p);
        }
    }
}
