// DOMContentLoaded 이벤트: DOM이 완전히 로드된 후 실행
document.addEventListener('DOMContentLoaded', function () {
    // --- 배너 슬라이더 기능 ---
    const bannerContainer = document.querySelector('.banner');
    const track = document.querySelector('.banner-track');
    const slides = document.querySelectorAll('.banner-slide');
    const prevBtn = document.querySelector('.banner-btn.prev');
    const nextBtn = document.querySelector('.banner-btn.next');
    let currentSlide = 0;

    // 배너 갱신 함수: 슬라이드 이동 및 active 클래스 업데이트
    function updateBanner() {
        const slideWidth = slides[0].clientWidth;
        const containerWidth = bannerContainer.clientWidth;
        // 중앙 정렬 offset: 컨테이너의 중앙에서 슬라이드 절반만큼 빼줌
        const offset = containerWidth / 2 - slideWidth / 2;
        track.style.transform = `translateX(${offset - currentSlide * slideWidth}px)`;
        // 현재 슬라이드에 active 클래스 토글
        slides.forEach((slide, index) => {
            slide.classList.toggle('active', index === currentSlide);
        });
    }

    // 다음 배너 버튼 (순환)
    nextBtn.addEventListener('click', function () {
        currentSlide = (currentSlide + 1) % slides.length;
        updateBanner();
    });

    // 이전 배너 버튼 (순환)
    prevBtn.addEventListener('click', function () {
        currentSlide = (currentSlide - 1 + slides.length) % slides.length;
        updateBanner();
    });

    // 초기 상태 및 자동 슬라이더
    updateBanner();
    setInterval(function () {
        currentSlide = (currentSlide + 1) % slides.length;
        updateBanner();
    }, 5000);

    // --- 인기 상품 필터 버튼 이벤트 등록 ---
    // #popular-option 내부의 모든 filter-button 요소 선택
    const filterPopularButtons = document.querySelectorAll('#popular-option .filter-button');
    const filterSaleButtons = document.querySelectorAll('#sale-option .filter-button');


    // 각 버튼에 클릭 이벤트 리스너 추가
    filterPopularButtons.forEach(button => {
        button.addEventListener('click', function () {
            // 모든 버튼에서 active 클래스 제거 후 클릭된 버튼에 active 클래스 추가
            filterPopularButtons.forEach(btn => btn.classList.remove('active'));
            this.classList.add('active');

            // data-category 속성 값 가져오기 (문자열이므로 숫자로 변환)
            const categoryNo = Number(this.getAttribute('data-category'));

            // 카테고리 번호에 따라 상품 정보를 불러오는 함수 호출
            getProduct(categoryNo);
        });
    });

    // 각 버튼에 클릭 이벤트 리스너 추가
    filterSaleButtons.forEach(button => {
        button.addEventListener('click', function () {
            // 모든 버튼에서 active 클래스 제거 후 클릭된 버튼에 active 클래스 추가
            filterSaleButtons.forEach(btn => btn.classList.remove('active'));
            this.classList.add('active');

            // data-sale-category 속성 값 가져오기 (문자열이므로 숫자로 변환)
            const categoryNo = Number(this.getAttribute('data-sale-category'));

            // 카테고리 번호에 따라 상품 정보를 불러오는 함수 호출
            getProductSale(categoryNo);
        });
    });

    // 초기 인기 상품 로드 (기본 카테고리 0 : ALL)
    getProduct(0);
    getProductSale(0);

    setupLiveFilterButtons();
    setupVodFilterButtons();
    getBroadCast();
    getVod();

});

// 버튼 클릭 시 호출되는 함수 (카테고리 번호 전달)
function getProduct(categoryNo) {
    loadSaleQuantity(categoryNo);
}

function getProductSale(categoryNo){
    loadSale(categoryNo);
}

// API에서 데이터를 가져와 인기 상품 영역에 렌더링하는 함수
function loadSaleQuantity(categoryNo) {
    axios.get(`/api/products/${categoryNo}/saleQuantity`)
        .then(response => {
            // API 응답의 content 배열을 가져온다고 가정합니다.
            const products = response.data;
            renderSaleQuantity(products);
        })
        .catch(error => {
            console.error("상품 목록을 불러오는 중 오류 발생:", error);
            alert("상품 정보를 불러오는 중 오류가 발생했습니다.");
        });
}

// API에서 데이터를 가져와 인기 상품 영역에 렌더링하는 함수
function loadSale(categoryNo) {
    axios.get(`/api/products/${categoryNo}/sale`)
        .then(response => {
            const products = response.data;
            renderSale(products);
        })
        .catch(error => {
            console.error("상품 목록을 불러오는 중 오류 발생:", error);
            alert("상품 정보를 불러오는 중 오류가 발생했습니다.");
        });
}

// 인기 상품 영역에 데이터를 렌더링하는 함수
function renderSaleQuantity(products) {
    // 인기 상품 영역 컨테이너 (#popular-items) 선택
    const productGrid = document.getElementById('popular-items');
    productGrid.innerHTML = ""; // 기존 내용 초기화

    products.forEach(product => {
        // 개별 상품 컨테이너 생성
        const productDiv = document.createElement('div');
        productDiv.classList.add('product-item'); // 스타일링을 위한 클래스

        // 가격 포매팅 (숫자에 천 단위 구분기호 적용)
        const formattedPrice = product.productPrice.toLocaleString('ko-KR');
        const formattedFinalPrice = product.discountedPrice.toLocaleString('ko-KR');
        const productImg = convertToWebP(product.productImg);

        // 상품명이 너무 길면 25자까지 표시 후 '...' 추가
        let productName = product.productName;
        if (productName.length > 25) {
            productName = productName.substring(0, 25) + '...';
        }

        // HTML 구성
        productDiv.innerHTML = `
          <div class="product-img-container">
              <img id="product-sale-icon" src="/img/icon/sale.png" alt="product-sale" class="sale-icon" style="width: 50px" />
              <img src="${productImg}" alt="${productName}" class="product-img" />
          </div>
          <p class="product-name">${productName}</p>
          <p class="product-sale" id="product-sale" style="text-decoration: line-through; font-size: 15px">${formattedPrice}원</p>
          <p class="product-sale-percent" id="product-sale-percent" style="color: red; font-size: 15px">${product.productSale} %</p>
          <p class="product-price-final" id="product-price-final" style="font-size: 20px">${formattedFinalPrice}원</p>
        
          <!-- 리뷰 영역 -->
          <div class="product-review">
              <div class="review-stars">
                  ${generateStars(product.reviewAverage)}
                  <p style="margin-bottom: 10px; margin-left: 5px; font-size: 15px" class="product-review-count">
                      ${product.reviewCount > 0 ? `(${product.reviewCount.toLocaleString()})` : ''}
                  </p>
              </div>
          </div>
        `;

        // 할인 정보가 없으면 할인 관련 요소 숨김
        if (product.productSale === 0) {
            productDiv.querySelector(".product-sale").style.display = "none";
            productDiv.querySelector("#product-sale-icon").style.display = "none";
            productDiv.querySelector("#product-sale-percent").style.display = "none";
        } else {
            productDiv.querySelector(".product-sale").style.display = "block";
            productDiv.querySelector("#product-sale-icon").style.display = "block";
            productDiv.querySelector("#product-sale-percent").style.display = "block";
        }

        // 리뷰 개수가 0이면 리뷰 영역 숨김
        if (product.reviewCount === 0) {
            productDiv.querySelector(".product-review").style.display = "none";
        }

        // 클릭 시 해당 상품 상세 페이지로 이동
        productDiv.addEventListener('click', () => {
            window.location.href = `/product/detail/${product.productNo}`;
        });

        productGrid.appendChild(productDiv);
    });
}

// 인기 상품 영역에 데이터를 렌더링하는 함수
function renderSale(products) {
    // 인기 상품 영역 컨테이너 (#popular-items) 선택
    const productSaleGrid = document.getElementById('sale-items');
    productSaleGrid.innerHTML = ""; // 기존 내용 초기화

    // products가 없거나 빈 배열일 경우 안내 메시지 출력
    if (!products || products.length === 0) {
        productSaleGrid.innerHTML = '<p style="height: 200px; text-align: center">현재 세일중인 상품이 없습니다</p>';
        return;
    }

    products.forEach(product => {
        // 개별 상품 컨테이너 생성
        const productDiv = document.createElement('div');
        productDiv.classList.add('product-item'); // 스타일링을 위한 클래스

        // 가격 포매팅 (숫자에 천 단위 구분기호 적용)
        const formattedPrice = product.productPrice.toLocaleString('ko-KR');
        const formattedFinalPrice = product.discountedPrice.toLocaleString('ko-KR');
        const productImg = convertToWebP(product.productImg);

        // 상품명이 너무 길면 25자까지 표시 후 '...' 추가
        let productName = product.productName;
        if (productName.length > 25) {
            productName = productName.substring(0, 25) + '...';
        }

        // HTML 구성
        productDiv.innerHTML = `
          <div class="product-img-container">
              <img id="product-sale-icon" src="/img/icon/sale.png" alt="product-sale" class="sale-icon" style="width: 50px" />
              <img src="${productImg}" alt="${productName}" class="product-img" />
          </div>
          <p class="product-name">${productName}</p>
          <p class="product-sale" id="product-sale" style="text-decoration: line-through; font-size: 15px">${formattedPrice}원</p>
          <p class="product-sale-percent" id="product-sale-percent" style="color: red; font-size: 15px">${product.productSale} %</p>
          <p class="product-price-final" id="product-price-final" style="font-size: 20px">${formattedFinalPrice}원</p>
        
          <!-- 리뷰 영역 -->
          <div class="product-review">
              <div class="review-stars">
                  ${generateStars(product.reviewAverage)}
                  <p style="margin-bottom: 10px; margin-left: 5px; font-size: 15px" class="product-review-count">
                      ${product.reviewCount > 0 ? `(${product.reviewCount.toLocaleString()})` : ''}
                  </p>
              </div>
          </div>
        `;

        // 할인 정보가 없으면 할인 관련 요소 숨김
        if (product.productSale === 0) {
            productDiv.querySelector(".product-sale").style.display = "none";
            productDiv.querySelector("#product-sale-icon").style.display = "none";
            productDiv.querySelector("#product-sale-percent").style.display = "none";
        } else {
            productDiv.querySelector(".product-sale").style.display = "block";
            productDiv.querySelector("#product-sale-icon").style.display = "block";
            productDiv.querySelector("#product-sale-percent").style.display = "block";
        }

        // 리뷰 개수가 0이면 리뷰 영역 숨김
        if (product.reviewCount === 0) {
            productDiv.querySelector(".product-review").style.display = "none";
        }

        // 클릭 시 해당 상품 상세 페이지로 이동
        productDiv.addEventListener('click', () => {
            window.location.href = `/product/detail/${product.productNo}`;
        });

        productSaleGrid.appendChild(productDiv);
    });
}

// 이미지 URL의 .jpg 확장자를 .webp로 변환하는 함수
function convertToWebP(imageUrl) {
    if (imageUrl.endsWith('.jpg')) {
        return imageUrl.replace('.jpg', '.webp');
    }
    return imageUrl;
}

// 리뷰 평점에 따른 별점을 생성하는 함수
function generateStars(reviewAverage) {
    let starsHTML = '';
    for (let i = 0; i < 5; i++) {
        if (i < Math.floor(reviewAverage)) {
            starsHTML += '<img src="/img/icon/fillStar.png" alt="filled star" style="width: 20px; height: 20px;">';
        } else if (i < Math.ceil(reviewAverage) && reviewAverage % 1 !== 0) {
            starsHTML += '<img src="/img/icon/halfStar.png" alt="half star" style="width: 20px; height: 20px;">';
        } else {
            starsHTML += '<img src="/img/icon/emptyStar.png" alt="empty star" style="width: 20px; height: 20px;">';
        }
    }
    return starsHTML;
}

function setupLiveFilterButtons() {
    const buttons = document.querySelectorAll('.live-filter-button');
    buttons.forEach(button => {
        button.addEventListener('click', function () {
            // 클릭된 버튼에 'selected' 클래스를 추가하고 나머지 버튼에서 제거
            buttons.forEach(btn => btn.classList.remove('selected'));
            button.classList.add('selected');

            const liveGrid = document.getElementById('live-grid');
            liveGrid.innerHTML = '';
        });
    });
    document.getElementById('all').selected = true;
}

function setupVodFilterButtons() {
    axios.get('/api/categories')
        .then((response) => {

            const filterButtons = document.getElementById('vod-buttons');
            const categories = response.data;

            const allButton = document.createElement("button");

            allButton.className = "vod-filter-button";
            allButton.id = `all`;
            allButton.textContent = 'ALL';

            allButton.addEventListener('click', function() {
                getVod();
            });
            filterButtons.appendChild(allButton);

            categories.forEach(category => {
                // 새 버튼 요소 생성
                const button = document.createElement("button");

                // 클래스와 id, 그리고 버튼 텍스트 설정
                button.className = "vod-filter-button";
                button.id = `${category.categoryNo}`;
                button.textContent = category.categoryName;

                button.addEventListener('click', function() {
                    categoryNumber = parseInt(category.categoryNo);
                    getVodByCategory(categoryNumber);
                });
                filterButtons.appendChild(button);
            });

            const buttons = document.querySelectorAll('.vod-filter-button');
            buttons.forEach(button => {
                button.addEventListener('click', function () {
                    // 클릭된 버튼에 'selected' 클래스를 추가하고 나머지 버튼에서 제거
                    buttons.forEach(btn => btn.classList.remove('selected'));
                    button.classList.add('selected');

                    const vodGrid = document.getElementById('vod-grid');
                    vodGrid.innerHTML = '';
                });
            })
        });
}

function getBroadCast() {
    axios.get('/api/live/active', {
        params: {
            pageNo: 0
        }
    }).then(response => {
        const pageInfo = response.data['pageInfo'];
        const broadCastContent = pageInfo['content'];
        const liveGrid = document.getElementById('live-grid');
        liveGrid.innerHTML = '';

        broadCastContent.forEach(broadCast => {
            const broadCastDiv = document.createElement('div');
            broadCastDiv.classList.add('live-item');
            const productPrice = broadCast.productPrice;
            const discountRate = broadCast.productSale;
            const finalPrice = Math.floor(productPrice * ((100 - discountRate) / 100));

            const formattedPrice = finalPrice.toLocaleString('ko-KR');

            broadCastDiv.innerHTML = `
                    <div class="stream-img-container">
                        <img id="live-icon" src="/img/icon/live.png" alt="product-live" class="live-icon" style="width: 50px; height: 50px;" />
                        <img id="standby-icon" src="/img/icon/standby.png" alt="product-standby" class="standby-icon" style="width: 70px; height: 30px;" />
                        <img src="${broadCast.productImg}" alt="방송 상품 1" />
                    </div>
                    <p id="title" style="font-size: 20px; font-weight: bold; margin: 8px 0 4px;">${broadCast.streamTitle}</p>
                            <p id="description" style="font-size: 15px; color: #555; margin: 0 0 8px;">${broadCast.streamDescription}</p>
                            <div class="product-price" style="display: flex; flex-direction: row; gap: 2px;
                            font-size: 18px; color: #000; margin: 4px 0;">
                                <span class="product-sale-percent" id="product-sale-percent" style="color: red; 
                                font-weight: bold; margin-top: 1px; margin-right: 3px; font-size: 15px">${broadCast.productSale} %</span>
                                ${formattedPrice}원
                            </div>
                    `;

            if (discountRate === 0) {
                broadCastDiv.querySelector("#product-sale-percent").style.display = "none";
            } else {
                broadCastDiv.querySelector("#product-sale-percent").style.display = "block";
            }

            // ONAIR 중인 방송에 특수 아이콘 넣기
            if (broadCast.streamStatus === 'ONAIR') {
                broadCastDiv.querySelector("#live-icon").style.display = "block";
            } else {
                broadCastDiv.querySelector("#live-icon").style.display = "none";
            }

            // ONAIR 중인 방송에 특수 아이콘 넣기
            if (broadCast.streamStatus === 'STANDBY') {
                broadCastDiv.querySelector("#standby-icon").style.display = "block";
            } else {
                broadCastDiv.querySelector("#standby-icon").style.display = "none";
            }

            // VOD 클릭 시 상세 및 시청 페이지로 이동
            broadCastDiv.addEventListener('click', () => {
                if (broadCast.streamStatus === 'ONAIR') {

                    window.location.href = window.location.href = `/stream/watch/${broadCast.streamNo}`;
                }
                else {
                    Swal.fire({
                        icon: 'error',
                        title: '시청 불가',
                        text: '준비중인 라이브입니다'
                    });
                }
            });
            liveGrid.appendChild(broadCastDiv);
        });
    })
}

function getLive() {
    axios.get('/api/live/onair')
        .then(response => {
            const live = response.data['onair'];
            const liveGrid = document.getElementById('live-grid');
            liveGrid.innerHTML = '';

            if (!live) {
                liveGrid.innerHTML = '<p>진행중인 라이브가 없습니다.</p>';
            } else {
                const liveDiv = document.createElement('div');
                liveDiv.classList.add('live-item');
                const productPrice = live.productPrice;
                const discountRate = live.productSale;
                const streamStatus = live.streamStatus;

                const discountedPrice = Math.floor(productPrice * ((100 - discountRate) / 100));
                const formattedFinalPrice = discountedPrice.toLocaleString('ko-KR');

                liveDiv.innerHTML = `
                            <div class="stream-img-container">
                                <img id="live-icon" src="/img/icon/live.png" alt="product-live" class="live-icon" style="width: 50px; height: 50px;" />
                                <img src="${live.productImg}" alt="${live.productName}" />
                            </div>
                            <p id="title" style="font-size: 15px; font-weight: bold; margin: 8px 0 4px;">${live.streamTitle}</p>
                            <p id="description" style="font-size: 13px; color: #555; margin: 0 0 8px;">${live.streamDescription}</p>
                            <div class="product-price" style="display: flex; flex-direction: row; gap: 2px;
                            font-size: 15px; color: #000; margin: 4px 0;">
                                <span class="product-sale-percent" id="product-sale-percent" style="color: red; 
                                font-weight: bold; margin-top: 1px; margin-right: 3px; font-size: 12px">${live.productSale} %</span>
                                ${formattedFinalPrice}원
                            </div>
                `;

                if (discountRate === 0) {
                    liveDiv.querySelector("#product-sale-percent").style.display = "none";
                } else {
                    liveDiv.querySelector("#product-sale-percent").style.display = "block";
                }

                if (streamStatus === 'ONAIR') {
                    liveDiv.querySelector("#live-icon").style.display = "block";
                } else {
                    liveDiv.querySelector("#live-icon").style.display = "none";
                }

                // 라이브 클릭 시 시청 및 상세 페이지로 이동
                liveDiv.addEventListener('click', () => {
                    window.location.href = `/stream/watch/${live.streamNo}`;
                });
                liveGrid.appendChild(liveDiv);
            }
        })
        .catch(error => {
            console.error("라이브 목록을 불러오는 중 오류 발생: ", error);
        });
}

function getStandBy() {
    axios.get('/api/live/standby', {
        params: {
            pageNo: 0,
        }
    })
        .then(response => {
            const pageInfo = response.data['pageInfo'];
            const standByContent = pageInfo['content'];
            const liveGrid = document.getElementById('live-grid');

            standByContent.forEach(standBy => {
                const standByDiv = document.createElement('div');
                standByDiv.classList.add('live-item');
                const productPrice = standBy.productPrice;
                const discountRate = standBy.productSale;
                const discountedPrice = Math.floor(productPrice * ((100 - discountRate) / 100));

                const formattedFinalPrice = discountedPrice.toLocaleString('ko-KR');

                standByDiv.innerHTML = `
                            <div class="stream-img-container">
                                <img id="standby-icon" src="/img/icon/standby.png" alt="product-standby" class="standby-icon" style="width: 70px; height: 30px;" />
                                <img src="${standBy.productImg}" alt="${standBy.productName}" />
                            </div>
                            <p id="title" style="font-size: 15px; font-weight: bold; margin: 8px 0 4px;">${standBy.streamTitle}</p>
                            <p id="description" style="font-size: 13px; color: #555; margin: 0 0 8px;">${standBy.streamDescription}</p>
                            <div class="product-price" style="display: flex; flex-direction: row; gap: 2px;
                            font-size: 15px; color: #000; margin: 4px 0;">
                                <span class="product-sale-percent" id="product-sale-percent" style="color: red; 
                                font-weight: bold; margin-top: 1px; margin-right: 3px; font-size: 12px">${standBy.productSale} %</span>
                                ${formattedFinalPrice}원
                            </div>
                        `;

                if (discountRate === 0) {
                    standByDiv.querySelector("#product-sale-percent").style.display = "none";
                } else {
                    standByDiv.querySelector("#product-sale-percent").style.display = "block";
                }

                // ONAIR 중인 방송에 특수 아이콘 넣기
                if (standBy.streamStatus === 'STANDBY') {
                    standByDiv.querySelector("#standby-icon").style.display = "block";
                } else {
                    standByDiv.querySelector("#standby-icon").style.display = "none";
                }

                // VOD 클릭 시 상세 및 시청 페이지로 이동
                standByDiv.addEventListener('click', () => {
                    Swal.fire({
                        icon: 'error',
                        title: '시청 불가',
                        text: '준비중인 라이브입니다'
                    });
                });

                liveGrid.appendChild(standByDiv);
            });

        })
        .catch(error => {
            console.error("준비중인 라이브 목록을 불러오는 중 오류 발생: ", error);
        })
}

function getVod() {
    axios.get('/api/vod/list', {
        params: {
            pageNo: 0,
            categoryNo: 0,
            sort: 'mostViewed'
        }
    }).then(response => {
        const data = response.data;
        const vodContent = data['content'];
        const vodGrid = document.getElementById('vod-grid');

        vodGrid.innerHTML = '';

        vodContent.forEach(vod => {

            const vodDiv = document.createElement('div');
            vodDiv.classList.add('vod-item');
            const productPrice = vod.productPrice;
            const discountRate = vod.productSale;
            const streamStartTime = vod.streamStartTime;

            const discountedPrice = Math.floor(productPrice * ((100 - discountRate) / 100));

            const formattedFinalPrice = discountedPrice.toLocaleString('ko-KR');
            const date = new Date(streamStartTime);

            // 년, 월, 일을 추출하여 포맷
            const formattedDate = `${date.getFullYear()}년 ${date.getMonth() + 1}월 ${date.getDate()}일`;

            vodDiv.innerHTML = `
                            <div class="stream-img-container">
                                <img src="${vod.productImg}" alt="${vod.productName}" />
                            </div>
                            <p id="title" style="font-size: 15px; font-weight: bold; margin: 8px 0 4px;">${vod.streamTitle}</p>
                            <p id="description" style="font-size: 13px; color: #555; margin: 0 0 8px;">${vod.streamDescription}</p>
                            <div class="product-price" style="display: flex; flex-direction: row; gap: 2px;
                            font-size: 15px; color: #000; margin: 4px 0;">
                                <span class="product-sale-percent" id="product-sale-percent" style="color: red; 
                                font-weight: bold; margin-top: 1px; margin-right: 3px; font-size: 12px">${vod.productSale} %</span>
                                ${formattedFinalPrice}원
                            </div>
                            <p id="date">${formattedDate}</p>
                `;

            if (discountRate === 0) {
                vodDiv.querySelector("#product-sale-percent").style.display = "none";
            } else {
                vodDiv.querySelector("#product-sale-percent").style.display = "block";
            }

            // VOD 클릭 시 상세 및 시청 페이지로 이동
            vodDiv.addEventListener('click', () => {
                window.location.href = `/watch/vod/${vod.streamNo}`;
            });

            vodGrid.appendChild(vodDiv);
        });
    })
        .catch(error => {
            console.error("VOD 목록을 불러오는 중 오류 발생:", error);
        });
}

function getVodByCategory(categoryNumber) {
    axios.get('/api/vod/list', {
        params: {
            pageNo: 0,
            categoryNo: categoryNumber,
            sort: 'mostViewed'
        }
    }).then(response => {
        const data = response.data;
        const vodContent = data['content'];
        const vodGrid = document.getElementById('vod-grid');

        if (vodContent == 0){
            vodGrid.innerHTML = '<p style="height: 200px; text-align: center">해당 카테고리의 VOD가 없습니다</p>';
            return;
        }

        vodGrid.innerHTML = '';

        vodContent.forEach(vod => {
            const vodDiv = document.createElement('div');
            vodDiv.classList.add('vod-item');
            const productPrice = vod.productPrice;
            const discountRate = vod.productSale;
            const streamStartTime = vod.streamStartTime;

            const discountedPrice = Math.floor(productPrice * ((100 - discountRate) / 100));

            const formattedFinalPrice = discountedPrice.toLocaleString('ko-KR');
            const date = new Date(streamStartTime);

            // 년, 월, 일을 추출하여 포맷
            const formattedDate = `${date.getFullYear()}년 ${date.getMonth() + 1}월 ${date.getDate()}일`;

            vodDiv.innerHTML = `
                            <div class="stream-img-container">
                                <img src="${vod.productImg}" alt="${vod.productName}" />
                            </div>
                            <p id="title" style="font-size: 15px; font-weight: bold; margin: 8px 0 4px;">${vod.streamTitle}</p>
                            <p id="description" style="font-size: 13px; color: #555; margin: 0 0 8px;">${vod.streamDescription}</p>
                            <div class="product-price" style="display: flex; flex-direction: row; gap: 2px;
                            font-size: 15px; color: #000; margin: 4px 0;">
                                <span class="product-sale-percent" id="product-sale-percent" style="color: red; 
                                font-weight: bold; margin-top: 1px; margin-right: 3px; font-size: 12px">${vod.productSale} %</span>
                                ${formattedFinalPrice}원
                            </div>
                            <p id="date">${formattedDate}</p>
                `;

            if (discountRate === 0) {
                vodDiv.querySelector("#product-sale-percent").style.display = "none";
            } else {
                vodDiv.querySelector("#product-sale-percent").style.display = "block";
            }

            // VOD 클릭 시 상세 및 시청 페이지로 이동
            vodDiv.addEventListener('click', () => {
                window.location.href = `/watch/vod/${vod.streamNo}`;
            });

            vodGrid.appendChild(vodDiv);
        });
    })
        .catch(error => {
            console.error("VOD 목록을 불러오는 중 오류 발생:", error);
        });
}

function goToLink(element) {
    window.location.href = element.getAttribute("data-url");
}