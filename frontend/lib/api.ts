import { getSession } from "next-auth/react";

const BASE_URL = '/api/v1';

export async function fetchApi<T>(endpoint: string, options: RequestInit = {}): Promise<T> {
  const session = await getSession();
  const token = session?.accessToken || '';
  
  const headers = {
    'Content-Type': 'application/json',
    'Authorization': `Bearer ${token}`, // Keycloak 토큰
    'X-User-Role': 'MASTER', // 기존 HubController 통과용
    'X-User-Id': 'frontend-test-user',
    ...options.headers,
  };

  const response = await fetch(`${BASE_URL}${endpoint}`, {
    ...options,
    headers,
  });

  if (!response.ok) {
    if (response.status === 401) {
      if (typeof window !== 'undefined') {
        window.location.href = '/api/auth/signin';
      }
      throw new Error('로그인이 필요합니다 (401 Unauthorized)');
    }

    let errorMessage = `API request failed with status ${response.status}`;
    try {
      const errorData = await response.json();
      errorMessage = errorData.message || errorMessage;
    } catch (e) {
      // 빈 응답일 경우 무시
    }
    throw new Error(errorMessage);
  }

  const responseData = await response.json();

  // 백엔드의 통일된 ApiResponse 구조 (code, message, data) 에서 data 부분만 추출해서 반환
  return responseData.data as T;
}
