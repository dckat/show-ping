const wsProtocol = location.protocol === 'https:' ? 'wss://' : 'ws://'; // https로 실행중이면 wss로 실행하고 http로 실행하면 ws로 실행
var ws = new WebSocket(wsProtocol + location.host + '/live');
var rec = new WebSocket(wsProtocol + location.host + '/record');
var live;
var watch;
var webRtcPeer;
var webRtcRecord;
var state;

let stompClient = null;
let memberId = null;
let memberRole = null;
let reconnectTimeout = 5000;
let viewerCount = null;

const NO_CALL = 0;
const IN_CALL = 1;
const POST_CALL = 2;
const DISABLED = 3;
const IN_PLAY = 4;

document.addEventListener('DOMContentLoaded', function () {
    window.addEventListener('dataLoaded', function () {
        live = document.getElementById('live-video');
        watch = document.getElementById('live');

        if (watch) {
            initViewerCount();
        }

        if (live) {
            getMemberInfo(); // 여기서 memberRole을 설정함

            const checkAdminInterval = setInterval(() => {
                if (memberRole !== null) {
                    clearInterval(checkAdminInterval);

                    if (memberRole !== 'ROLE_ADMIN') {
                        window.location.href = '/';
                    }
                }
            }, 100);

        }
        getMemberInfo();

        // send 버튼 이벤트와 STOMP 연결 초기화
        const sendButton = document.getElementById('send-button');
        if (sendButton) {
            sendButton.addEventListener('click', sendChatMessage);
        }

        // --- 신고 모달 관련 이벤트 ---
        const reportForm = document.getElementById('reportForm');
        const cancelBtn = document.getElementById('cancelBtn');
        const reportModal = document.getElementById('reportModal');
        const modalOverlay = document.getElementById('modalOverlay');

        if (reportForm) {
            reportForm.addEventListener('submit', async (e) => {
                e.preventDefault();
                const checkedReason = document.querySelector('input[name="reportReason"]:checked');
                if (!checkedReason) return;

                const reasonValue = checkedReason.value;
                const reportContent = document.getElementById('reportTargetText').textContent;

                try {
                    await window.ensureCsrfCookie(); // ✅ async 가능
                    const response = await window.csrfPost('/api/report/register', {
                        reportReason: reasonValue,
                        reportContent: reportContent
                    });

                    console.log("신고 등록 완료:", response.data);
                    Swal.fire({
                        icon: 'success',
                        title: '신고 접수 완료',
                        text: '신고가 접수되었습니다.'
                    });
                    closeReportModal();

                } catch (error) {
                    console.error("신고 등록 중 오류 발생:", error);
                    Swal.fire({
                        icon: 'error',
                        title: '신고 등록 오류',
                        text: '신고 등록 중 오류가 발생했습니다.'
                    });
                }
            });
        }

        if (cancelBtn) {
            cancelBtn.addEventListener('click', () => {
                Swal.fire({
                    icon: 'info',
                    title: '신고 취소',
                    text: '신고를 취소했습니다.'
                });

                closeReportModal();
            });
        }

        if (modalOverlay) {
            modalOverlay.addEventListener('click', () => {
                closeReportModal();
            });
        }

        function closeReportModal() {
            if (reportModal) {
                reportModal.style.display = 'none';
            }
            if (modalOverlay) {
                modalOverlay.style.display = 'none';
            }
        }

        // --- 채팅 영역 관련 이벤트 ---
        const messageInput = document.getElementById('message-input');
        const chatContainer = document.getElementById('chat-messages');
        const scrollToLatestButton = document.getElementById('scroll-to-latest');
        const charCount = document.getElementById('char-count');

        if (chatContainer && scrollToLatestButton) {
            chatContainer.addEventListener('scroll', function () {
                if (chatContainer.scrollTop + chatContainer.clientHeight < chatContainer.scrollHeight - 20) {
                    scrollToLatestButton.style.display = 'block';
                } else {
                    scrollToLatestButton.style.display = 'none';
                }
            });

            scrollToLatestButton.addEventListener('click', function () {
                chatContainer.scrollTop = chatContainer.scrollHeight;
                scrollToLatestButton.style.display = 'none';
            });
        }

        if (messageInput) {
            // Enter 키로 메시지 전송
            messageInput.addEventListener('keypress', (event) => {
                if (event.key === 'Enter') {
                    event.preventDefault();
                    sendChatMessage();
                }
            });

            // 글자 수 업데이트
            messageInput.addEventListener('input', () => {
                const length = messageInput.value.length;
                if (charCount) {
                    charCount.textContent = `${length}/200`;
                }
            });
        }

        // streamInfo == null이면(등록된 방송 정보가 없다면) 방송 시작, 방송 종료 버튼 비활성화
        if (streamInfo === false) {
            setState(DISABLED);
        } else {    // 기존에 등록된 방송 정보가 있다면 방송 시작 버튼 활성화, 방송 종료 버튼 비활성화
            setState(NO_CALL);
        }

        // 할인율 input 요소
        const discountRateInput = document.getElementById('discountRate');
        if (discountRateInput) {
            // 할인율 변경 시 이벤트 리스너 등록
            discountRateInput.addEventListener('input', updateAnnouncementDiscount);
        }

        // 업데이트 버튼을 눌러야 적용된다면, update-btn 클릭 이벤트에 바인딩
        const updateBtn = document.querySelector('.update-btn');
        if (updateBtn) {
            updateBtn.addEventListener('click', updateAnnouncementDiscount);
        }

        const tabButtons = document.querySelectorAll('.tab-btn');
        const tabContents = document.querySelectorAll('.tab-content');
        const faqItems = document.querySelectorAll('.faq-item');

        faqItems.forEach(item => {
            const question = item.querySelector('.faq-question');

            // 질문 영역 클릭 시 active 토글
            question.addEventListener('click', () => {
                // 이미 active라면 닫고, 아니면 열기
                item.classList.toggle('active');
            });
        });

        tabButtons.forEach(btn => {
            btn.addEventListener('click', () => {
                // 1) 모든 버튼에서 active 제거
                tabButtons.forEach(b => b.classList.remove('active'));
                // 2) 모든 탭 콘텐츠에서 active 제거
                tabContents.forEach(tc => tc.classList.remove('active'));

                // 3) 클릭된 버튼에 active 추가
                btn.classList.add('active');
                // 4) data-target 속성으로 연결된 콘텐츠를 찾아서 active 추가
                const targetId = btn.getAttribute('data-target');
                const targetContent = document.getElementById(targetId);
                if (targetContent) {
                    targetContent.classList.add('active');
                }
            });
        });
    });
})

window.onbeforeunload = function() {
    ws.close();
    rec.close();
}

function setState(nextState) {
    switch (nextState) {
        case NO_CALL:
            $('#start').attr('disabled', false);
            $('#stop').attr('disabled', true);
            break;
        case DISABLED:
            $('#start').attr('disabled', true);
            $('#stop').attr('disabled', true);
            break;
        case IN_CALL:
            $('#start').attr('disabled', true);
            $('#stop').attr('disabled', false);
            break;
        case POST_CALL:
            $('#start').attr('disabled', false);
            $('#stop').attr('disabled', true);
            break;
        case IN_PLAY:
            $('#start').attr('disabled', true);
            $('#stop').attr('disabled', false);
            break;
        default:
            onLiveError('Unknown state ' + nextState);
            return;
    }
    state = nextState;
}

ws.onopen = function () {
    if (watch) {
        viewer();
    }
}

ws.onmessage = function(message) {
    let parsedMessage = JSON.parse(message.data);

    switch (parsedMessage.id) {
        case 'presenterResponse':
            presenterResponse(parsedMessage);
            break;
        case 'viewerResponse':
            viewerResponse(parsedMessage);
            break;
        case 'iceCandidate':
            webRtcPeer.addIceCandidate(parsedMessage.candidate, function(error) {
                if (error)
                    return console.error('Error adding candidate: ' + error);
            });
            break;
        case 'viewerCountUpdate':
            updateViewerCount(parsedMessage.count);
            break;
        case 'stopCommunication':
            dispose();
            break;
        default:
            console.error('Unrecognized message', parsedMessage);
    }
}

rec.onmessage = function(message) {
    let parsedMessage = JSON.parse(message.data);

    switch (parsedMessage.id) {
        case 'startResponse':
            startResponse(parsedMessage);
            break;
        case 'stop':
            dispose();
            break;
        case 'iceCandidate':
            webRtcRecord.addIceCandidate(parsedMessage.candidate, function(error) {
                if (error)
                    return console.error('Error adding candidate: ' + error);
            });
            break;
        case 'recording':
            break;
        case 'stopped':
            stopChat();
            uploadFileToNCP();
            break;
        default:
            console.error('Unrecognized message', parsedMessage);
    }
}

function presenterResponse(message) {
    setState(IN_CALL);

    webRtcPeer.processAnswer(message.sdpAnswer, function(error) {
        if (error) {
            reject(error);
        }
        createVideo();
    })
}

function startResponse(message) {
    webRtcRecord.processAnswer(message.sdpAnswer, function(error) {
        if (error)
            return console.error(error);
    });
    createChatRoom();
}

function viewerResponse(message) {
    if (message.response != 'accepted') {
        var errorMsg = message.message ? message.message : 'Unknown error';
        console.info('Call not accepted for the following reason: ' + errorMsg);
    } else {
        webRtcPeer.processAnswer(message.sdpAnswer, function(error) {
            if (error)
                return console.error(error);
        });
    }
    connectToChatRoom();
}

function initViewerCount() {
    axios.get(`/api/live/viewCount/${streamNo}`)
        .then(response => {
            updateViewerCount(response.data);
            }
        )
}

function updateViewerCount(count) {
    console.log("시청자 수 집계 업데이트");

    const countElement = document.getElementById('viewer-count');
    if (!countElement) return;

    let displayCount;
    if (count >= 1000000) {
        displayCount = (count / 1000000).toFixed(1) + 'M'; // 1.1M
    } else if (count >= 1000) {
        displayCount = (count / 1000).toFixed(1) + 'K'; // 1.2K
    } else {
        displayCount = count; // 999 이하일 땐 숫자 그대로
    }

    countElement.textContent = displayCount;
    viewerCount = displayCount;
}

async function startLive() {
    try {
        // 1) 안전하게 쿠키 보장
        await window.ensureCsrfCookie(); // 없으면 /api/csrf로 발급
        // 2) 403 시 자동 재시도까지 해주는 래퍼
        const response = await window.csrfPost("/api/live/start", { streamNo });
        const data = response.data;

        document.getElementById("broadcastTitle").value = data.streamTitle;
        document.getElementById("broadcastDesc").value = data.streamDescription;

        const productElement = document.querySelector(".product-element");
        productElement.innerHTML = `
      <img src="${data.productImg}" alt="상품 이미지" class="product-img">
      <div class="product-info" id="${data.productNo}">
        <p class="product-name">${data.productName}</p>
        <div class="product-price-container">
          <p class="product-origin-price">${data.productPrice}</p>
        </div>
      </div>`;
        document.getElementById("discountRate").value = data.productSale;

        if (!webRtcPeer) {
            const options = { localVideo: live, onicecandidate: onLiveIceCandidate };
            webRtcPeer = new kurentoUtils.WebRtcPeer.WebRtcPeerSendonly(options, function (error) {
                if (error) return console.error(error);
                webRtcPeer.generateOffer(onLiveOffer);
            });
        }
    } catch (error) {
        console.error("방송 시작 오류:", error);
        Swal.fire({ icon: 'error', title: '방송 시작 실패', text: '방송 시작 중 문제가 발생했습니다.' });
    }
}

function viewer() {
    if (!webRtcPeer) {
        // showSpinner(watch);

        var options = {
            remoteVideo : watch,
            onicecandidate : onLiveIceCandidate
        }
        webRtcPeer = new kurentoUtils.WebRtcPeer.WebRtcPeerRecvonly(options,
            function(error) {
                if (error) {
                    return console.error(error);
                }
                this.generateOffer(onViewOffer);
            });
    }
}

function onLiveIceCandidate(candidate) {
    let message = {
        id : 'onIceCandidate',
        candidate : candidate
    };
    sendLiveMessage(message);
}

function createVideo() {
    let options = {
        localVideo : live,
        mediaConstraints : getConstraints(),
        onicecandidate : onRecordIceCandidate
    }

    webRtcRecord = new kurentoUtils.WebRtcPeer.WebRtcPeerSendrecv(options,
        function(error) {
            if (error)
                return console.error(error);
            webRtcRecord.generateOffer(onRecordOffer);
        });
}

function onRecordIceCandidate(candidate) {
    let message = {
        id : 'onIceCandidate',
        candidate : candidate
    };
    sendRecordMessage(message);
}

function onLiveOffer(error, offerSdp) {
    if (error)
        return console.error('Error generating the offer');
    let message = {
        id : 'presenter',
        streamNo: streamNo,
        sdpOffer : offerSdp
    }
    sendLiveMessage(message);
}

function onViewOffer(error, offerSdp) {
    if (error)
        return console.error('Error generating the offer');

    var message = {
        id : 'viewer',
        streamNo: streamNo,
        sdpOffer : offerSdp
    }
    sendLiveMessage(message);
}

function onRecordOffer(error, offerSdp) {
    let title = "stream_" + streamNo + "_" + document.getElementById('broadcastTitle').value;

    if (error)
        return console.error('Error generating the offer');

    let message = {
        id : 'start',
        title: title,
        sdpOffer : offerSdp,
        mode :  $('input[name="mode"]:checked').val()
    }
    sendRecordMessage(message);
}

function getConstraints() {
    return {
        audio: true,
        video: true
    };
}

async function stopLive() {
    try {
        // CSRF 쿠키가 없다면 발급
        await window.ensureCsrfCookie();

        // 403이면 한 번 재시도까지 해주는 래퍼 사용
        const response = await window.csrfPost("/api/live/stop", { streamNo });
        console.log(response);

        // 서버 정지 성공 후 WebSocket/녹화 쪽 정리
        const message = { id: 'stop' };
        if (live) {
            dispose();
        }
        sendLiveMessage(message);
        stopRecord();

    } catch (error) {
        console.error("방송 종료 오류:", error);

        // 선택: 서버 호출 실패해도 로컬 리소스는 정리하고 싶다면 아래 주석 해제
        // try {
        //   const message = { id: 'stop' };
        //   if (live) dispose();
        //   sendLiveMessage(message);
        //   stopRecord();
        // } catch (_) {}

        Swal.fire({
            icon: 'error',
            title: '방송 종료 실패',
            text: '방송 종료 중 문제가 발생했습니다.'
        });
    }
}
function stopRecord() {
    let stopMessageId = (state === IN_CALL) ? 'stop' : 'stopPlay';
    console.log('Stopping video while in ' + state + '...');
    if (webRtcRecord) {
        webRtcRecord.dispose();
        webRtcRecord = null;
        setState(NO_CALL);

        let message = {
            id : stopMessageId
        }
        sendRecordMessage(message);
    }

    // hideSpinner(live);
}

function dispose() {
    if (webRtcPeer) {
        webRtcPeer.dispose();
        webRtcPeer = null;
    }
    stopChat();
}

function sendLiveMessage(message) {
    let jsonMessage = JSON.stringify(message);
    ws.send(jsonMessage);
}

function sendRecordMessage(message) {
    let jsonMessage = JSON.stringify(message);
    rec.send(jsonMessage);
}

// function showSpinner() {
//     for (let i = 0; i < arguments.length; i++) {
//         arguments[i].poster = '/img/transparent-1px.png';
//         arguments[i].style.background = 'center transparent url("/img/spinner.gif") no-repeat';
//     }
// }

// function hideSpinner() {
//     for (let i = 0; i < arguments.length; i++) {
//         arguments[i].src = '';
//         arguments[i].poster = '/img/webrtc.png';
//         arguments[i].style.background = '';
//     }
// }

function onLiveError(error) {
    console.error(error);
}

async function uploadFileToNCP() {
    let title = "stream_" + streamNo + "_" + document.getElementById('broadcastTitle').value;
    let fileName = title + ".mp4";

    try {
        // 1) XSRF 쿠키 보장
        await window.ensureCsrfCookie();

        // 2) 순차 실행 (403 시 자동 재시도 포함)
        await window.csrfPost('/api/vod/upload', { title: fileName });
        Promise.all([
            window.csrfPost('/api/batch/hls/create',     { fileTitle: title }),
            window.csrfPost('/api/batch/subtitle/create',{ fileTitle: title }),
        ]).catch(err => console.error('후속 작업 실패:', err));

        console.log('업로드 정상적으로 실행 중...');
    } catch (error) {
        console.error("업로드 중 에러 발생:", error);
        Swal.fire({
            icon: 'error',
            title: '업로드 실패',
            text: '동영상 업로드 중 문제가 발생했습니다.'
        });
    }
}

async function createChatRoom() {
    try {
        // 1) XSRF 쿠키 보장
        await window.ensureCsrfCookie();

        // 2) 403 시 자동 재시도까지 해주는 POST 호출
        const response = await window.csrfPost(`/api/chatRoom/create`, { streamNo });

        console.log(response.data);
        Swal.fire({
            icon: 'info',
            title: '채팅방 생성 완료',
            text: `채팅방 번호: ${response.data.chatRoomNo}`
        });

        chatRoomNo = response.data.chatRoomNo;
        connectToChatRoom();

    } catch (error) {
        console.error('채팅방 생성 중 오류 발생:', error);
        Swal.fire({
            icon: 'error',
            title: '오류',
            text: '채팅방 생성 중 오류가 발생했습니다.'
        });
    }
}

function connectToChatRoom() {
    // 이미 stompClient가 연결되어 있으면 재연결하지 않음
    if (stompClient && stompClient.connected) {
        console.log('이미 채팅방에 연결되어 있습니다.');
        return;
    }

    var socket = new SockJS('/ws-stomp-chat');
    stompClient = Stomp.over(socket);

    stompClient.connect({}, onConnected, onChatError);
}

// 연결 성공 콜백
function onConnected(frame) {
    console.log('Connected to WebSocket:', frame);
    console.log(document.getElementById('error-message'));


    // 채팅방 구독
    stompClient.subscribe(`/sub/chat/room/${chatRoomNo}`, function (message) {
        console.log("Received message from subscription:", message);
        try {
            const parsedMessage = JSON.parse(message.body);
            console.log("Parsed message:", parsedMessage);
            addMessageToChat(parsedMessage);
        } catch (e) {
            console.error("Error parsing message:", e);
        }
    });

    // STOMP 클라이언트 연결 후 사용자 전용 에러 채널 구독
    stompClient.subscribe('/user/queue/errors', function(message) {
        var errorResponse = JSON.parse(message.body);
        console.log("Inline error message:", errorResponse.chat_message); // 로그 출력 확인
        Swal.fire({
            icon: 'error',
            title: '금칙어 오류',
            text: errorResponse.chat_message
        });

        showInlineError(errorResponse.chat_message);
    });

}

// 채팅 메시지 송신 함수
function sendChatMessage() {
    const input = document.getElementById('message-input');
    const messageText = input.value.trim();
    const charCount = document.getElementById('char-count');
    console.log("Attempting to send message:", messageText);

    // 로그인 여부 체크. memberId가 null이면 로그인하지 않은 것으로 간주)
    if (!memberId) {
        Swal.fire({
            title: '로그인 필요',
            text: '로그인 후 입력 가능합니다. 로그인 화면으로 이동하시겠습니까?',
            icon: 'warning',
            showCancelButton: true,
            confirmButtonText: '네, 이동합니다.',
            cancelButtonText: '취소'
        }).then((result) => {
            if (result.isConfirmed) {
                window.location.href = '/login'; // 로그인 페이지 URL로 변경
            }
        });
        return;
    }

    if (!stompClient || !stompClient.connected) {
        console.warn('WebSocket이 연결되어 있지 않습니다. 재연결 시도 후 다시 전송해 주세요.');
        return;
    }
    // 메세지 전송 전 기존 에러 메세지 초기화
    clearErrorMessage();

    if (messageText) {
        const message = {
            _id: "", // 서버 또는 DB에서 할당
            chat_member_id: memberId,
            chat_room_no: chatRoomNo,
            chat_stream_no : chatStreamNo || streamNo,
            chat_message: messageText,
            chat_role: memberRole,
            chat_created_at: new Date().toLocaleString()
        };
        console.log("Sending message:", message);
        stompClient.send('/pub/chat/message', {}, JSON.stringify(message));
        input.value = ''; // 입력 필드 초기화
        charCount.textContent = '0/200'; // 글자 수 초기화
    }
}

function addMessageToChat(message) {
    // 채팅 메시지를 삽입할 요소 선택 (chat-messages div)
    const chatMessagesContainer = document.getElementById('chat-messages');

    // 새 메시지 요소 생성
    const messageElement = document.createElement("div");
    messageElement.classList.add("message");

    // 사용자 아이디
    const userNameSpan = document.createElement("span");
    userNameSpan.classList.add("user-name");
    userNameSpan.textContent = message.chat_member_id;

    // 메시지 텍스트
    const messageTextP = document.createElement("p");
    messageTextP.classList.add("chat-text");
    messageTextP.textContent = message.chat_message;

    if (message.chat_role && message.chat_role === "ROLE_ADMIN") {
        userNameSpan.textContent = "관리자 ✓";
        // 관리자 이름을 빨간색으로 표시
        userNameSpan.style.color = "red";
        messageElement.classList.add("admin");
        messageTextP.style.color = "red";
        messageTextP.textContent = message.chat_message;
    } else {
        userNameSpan.textContent = message.chat_member_id;
        messageTextP.textContent = message.chat_message;

        // 아이디 클릭 시 신고 모달 오픈
        userNameSpan.addEventListener('click', () => {
            openReportModal(message.chat_member_id, message.chat_message);
        });
    }

    // 요소 조합
    messageElement.appendChild(userNameSpan);
    messageElement.appendChild(messageTextP);

    // 채팅 메시지 컨테이너에 추가
    chatMessagesContainer.appendChild(messageElement);

    // 자동 스크롤
    setTimeout(() => {
        chatMessagesContainer.scrollTop = chatMessagesContainer.scrollHeight;
    }, 100);
}

function openReportModal(memberId, chatMessage) {
    // 신고 대상 정보 세팅
    const reportTargetText = document.getElementById('reportTargetText');
    reportTargetText.textContent = `${memberId}님: ${chatMessage}`;

    // 모달 & 오버레이 표시
    const reportModal = document.getElementById('reportModal');
    const modalOverlay = document.getElementById('modalOverlay');

    reportModal.style.display = 'block';
    modalOverlay.style.display = 'block';
}

// 연결 에러 콜백
function onChatError(error) {
    console.error('STOMP error:', error);

    // 일정 시간 후 재연결 시도
    setTimeout(function () {
        console.log('Attempting to reconnect...');
        connectToChatRoom();
    }, reconnectTimeout);
}


function stopChat() {
    if (stompClient) {
        stompClient.disconnect(() => {
            console.log('Disconnected from WebSocket');
        });
    }
}

function clearErrorMessage() {
    const errorElement = document.getElementById('error-message');
    if (errorElement) {
        errorElement.textContent = '';
        errorElement.style.display = 'none';
    }
}

function getMemberInfo(){
    axios.get('/api/chat/info', {
        withCredentials: true
    })
        .then(response => {
            memberId = response.data.memberId;
            memberRole = response.data.role;
            console.log("[DEBUG] Retrieved memberId:", memberId);
            console.log("[DEBUG] Retrieved memberRole:", memberRole);
        })
        .catch(error => {
            console.error("사용자 정보를 불러오는 중 오류:", error);
        });
}

// 인라인 에러 메시지를 표시하는 함수
function showInlineError(errorMessage) {
    const errorElement = document.getElementById('inline-error');
    if (errorElement) {
        errorElement.textContent = errorMessage;
        console.log("Inline error message: ", errorMessage);
        errorElement.style.display = 'block';
        // 5초 후 자동 숨김 처리
        setTimeout(() => {
            errorElement.style.display = 'none';
        }, 5000);
    } else {
        console.error("Inline error element not found!");
    }
}

/**
 * 공지 영역, 할인율과 할인가 업데이트
 */
function updateAnnouncementDiscount() {
    // 할인율, 상품 원가 가져오기
    const discountRate = parseFloat(document.getElementById('discountRate').value) || 0;

    // 3) 공지 영역 갱신
    const announcementEl = document.querySelector('.announcement');
    if (announcementEl) {
        announcementEl.innerHTML = `
            공지 | Live 방송 중에만 초특가 
            <strong>${discountRate}% 세일!! </strong>
        `;
    }
}

/**
 * Lightbox utility (to display media pipeline image in a modal dialog)
 */
$(document).delegate('*[data-toggle="lightbox"]', 'click', function(event) {
    event.preventDefault();
    $(this).ekkoLightbox();
});
