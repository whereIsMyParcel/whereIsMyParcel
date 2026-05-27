import { NextRequest, NextResponse } from 'next/server'

function forwardHeaders(request: NextRequest): Record<string, string> {
  const headers: Record<string, string> = {
    'Content-Type': 'application/json',
    'X-User-Role': 'MASTER',
    'X-User-Id': 'frontend-test-user',
  }
  const auth = request.headers.get('Authorization')
  if (auth) headers['Authorization'] = auth
  return headers
}

export async function GET(request: NextRequest, { params }: { params: { id: string } }) {
  try {
    const response = await fetch(
      `http://localhost:8086/api/v1/ai-slack/${params.id}`,
      { headers: forwardHeaders(request) }
    )
    const data = await response.json()
    return NextResponse.json(data, { status: response.status })
  } catch {
    return NextResponse.json({ success: false, message: '서비스를 사용할 수 없습니다.' }, { status: 503 })
  }
}

export async function PUT(request: NextRequest, { params }: { params: { id: string } }) {
  try {
    const body = await request.text()
    const response = await fetch(
      `http://localhost:8086/api/v1/ai-slack/${params.id}`,
      { method: 'PUT', headers: forwardHeaders(request), body }
    )
    const data = await response.json()
    return NextResponse.json(data, { status: response.status })
  } catch {
    return NextResponse.json({ success: false, message: '서비스를 사용할 수 없습니다.' }, { status: 503 })
  }
}

export async function DELETE(request: NextRequest, { params }: { params: { id: string } }) {
  try {
    const response = await fetch(
      `http://localhost:8086/api/v1/ai-slack/${params.id}`,
      { method: 'DELETE', headers: forwardHeaders(request) }
    )
    return NextResponse.json({}, { status: response.status })
  } catch {
    return NextResponse.json({ success: false, message: '서비스를 사용할 수 없습니다.' }, { status: 503 })
  }
}
