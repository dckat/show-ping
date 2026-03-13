document.addEventListener("DOMContentLoaded", async () => {
    loadCategories();
    await initAuthUI();
    await setAdminNav();
    initScrollToTop();
});

function getCookie(name) {
    return document.cookie.split('; ')
        .find(c => c.startsWith(name + '='))?.split('=')[1];
}

// 추가: XSRF-TOKEN이 실제 생길 때까지 보장
async function ensureFreshXsrf() {
    if (!getCookie('XSRF-TOKEN')) {
        await fetch('/api/csrf', { credentials: 'include' });
        // 쿠키가 실제로 보일 때까지 잠깐 대기 (최대 200ms)
        for (let i = 0; i < 10; i++) {
            if (getCookie('XSRF-TOKEN')) break;
            await new Promise(r => setTimeout(r, 20));
        }
    }
}

function isLoginPage() {
    return window.location.pathname.startsWith("/login");
}

function loadCategories() {
    axios.get('/api/categories')
        .then(response => {
            const categories = response.data;
            const dropdownMenu = document.querySelector('.dropdown-menu');
            if (!dropdownMenu) return;

            // 초기화
            dropdownMenu.innerHTML = ''; // 초기화

            const listItem = document.createElement('li' );
            const link = document.createElement('a');
            link.href = `/category/0`;
            link.textContent = `전체`;
            listItem.appendChild(link);
            dropdownMenu.appendChild(listItem);

            categories.forEach(category => {
                const listItem = document.createElement('li' );
                const link = document.createElement('a');
                link.href = `/category/${category.categoryNo}`; // 해당 category로 이동
                link.textContent = category.categoryName;
                listItem.appendChild(link);
                dropdownMenu.appendChild(listItem);
            });
        })
        .catch(error => {
            console.error("카테고리를 불러오는 중 오류 발생:", error);
        });
}

async function initAuthUI() {
    const btn = document.getElementById("auth-button");
    const icon = document.getElementById("auth-icon");
    if (!btn) return;

    // 기본 폴백: 로그인 버튼(파랑)
    setAsLogin(btn, icon);

    // 로그인 페이지에서는 상태조회/토글 스킵
    if (isLoginPage()) return;

    // 로그인 상태 조회 (jwt.js 제공)
    try {
        const me = await window.fetchUserInfo?.();
        if (!me) return;

        if (me) {
            // 로그인 상태 -> 로그아웃 버튼(빨강)으로 변환
            setAsLogout(btn, icon);
        } else {
            // 미로그인 -> 폴백 유지
            setAsLogin(btn, icon);
        }
    } catch (e) {
        // 조회 실패 -> 폴백 유지
        setAsLogin(btn, icon);
    }
}

function setAsLogin(btn, icon) {
    btn.setAttribute("href", "/login");
    btn.classList.add("btn-primary");
    btn.classList.remove("btn-danger");
    if (icon) icon.src = "/img/icon/login.png";
    btn.onclick = null;
}

async function doLogoutOnce() {
    try {
        await ensureFreshXsrf();
        const xsrf = getCookie('XSRF-TOKEN');

        await window.authApi.post('logout', {}, {
            withCredentials: true,
            headers: { 'X-XSRF-TOKEN': xsrf }, // 명시적으로 첨부
            _skipRefresh: true                 // 재발급 인터셉터 타지 않게
        });

        // UI 즉시 전환 (메인 그대로 유지)
        const btn  = document.getElementById('auth-button');
        const icon = document.getElementById('auth-icon');
        if (btn) {
            btn.setAttribute('href', '/login');
            btn.classList.add('btn-primary');
            btn.classList.remove('btn-danger');
            btn.onclick = null;
        }
        if (icon) icon.src = '/img/icon/login.png';
        await setAdminNav();

        // (선택) 토스트
        if (window.Swal) {
            Swal.fire({ icon: 'success', title: '로그아웃', text: '정상적으로 로그아웃되었습니다.' });
        }

        const recommendSection = document.querySelector('.recommend');
        if (recommendSection) {
            recommendSection.style.display = 'none';
            // 기존 데이터도 비워주는 것이 보안상 좋습니다.
            document.getElementById('recommend-grid').innerHTML = '';
        }

    } catch (e) {
        console.warn('logout failed:', e);
        if (window.Swal) {
            Swal.fire({ icon: 'error', title: '로그아웃 실패', text: '잠시 후 다시 시도해 주세요.' });
        }
    }
}

function setAsLogout(btn, icon) {
    btn.setAttribute("href", "#");
    btn.classList.remove("btn-primary");
    btn.classList.add("btn-danger");
    if (icon) icon.src = "/img/icon/logout.png";
    btn.onclick = (e) => { e.preventDefault(); doLogoutOnce(); };
}


async function setAdminNav() {
    const adminMenu = document.getElementById("admin-menu");
    if (!adminMenu) return;

    // 기본은 숨김
    adminMenu.hidden = true;

    // 로그인 페이지에선 스킵
    if (isLoginPage()) return;

    try {
        // jwt.js에 정의한 util (쿠키 기반, 미로그인 시 null 반환)
        const me = await window.fetchUserInfo?.();
        if (!me) return;

        const role = (me.role || "").toUpperCase();
        if (role === "ADMIN" || role === "ROLE_ADMIN") {
            adminMenu.hidden = false;
        }
    } catch (_) {
        // 미로그인/오류 -> 숨김 유지
    }
}

// 버튼 요소 가져오기
function initScrollToTop() {
    const scrollToTopButton = document.getElementById("scrollToTop");
    if (!scrollToTopButton) return;

    window.addEventListener("scroll", () => {
        if (window.scrollY > 200) {
            scrollToTopButton.classList.add("show");
            scrollToTopButton.classList.remove("hide");
        } else {
            scrollToTopButton.classList.add("hide");
            scrollToTopButton.classList.remove("show");
        }
    });

    scrollToTopButton.addEventListener("click", () => {
        window.scrollTo({ top: 0, behavior: "smooth" });
    });
}