package com.ssginc.showpingrefactoring.domain.stream.repository;

import com.ssginc.showpingrefactoring.domain.stream.dto.response.StreamResponseDto;
import com.ssginc.showpingrefactoring.domain.stream.entity.Stream;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface VodRepository extends JpaRepository<Stream, Long> {

    /**
     * VOD 목록과 페이지 정보를 반환해주는 쿼리 메서드
     * @param pageable 페이징 정보 객체
     * @return 페이징 정보가 포함된 VOD 목록
     */
    @Query("""
        SELECT new com.ssginc.showpingrefactoring.domain.stream.dto.response.StreamResponseDto
        (s.streamNo, s.streamTitle, s.streamDescription, s.streamStatus, c.categoryNo, c.categoryName, p.productName,
        p.productPrice, p.productSale, p.productImg, s.streamStartTime, s.streamEndTime)
        FROM Stream s JOIN Product p ON s.product.productNo = p.productNo
        JOIN Category c ON p.category.categoryNo = c.categoryNo WHERE s.streamStatus = 'ENDED'
        ORDER BY s.streamNo DESC
    """)
    Page<StreamResponseDto> findAllVod(Pageable pageable);

    @Query("""
        SELECT new com.ssginc.showpingrefactoring.domain.stream.dto.response.StreamResponseDto
        (s.streamNo, s.streamTitle, s.streamDescription ,s.streamStatus, c.categoryNo, c.categoryName, p.productName,
        p.productPrice, p.productSale, p.productImg, s.streamStartTime, s.streamEndTime)
        FROM Stream s JOIN Product p ON s.product.productNo = p.productNo
        JOIN Category c ON p.category.categoryNo = c.categoryNo WHERE s.streamStatus = 'ENDED'
        AND c.categoryNo = :categoryNo ORDER BY s.streamNo DESC
    """)
    Page<StreamResponseDto> findByCategory(Long categoryNo, Pageable pageable);

    @Query("""
        SELECT new com.ssginc.showpingrefactoring.domain.stream.dto.response.StreamResponseDto
        (s.streamNo, s.streamTitle, s.streamDescription ,s.streamStatus, c.categoryNo, c.categoryName, p.productName,
        p.productPrice, p.productSale, p.productImg, s.streamStartTime, s.streamEndTime)
        FROM Stream s JOIN Product p ON s.product.productNo = p.productNo
        JOIN Watch w ON w.stream.streamNo = s.streamNo
        JOIN Category c ON p.category.categoryNo = c.categoryNo WHERE s.streamStatus = 'ENDED'
        GROUP BY w.stream.streamNo ORDER BY count(w.stream.streamNo) DESC, w.stream.streamNo DESC
    """)
    Page<StreamResponseDto> findAllOrderByViewsDesc(Pageable pageable);

    @Query("""
        SELECT new com.ssginc.showpingrefactoring.domain.stream.dto.response.StreamResponseDto
        (s.streamNo, s.streamTitle, s.streamDescription ,s.streamStatus, c.categoryNo, c.categoryName, p.productName,
        p.productPrice, p.productSale, p.productImg, s.streamStartTime, s.streamEndTime)
        FROM Stream s JOIN Product p ON s.product.productNo = p.productNo
        JOIN Watch w ON s.streamNo = w.stream.streamNo
        JOIN Category c ON p.category.categoryNo = c.categoryNo
        WHERE s.streamStatus = 'ENDED' AND c.categoryNo = :categoryNo
        GROUP BY w.stream.streamNo ORDER BY count(w.stream.streamNo) DESC, w.stream.streamNo DESC
    """)
    Page<StreamResponseDto> findByCategoryIdOrderByViewsDesc(Long categoryNo, Pageable pageable);

    /**
     * 특정 영상번호의 VOD 정보를 반환해주는 쿼리 메서드
     * @param streamNo 영상 번호
     * @return VOD 정보
     */
    @Query("""
        SELECT new com.ssginc.showpingrefactoring.domain.stream.dto.response.StreamResponseDto
        (s.streamNo, s.streamTitle, s.streamDescription, s.streamStatus, c.categoryNo, c.categoryName, p.productName,
        p.productPrice, p.productSale, p.productImg, s.streamStartTime, s.streamEndTime)
        FROM Stream s JOIN Product p ON s.product.productNo = p.productNo
        JOIN Category c ON p.category.categoryNo = c.categoryNo WHERE s.streamNo = :streamNo
    """)
    StreamResponseDto findVodByNo(Long streamNo);

    /**
     * VOD 목록 최신순 커서 기반 조회
     * @param categoryNo    카테고리 번호 (null 허용)
     * @param cursorStreamNo 마지막으로 확인한 영상 번호 (첫 페이지는 null)
     * @param limitPlusOne   한 페이지 개수 + 1 (hasNext 판단용)
     */
    @Query(value = """
        SELECT 
            s.stream_no AS streamNo, 
            s.stream_title AS streamTitle, 
            s.stream_description AS streamDescription,
            s.stream_status AS streamStatus, 
            c.category_no AS categoryNo, 
            c.category_name AS categoryName, 
            p.product_name AS productName,
            p.product_price AS productPrice, 
            p.product_sale AS productSale, 
            p.product_img AS productImg, 
            s.stream_start_time AS streamStartTime, 
            s.stream_end_time AS streamEndTime
        FROM stream s
        JOIN product p ON s.product_no = p.product_no
        JOIN category c ON p.category_no = c.category_no
        WHERE s.stream_status = 'ENDED'
          AND (:categoryNo = 0 OR c.category_no = :categoryNo)
          AND (:cursorStreamNo IS NULL OR s.stream_no < :cursorStreamNo)
        ORDER BY s.stream_no DESC
        LIMIT :limitPlusOne
    """, nativeQuery = true)
    List<VodRowProjection> getVodScroll(
            @Param("categoryNo")        Long categoryNo,
            @Param("cursorStreamNo")    Long cursorStreamNo,
            @Param("limitPlusOne")      int limitPlusOne);
}
