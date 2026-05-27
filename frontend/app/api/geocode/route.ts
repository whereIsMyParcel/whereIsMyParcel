import { NextResponse } from "next/server";

export async function GET(request: Request) {
  const { searchParams } = new URL(request.url);
  const address = searchParams.get("address");

  if (!address) {
    return NextResponse.json({ error: "Address is required" }, { status: 400 });
  }

  const apiKey = process.env.NEXT_PUBLIC_KAKAO_REST_API_KEY;
  
  if (!apiKey) {
    return NextResponse.json({ error: "Kakao API key is missing" }, { status: 500 });
  }

  try {
    /* 
    ========================================================================
    [TODO: 카카오맵 API 심사 통과 후 주석 해제 (실제 API 연동)]
    현재 카카오 측 'OPEN_MAP_AND_LOCAL' 서비스 심사 대기로 인해 API 호출이 차단된 상태입니다.
    심사가 통과되면 아래 임시 Mock 로직을 지우고, 이 주석 안에 있는 실제 API 호출 로직을 사용하세요.
    ========================================================================
    const kakaoRes = await fetch(
      `https://dapi.kakao.com/v2/local/search/address.json?query=${encodeURIComponent(address)}`,
      {
        headers: {
          Authorization: `KakaoAK ${apiKey}`,
        },
      }
    );

    const data = await kakaoRes.json();
    
    if (!kakaoRes.ok) {
      console.error("Kakao API Error Response:", data);
      return NextResponse.json({ error: data.msg || "Kakao API error" }, { status: kakaoRes.status });
    }

    return NextResponse.json(data);
    ========================================================================
    */

    // --- 임시 가짜(Mock) 데이터 응답 (팀 프로젝트 프론트/백엔드 테스트용) ---
    // 어떤 주소를 입력하든 고정된 위도(37.3595)와 경도(127.1053)를 반환합니다. (판교역 근방 좌표)
    console.warn("[MOCK MODE] 카카오 심사 대기로 인해 임시 좌표를 반환합니다.");
    return NextResponse.json({
      documents: [
        {
          x: "127.105399", // 경도 (longitude)
          y: "37.359570",  // 위도 (latitude)
        }
      ]
    });

  } catch (error: any) {
    console.error("Geocoding proxy error:", error);
    return NextResponse.json({ error: "Failed to fetch geocoding data" }, { status: 500 });
  }
}
