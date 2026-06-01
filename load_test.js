import http from 'k6/http';
import { check, sleep } from 'k6';

// 테스트 옵션: 100명의 가상 유저(VUser)가 10초 동안 미친듯이 요청을 보냅니다.
export const options = {
  vus: 100,
  duration: '10s',
};

export default function () {

  const res = http.get('http://localhost:8082/internal/v1/hub-routes/shortest-path?originHubId=2a40966e-49a2-4e33-86cc-8c49e50cfd61&destinationHubId=da68820f-e8f2-4dba-9dff-bee456c72003');
  
  // 응답 상태가 200 OK인지, 응답 시간이 200ms 이하인지 체크합니다.
  check(res, {
    'status is 200': (r) => r.status === 200,
    'response time < 200ms': (r) => r.timings.duration < 200,
  });
  
  // 요청 간 0.1초 대기
  sleep(0.1);
}
