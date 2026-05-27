import { NextRequest, NextResponse } from 'next/server'

const EMPTY_PAGE = {
  content: [],
  totalElements: 0,
  totalPages: 0,
  size: 10,
  number: 0,
}

const EMPTY_RESPONSE = {
  success: true,
  status: 200,
  errorCode: null,
  message: 'OK',
  data: EMPTY_PAGE,
}

export async function GET(request: NextRequest) {
  try {
    const params = request.nextUrl.searchParams.toString()
    const url = `${process.env.AI_SLACK_SERVICE_URL || 'http://localhost:8086'}/api/v1/ai-slack${params ? '?' + params : ''}`

    const forwardHeaders: Record<string, string> = {
      'Content-Type': 'application/json',
      'X-User-Role': 'MASTER',
      'X-User-Id': 'frontend-test-user',
    }

    const authHeader = request.headers.get('Authorization')
    if (authHeader) forwardHeaders['Authorization'] = authHeader

    const response = await fetch(url, { headers: forwardHeaders })

    if (!response.ok) {
      return NextResponse.json(EMPTY_RESPONSE)
    }

    const data = await response.json()
    return NextResponse.json(data)
  } catch {
    return NextResponse.json(EMPTY_RESPONSE)
  }
}
