let liveFilterOption = 'all';
let vodFilterOption = 'all';
let categoryNumber = null;

document.addEventListener("DOMContentLoaded", function () {
    setupFilterButtons();                      // 필터링 버튼 셋팅
    loadBroadCast(0);                  // ONAIR + STANDBY 불러오기
    loadVod(null);                    // 최초 1페이지의 VOD 목록 불러오기

    document.getElementById('filter-all').selected = true;
});

// 정렬 버튼 클릭 시
function setupFilterButtons() {
    setupLiveFilterButtons();
    setupVodFilterButtons();
}

function setupLiveFilterButtons() {
    const buttons = document.querySelectorAll('.live-filter-btn');
    buttons.forEach(button => {
        button.addEventListener('click', function () {
            // 클릭된 버튼에 'selected' 클래스를 추가하고 나머지 버튼에서 제거
            buttons.forEach(btn => btn.classList.remove('selected'));
            button.classList.add('selected');

            const liveGrid = document.getElementById('live-grid');
            liveGrid.innerHTML = '';
        });
    });
}

function setupVodFilterButtons() {
    axios.get('/api/categories')
        .then((response) => {
            const filterButtons = document.getElementById('vod-buttons');
            const categories = response.data;

            const allButton = document.createElement("button");

            allButton.className = "vod-filter-btn";
            allButton.id = `filter-all`;
            allButton.textContent = '전체';

            allButton.addEventListener('click', function() {
                vodFiltered = false;
                loadVod(null);
            });
            filterButtons.appendChild(allButton);

            categories.forEach(category => {
                // 새 버튼 요소 생성
                const button = document.createElement("button");

                // 클래스와 id, 그리고 버튼 텍스트 설정
                button.className = "vod-filter-btn";
                button.id = `filter-${category.categoryNo}`;
                button.textContent = category.categoryName;

                button.addEventListener('click', function() {
                    vodFilterOption = 'filtered';
                    categoryNumber = parseInt(category.categoryNo);
                    loadVodByCategory(null, categoryNumber);
                });

                filterButtons.appendChild(button);
            });

            const buttons = document.querySelectorAll('.vod-filter-btn');
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

function filterAll() {
    liveFilterOption = 'all';
    loadBroadCast(0);
}

function filterLive() {
    liveFilterOption = 'live';
    loadLive();
}

function filterStandBy() {
    liveFilterOption = 'standby';
    loadStandBy(0);
}

function loadBroadCast(pageNo) {
    axios.get('/api/live/active', {
        params: {
            pageNo: pageNo,
        }
    }).then(response => {
        const pageInfo = response.data['pageInfo'];
        const broadCastContent = pageInfo['content'];
        const liveGrid = document.getElementById('live-grid');

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
                                <img src="${broadCast.productImg}" alt="${broadCast.productName}" />
                            </div>
                            <p id="title" style="font-size: 15px; font-weight: bold; margin: 8px 0 4px;">${broadCast.streamTitle}</p>
                            <p id="description" style="font-size: 13px; color: #555; margin: 0 0 8px;">${broadCast.streamDescription}</p>
                            <div class="product-price" style="display: flex; flex-direction: row; gap: 2px;
                            font-size: 15px; color: #000; margin: 4px 0;">
                                <span class="product-sale-percent" id="product-sale-percent" style="color: red; 
                                font-weight: bold; margin-top: 1px; margin-right: 3px; font-size: 12px">${broadCast.productSale} %</span>
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
        renderPageContent(pageInfo, 'live-page-container');

    })
        .catch(error => {
            console.error("방송 목록을 불러오는 중 오류 발생: ", error);
        });
}

function loadLive() {
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

function loadStandBy(pageNo) {
    axios.get('/api/live/standby', {
        params: {
            pageNo: pageNo,
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

            renderPageContent(pageInfo, 'live-page-container');

        })
        .catch(error => {
            console.error("준비중인 라이브 목록을 불러오는 중 오류 발생: ", error);
        })
}

function loadVod(streamNo) {
    // 현재 페이지의 VOD 목록 불러오기
    axios.get('/api/vod/list/scroll', {
        params: {
            categoryNo: 0,
            cursorStreamNo: streamNo,  // 첫 요청 시 null
            pageSize: 4
        }
    }).then(response => {
            const data = response.data;
            const vodContent = data['content'];

            const vodGrid = document.getElementById('vod-grid');

            if (!streamNo) {
                vodGrid.innerHTML = '';
            }

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

            renderPageContent(data, 'vod-page-container');
        })
        .catch(error => {
            console.error("VOD 목록을 불러오는 중 오류 발생:", error);
        });
}

function loadVodByCategory(streamNo, categoryNo) {
    axios.get(`/api/vod/list/scroll`, {
        params: {
            categoryNo: categoryNo,
            cursorStreamNo: streamNo,  // 첫 요청 시 null
            pageSize: 4
        }
    }).then(response => {
        const data = response.data;
        const vodContent = data['content'];

        const vodGrid = document.getElementById('vod-grid');

        if (!streamNo) {
            vodGrid.innerHTML = '';
        }

        vodContent.forEach(vod => {
            const vodDiv = document.createElement('div');
            vodDiv.classList.add('vod-item');
            const productPrice = vod.productPrice;
            const discountRate = vod.productSale;
            const streamStartTime = vod.streamStartTime;

            const discountedPrice = Math.floor(productPrice * ((100 - discountRate) / 100));

            const formattedOriginPrice = productPrice.toLocaleString('ko-KR');
            const formattedDiscountedPrice = discountedPrice.toLocaleString('ko-KR');

            const date = new Date(streamStartTime);

            // 년, 월, 일을 추출하여 포맷
            const formattedDate = `${date.getFullYear()}년 ${date.getMonth() + 1}월 ${date.getDate()}일`;

            vodDiv.innerHTML = `
                    <div class="stream-img-container">
                        <img src="${vod.productImg}" alt="${vod.productName}" />
                    </div>
                    <p id="date">${formattedDate}</p>
                    <p id="title">${vod.streamTitle}</p>
                    <p class="product-sale" id="product-sale" style="text-decoration: line-through; font-size: 15px">${formattedOriginPrice}원</p>
                    <p class="product-sale-percent" id="product-sale-percent" style="color: red; font-size: 15px">${vod.productSale} %</p>
                    <p class="product-price-final" id="product-price-final" style="font-size: 20px">${formattedDiscountedPrice}원</p>
                `;

            if (discountRate === 0) {
                vodDiv.querySelector(".product-sale").style.display = "none";
                vodDiv.querySelector("#product-sale-percent").style.display = "none";
            } else {
                vodDiv.querySelector(".product-sale").style.display = "block";
                vodDiv.querySelector("#product-sale-percent").style.display = "block";
            }

            // VOD 클릭 시 상세 및 시청 페이지로 이동
            vodDiv.addEventListener('click', () => {
                window.location.href = `/watch/vod/${vod.streamNo}`;
            });

            vodGrid.appendChild(vodDiv);
        });
        renderPageContent(data, 'vod-page-container');
    })
        .catch(error => {
            console.error("VOD 목록을 불러오는 중 오류 발생:", error);
        });
}

function renderPageContent(data, containerName) {
    // 페이지 버튼 영역 생성
    const pageContainer = document.getElementById(containerName);
    pageContainer.innerHTML = '';

    const hasMore = data['hasMore'];

    if (hasMore) {
        const nextCursor = data['nextCursor'];
        const cursorStreamNo = nextCursor['streamNo'];

        const pageDiv = document.createElement('div');
        pageDiv.classList.add('page-item');
        pageDiv.innerHTML = `<button class="load-more">더보기</button>`

        // 페이지 번호 클릭시 해당 페이지의 VOD 목록을 가져옴
        pageDiv.addEventListener('click', () => {
            if (containerName === 'vod-page-container') {
                if (vodFilterOption === 'all') {
                    loadVod(cursorStreamNo);
                }
                else {
                    loadVodByCategory(cursorStreamNo, categoryNumber);
                }
            }
            else if (containerName === 'live-page-container') {
                if (liveFilterOption === 'all') {
                    loadBroadCast(pageNumber + 1);
                }
                else if (liveFilterOption === 'standby') {
                    loadStandBy(pageNumber + 1);
                }
            }
        });
        pageContainer.appendChild(pageDiv);
    }
}
