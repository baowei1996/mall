package bobking.icu.mall.config;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.context.annotation.Configuration;

/**
 * MyBatis配置类
 */
@Configuration
@MapperScan({"bobking.icu.mall.mbg.mapper", "bobking.icu.mall.dao"})
public class MyBatisConfig {
}
