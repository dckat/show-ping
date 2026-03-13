package com.ssginc.showpingrefactoring.batch.job;

import com.ssginc.showpingrefactoring.domain.stream.dto.object.VodRecommendDto;
import lombok.RequiredArgsConstructor;
import org.springframework.batch.item.database.JdbcPagingItemReader;
import org.springframework.batch.item.database.Order;
import org.springframework.batch.item.database.PagingQueryProvider;
import org.springframework.batch.item.database.builder.JdbcPagingItemReaderBuilder;
import org.springframework.batch.item.database.support.SqlPagingQueryProviderFactoryBean;
import org.springframework.context.annotation.Bean;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.util.HashMap;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class VodRecommendReader {

    private final DataSource dataSource;

    @Bean
    public JdbcPagingItemReader<VodRecommendDto> jdbcPagingReader() throws Exception {
        return new JdbcPagingItemReaderBuilder<VodRecommendDto>()
                .name("recommendReader")
                .dataSource(dataSource)
                .pageSize(500)
                .rowMapper(new BeanPropertyRowMapper<>(VodRecommendDto.class))
                .queryProvider(createQueryProvider())
                .build();
    }

    private PagingQueryProvider createQueryProvider() throws Exception {
        SqlPagingQueryProviderFactoryBean factory = new SqlPagingQueryProviderFactoryBean();
        factory.setDataSource(dataSource);

        factory.setSelectClause("""
        FLOOR(TIMESTAMPDIFF(YEAR, m.member_birthdate, CURDATE()) / 10) * 10 AS ageGroup,
        m.member_gender AS gender,
        w.stream_no AS streamNo,
        COUNT(*) AS viewCount,
        MAX(s.stream_title) AS streamTitle,
        MAX(p.product_name) AS productName,
        MAX(p.product_price) AS productPrice,
        MAX(p.product_sale) AS productSale,
        MAX(p.product_img) AS productImg
        """);

        factory.setFromClause("""
        FROM watch w
        JOIN member m ON w.member_no = m.member_no
        JOIN stream s ON w.stream_no = s.stream_no
        JOIN product p ON s.product_no = p.product_no
        """);

        factory.setWhereClause("w.watch_time >= DATE_SUB(NOW(), INTERVAL 7 DAY)");

        factory.setGroupClause("ageGroup, gender, w.stream_no");

        Map<String, Order> sortKeys = new HashMap<>();
        sortKeys.put("ageGroup", Order.ASCENDING);
        sortKeys.put("viewCount", Order.DESCENDING);
        factory.setSortKeys(sortKeys);

        return factory.getObject();
    }

}
