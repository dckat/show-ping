package com.ssginc.showpingrefactoring.batch.job;

import com.ssginc.showpingrefactoring.batch.dto.VodRecommendDto;
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

        // SELECT 절: 연령대 계산 로직
        factory.setSelectClause("""
            FLOOR(TIMESTAMPDIFF(YEAR, m.member_birthdate, CURDATE()) / 10) * 10 AS ageGroup,
            m.member_gender AS gender,
            w.stream_no AS streamNo,
            COUNT(*) AS viewCount
        """);

        // FROM 절
        factory.setFromClause("""
            FROM watch w
            JOIN member m ON w.member_no = m.member_no
        """);

        // 필터링 기준: 최근 7일 이내
        factory.setWhereClause("w.watch_time >= DATE_SUB(NOW(), INTERVAL 7 DAYS)");

        factory.setGroupClause("ageGroup, m.member_gender, w.stream_no");

        // 정렬 설정 (PagingReader는 반드시 최소 한 개 이상의 정렬 키가 필요함)
        // 여기서는 viewCount 내림차순을 위해 Map 활용
        Map<String, Order> sortKeys = new HashMap<>();
        sortKeys.put("ageGroup", Order.ASCENDING);
        sortKeys.put("viewCount", Order.DESCENDING);
        factory.setSortKeys(sortKeys);

        return factory.getObject();
    }

}
